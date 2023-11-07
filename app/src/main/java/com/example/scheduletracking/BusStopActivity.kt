package com.example.scheduletracking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList

class BusStopActivity : AppCompatActivity() {

    companion object {
        private lateinit var recyclerView: RecyclerView
        private lateinit var searchView: SearchView
        private var mList = ArrayList<BusStopData>()
        private lateinit var adapter: BusStopAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_stop)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Display the back button with an arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Choose Bus Stop"

        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        addDataToList()
        adapter = BusStopAdapter(mList){ clickedBusStopData ->
            clickedBusStopData?.let { // add null check
                // Create an Intent to start the new activity
                val intent = Intent(this@BusStopActivity, RouteActivity::class.java)
                // Add the clicked BusStopData as an extra to the intent
                intent.putExtra("busStopData", it)
                // Start the new activity with the intent
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }

        })
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

    private fun filterList(query: String?) {

        if (query != null) {
            val filteredList = ArrayList<BusStopData>()
            for (i in mList) {
                if (i.title.lowercase(Locale.ROOT).contains(query)) {
                    filteredList.add(i)
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No Data found", Toast.LENGTH_SHORT).show()
            } else {
                adapter.setFilteredList(filteredList)
            }
        }
    }

    private fun addDataToList(){
        mList.clear() // Clear the existing list before adding new data

        mList.add(BusStopData("ATM / Library", R.drawable.pin,2.9776232,101.7336344))
        mList.add(BusStopData("Admin", R.drawable.pin, 2.97678,101.7271))
        mList.add(BusStopData("Murni", R.drawable.pin, 2.97551,101.72919))
        mList.add(BusStopData("COE", R.drawable.pin, 2.97538,101.72915))
        mList.add(BusStopData("BW", R.drawable.pin, 2.962758,101.725643))
        mList.add(BusStopData("Amanah", R.drawable.pin, 2.9657615,101.7286221))
        mList.add(BusStopData("DSS / Cendi", R.drawable.pin, 2.9679094, 101.7307))
    }
}