package com.example.scheduletracking

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.gson.Gson
import com.google.maps.DirectionsApi
import okhttp3.OkHttpClient
import okhttp3.Request


class RouteActivity : AppCompatActivity() {

    companion object {
        private const val JOB_ID = 123
        private lateinit var mapFragment: SupportMapFragment
        private lateinit var googleMap: GoogleMap
        private lateinit var durationTextView: TextView
        private lateinit var broadcastReceiver: BroadcastReceiver
        private lateinit var latLng: LatLng
        private var lat: Double = 0.0
        private var lng: Double = 0.0
        private var isMapInitialized = false
        private var driverMarker: Marker? = null
        private var isMapReady = false
        private var isDriverMarkerAdded = false
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        val apiKey = getString(R.string.google_maps_key)
        // Initializing the Places API with the help of our API_KEY
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Display the back button with an arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Tracking Driver"

        // Check if there is any network
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            val builder = JobInfo.Builder(JOB_ID, ComponentName(this, FirebaseJobService::class.java))
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        } else {
            // Handle case when there is no network available
            // Show AlertDialog and redirect to MainActivity when OK button is clicked
            val alertDialogBuilder = AlertDialog.Builder(this@RouteActivity)
            alertDialogBuilder.setTitle("Alert")
            alertDialogBuilder.setMessage("Not internet available.")
            alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                redirectToMainActivity()
            }
        }

        // Initialize the map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
            isMapReady = true
            isMapInitialized = true
            checkAndUpdateMap()
        }

        // Register a BroadcastReceiver to receive updates from the FirebaseJobService
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Retrieve the latitude and longitude from the Intent
                lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
                lng = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0

                // Add marker if coordinates are not null
                if (isMapInitialized && lat != 0.0 && lng != 0.0) {
                    latLng = LatLng(lat, lng)
                    // Add custom marker to driver's location
                    addDriverMarker(googleMap, latLng)
                    isDriverMarkerAdded = true
                } else {
                    // Show AlertDialog and redirect to MainActivity when OK button is clicked
                    val alertDialogBuilder = AlertDialog.Builder(this@RouteActivity)
                    alertDialogBuilder.setTitle("Alert")
                    alertDialogBuilder.setMessage("Latitude and longitude values are not available.")
                    alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        redirectToMainActivity()
                    }
                    alertDialogBuilder.setCancelable(false)
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()

                    // Update the map
                    driverMarker?.remove()
                    isDriverMarkerAdded = false

                }

                // Call the function to check and update the map
                checkAndUpdateMap()
            }
        }
        val intentFilter = IntentFilter("UPDATE_COORDINATES")
        registerReceiver(broadcastReceiver, intentFilter)

    }

    // Function to redirect to MainActivity
    private fun redirectToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Function to check if the map is ready and update it accordingly
    private fun checkAndUpdateMap() {
        if (isMapReady) {
            // Get the selected BusStopData from the Intent
            val busStopData = intent.getParcelableExtra<BusStopData>("busStopData")

            val busStopTitle = findViewById<TextView>(R.id.busStop)
            busStopTitle.text = busStopData?.title ?: "Unidentified bus stop"

            // Add a marker to the map for the bus stop, which will also be the destination
            val destinationLocation = LatLng(busStopData!!.latitude, busStopData.longitude)
            val destinationMarkerOptions = MarkerOptions().position(destinationLocation).title(busStopData.title)
            googleMap.addMarker(destinationMarkerOptions)

            // Send coordinates of driver and bus stop to generate route & calculate time journey
            val originLocation = LatLng(lat, lng)
            val apiKey = getString(R.string.google_maps_key)
            val url = getDirectionURL(originLocation, destinationLocation, apiKey)
            GetDirection(url).execute()

            // Create a LatLngBounds object that includes both the bus stop and driver's markers
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(destinationLocation)

            if (lat != 0.0 && lng != 0.0) {
                boundsBuilder.include(LatLng(lat, lng))
            }
            val bounds = boundsBuilder.build()

            // Calculate padding to add some space around the bounds (optional)
            val padding = resources.getDimensionPixelSize(R.dimen.map_padding)

            // Move the camera to fit the bounds with padding
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

            // After the map has been initialized and coordinates are available, add the driver's marker
            if (lat != 0.0 && lng != 0.0) {
                latLng = LatLng(lat, lng)
                addDriverMarker(googleMap, latLng)
            }

        }
    }

    private fun getDirectionURL(origin:LatLng, dest:LatLng, secret: String) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(broadcastReceiver)
    }

    // Override onOptionsItemSelected to handle back button click events
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button click event here
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addDriverMarker(googleMap: GoogleMap, location: LatLng) {
        val scaledWidth = 150 // in pixels
        val scaledHeight = 150 // in pixels
        // custom marker
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.bus)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        // Remove any existing marker from the map
        driverMarker?.remove()

        // Add the new marker to the map
        driverMarker = googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)))
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url : String) : AsyncTask<Void, Void, List<List<LatLng>>>(){
        val durationTextView = findViewById<TextView>(R.id.txt)

        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data,MapData::class.java)
                val path =  ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size){
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
                val duration = respObj.routes[0].legs[0].duration.text
                durationTextView.text = duration

            }catch (e:Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices){
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLACK)
                lineoption.geodesic(true)
            }
            googleMap.addPolyline(lineoption)
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

}