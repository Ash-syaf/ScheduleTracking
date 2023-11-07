package com.example.scheduletracking

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity() {

    companion object {
        private const val JOB_ID = 123
        private lateinit var googleMap: GoogleMap
        private lateinit var mapFragment: SupportMapFragment
        private lateinit var broadcastReceiver: BroadcastReceiver
        private lateinit var binding: MainActivity
        private var driverMarker: Marker? = null
        private lateinit var latLng: LatLng
        private lateinit var txt: TextView
        private lateinit var statusTv: TextView
        private lateinit var button: Button
        private var lat: Double = 0.0
        private var lng: Double = 0.0
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
        }

        txt = findViewById(R.id.txt)
        statusTv = findViewById(R.id.text)
        button = findViewById(R.id.busStop)
        button.setOnClickListener {
            val intent = Intent(this, BusStopActivity::class.java)
            startActivity(intent)
        }

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
        }

        // Register a BroadcastReceiver to receive updates from the FirebaseJobService
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Retrieve the latitude and longitude from the Intent
                lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
                lng = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0

                // Display the latitude and longitude in a TextView
                txt.text = "Latitude: $lat, Longitude: $lng"

                // Add marker if coordinates not null
                if(lat != 0.0 && lng != 0.0){
                    latLng = LatLng(lat, lng)
                    addDriverMarker(googleMap, latLng)

                    // Update the camera position to center on the driver's location
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                    statusTv.text = "Bus is on the way"

                    // Show the button
                    button.visibility = View.VISIBLE
                }
                else{
                    statusTv.text = "No online Drivers"
                    // Update the map
                    driverMarker?.remove()
                    // Hide the button
                    button.visibility = View.GONE
                }
            }
        }
        val intentFilter = IntentFilter("UPDATE_COORDINATES")
        registerReceiver(broadcastReceiver, intentFilter)

    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(broadcastReceiver)
    }

    private fun addDriverMarker(googleMap: GoogleMap, location: LatLng) {
        val scaledWidth = 150 // in pixels
        val scaledHeight = 150 // in pixels
        // custom marker
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.bus)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        // Remove any existing marker from the map
        driverMarker
        driverMarker?.remove()

        // Add the new marker to the map
        driverMarker = googleMap.addMarker(
            MarkerOptions()
            .position(location)
            .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)))
    }
}