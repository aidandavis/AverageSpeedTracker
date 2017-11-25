package com.aidandavisdev.aidandavis.averagespeedtracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

abstract class SpeedTracker(context: Context, private val isTrackingStatus: Boolean) : LocationListener {
    private val mLocationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private lateinit var mGnssStatusCallback: GnssStatus.Callback
    private var isTracking = false

    var speedMPS = 0.0 // m/s
    val speedKMH: Double
        get() = speedMPS * 3.6

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!isTracking) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            if (isTrackingStatus) registerGnssCallback()
            isTracking = true
        }
    }

    @SuppressLint("MissingPermission")
    fun stopTracking() {
        if (isTracking) {
            mLocationManager.removeUpdates(this)
            if (isTrackingStatus) unregisterGnssCallback()
            isTracking = false
            speedMPS = 0.0
            locationBuffer.clear()
        }
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun registerGnssCallback() {
        mGnssStatusCallback = object : GnssStatus.Callback() {
            override fun onFirstFix(ttffMillis: Int) {
                super.onFirstFix(ttffMillis)
                onGPSFix()
            }

            override fun onStarted() {
                super.onStarted()
                onGPSWaiting()
            }
        }
        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback)
    }

    @SuppressLint("NewApi")
    private fun unregisterGnssCallback() {
        mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback)
    }

    private var locationBuffer: ArrayList<Location> = ArrayList()
    private val BUFFER_SIZE = 4

    override fun onLocationChanged(location: Location) {
        locationBuffer.add(location)
        if (isTracking) {
            if (locationBuffer.size <= BUFFER_SIZE) return
            val buffer = locationBuffer.subList(locationBuffer.lastIndex - BUFFER_SIZE, locationBuffer.lastIndex)
            speedMPS = calculateSpeed(buffer)
            onSpeedChanged()
        }
    }

    private fun calculateSpeed(buffer: List<Location>): Double {
        val distance = averageDistanceFromBuffer(buffer) // metres
        val time = (buffer.last().time - buffer.first().time) / 1000 // seconds

        return if ((System.currentTimeMillis() - buffer.last().time) > 10000) {
            // speed goes to 0 if no points received in last 10 seconds
            onGPSWaiting()
            0.0
        } else (distance / time.toDouble())
    }

    private fun averageDistanceFromBuffer(buffer: List<Location>): Double {
        val totalDistance = buffer.indices
                .filter { it != 0 } // skip first element
                .map { buffer[it - 1].distanceTo(buffer[it]) } // distance from previous element to this one
                .sum()
        return (totalDistance / buffer.size).toDouble()
    }

    // anonymous functions so calling class knows when speed is changed, gps is fixed, etc
    abstract fun onSpeedChanged()
    abstract fun onGPSDisabled()
    abstract fun onGPSWaiting()
    abstract fun onGPSFix()

    override fun onProviderDisabled(s: String) {
        onGPSDisabled()
    }

    /* unused but required */
    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
    }

    override fun onProviderEnabled(s: String) {
    }
}
