package com.example.scheduletracking

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        // Retrieve data from Firebase
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("user_location")
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // Get the last known location of the driver
                val lat = dataSnapshot.child("latitude").value as? Double
                val lng = dataSnapshot.child("longitude").value as? Double

                // Create an Intent to update the coordinates in the MainActivity
                val intent = Intent("UPDATE_COORDINATES")


                if (lat != null && lng != null) {
                    intent.putExtra("latitude", lat)
                    intent.putExtra("longitude", lng)
                    sendBroadcast(intent)
                }
                else{
                    intent.putExtra("latitude", 0.0)
                    intent.putExtra("longitude", 0.0)
                    sendBroadcast(intent)
                }

                // Finish the job
                jobFinished(params, false)
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle errors here
            }
        })
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Cancel the job if it's stopped before completion
        return true
    }
}