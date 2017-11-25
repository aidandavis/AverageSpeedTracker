package com.aidandavisdev.aidandavis.averagespeedtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.WindowManager

//todo: persistence when activity closed (background service?)

class MainActivity : AppCompatActivity() {

    private lateinit var speedTracker: SpeedTracker

    // set average speed
    private var currentAverageSpeed: Double = 0.0
    // point history (for graphing), List<SpeedTimePair> speed, seconds since time of first point
    // above/below (prevTotal + ((setAverageSpeed-currentSpeed)*seconds since last point))
    private var timeOfFirstPoint: Long = 0
    private var timeOfLastPoint: Long = 0
    private var timeOfNewPoint: Long = 0
    private var currentSpeed: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // keep screen on
        setContentView(R.layout.activity_main)

        speedTracker = object : SpeedTracker(this, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) {
            override fun onGPSDisabled() {
            }

            override fun onGPSWaiting() {
            }

            override fun onGPSFix() {
            }

            override fun onSpeedChanged() {
                currentSpeed = speedKMH
                updateTimes()
                currentAverageSpeed = ((currentAverageSpeed * (timeOfLastPoint - timeOfFirstPoint)) + currentSpeed) / (timeOfNewPoint - timeOfFirstPoint)

                // do above/below
                // add point to graph
                // update display fields
            }
        }
    }

    private fun updateTimes() {
        timeOfLastPoint = timeOfNewPoint
        timeOfNewPoint = System.currentTimeMillis()
        if (timeOfFirstPoint == 0L) {
            timeOfFirstPoint = timeOfNewPoint
        }
    }

    private fun requestPermission(): Boolean {
        val hasPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        if (!hasPermission){
            val fineRequestCode = 1
            val finePermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this, finePermission, fineRequestCode)

            //todo: look into callbacks for permissions
        }
        return hasPermission
    }

    override fun onResume() {
        super.onResume()
        requestPermission()
        speedTracker.startTracking()
    }

    override fun onPause() {
        super.onPause()
        speedTracker.stopTracking()
    }
}
