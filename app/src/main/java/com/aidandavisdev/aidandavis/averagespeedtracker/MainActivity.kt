package com.aidandavisdev.aidandavis.averagespeedtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

//todo: persistence when activity closed (background service?)
//todo: graph
//todo: permission return


class MainActivity : AppCompatActivity() {

    private lateinit var speedTracker: SpeedTracker

    // set average speed
    private var currentAverage: Double = 0.0
    private var setAverageSpeed: Double = 0.0
    private var speedTimeList: ArrayList<SpeedTimePair> = ArrayList() // point history (for graphing), List<SpeedTimePair> speed, seconds since time of first point
    private var aboveBelow: Long = 0L
    private var timeOfFirstPoint: Long = 0
    private var timeOfLastPoint: Long = 0
    private var timeOfNewPoint: Long = 0
    private var currentSpeed: Double = 0.0

    private lateinit var displayCurrentSpeed: TextView
    private lateinit var displaySetAverage: TextView
    private lateinit var displayCurrentAverage: TextView
    private lateinit var displayAboveBelow: TextView
    private lateinit var setButton: Button
    private lateinit var graph: LineChart

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
                gpsFixToast()
            }

            override fun onSpeedChanged() {
                currentSpeed = speedKMH
                updateTimes()
                addSpeedTimePair()
                if (speedTimeList.size > 3) {
                    calculateCurrentAverageSpeed()
                    calculateAboveBelow()
                }
                updateDisplayFields()
                updateGraph()
                // add point to graph
            }
        }

        displayCurrentSpeed = findViewById(R.id.display_current_speed)
        displaySetAverage = findViewById(R.id.display_set_average)
        displayCurrentAverage = findViewById(R.id.display_current_average)
        displayAboveBelow = findViewById(R.id.display_above_below)

        setButton = findViewById(R.id.button_set_average)
        setButton.setOnClickListener({
            setAverageSpeed = currentSpeed
            resetValues()
            updateDisplayFields()
        })

        graph = findViewById(R.id.lineChart)
    }

    private fun updateGraph() {
        val entries: ArrayList<Entry> = ArrayList()
        speedTimeList.mapTo(entries) { Entry(it.time.toFloat(), it.speed.toFloat()) }

        val dataSet = LineDataSet(entries, "speed")
        dataSet.color = R.color.colorPrimaryDark
        val lineData = LineData(dataSet)

        graph.data = lineData
        graph.invalidate()

    }

    private fun gpsFixToast() {
        Toast.makeText(this, "GPS Fix", Toast.LENGTH_SHORT).show()
    }

    private fun resetValues() {
        currentAverage = 0.0
        timeOfFirstPoint = 0L
        timeOfLastPoint = 0L
        timeOfNewPoint = 0L
        aboveBelow = 0L
        speedTimeList.clear()
        graph.clear()
    }

    private fun updateDisplayFields() {
        displayCurrentSpeed.text = getString(R.string.speed_text_format).format(currentSpeed)
        displaySetAverage.text = getString(R.string.speed_text_format).format(setAverageSpeed)
        displayCurrentAverage.text = getString(R.string.speed_text_format).format(currentAverage)
        displayAboveBelow.text = aboveBelow.toString()
    }

    private fun calculateCurrentAverageSpeed() {
        val prevTotalTime = timeOfLastPoint - timeOfFirstPoint // Long
        val newTotalTime = timeOfNewPoint - timeOfFirstPoint // Long
        if (newTotalTime == 0L) { // prevent singularity
            currentAverage = 0.0
            return
        }
        val oldTotalSpeed = (currentAverage * prevTotalTime) // Double
        val newTotalSpeed = (oldTotalSpeed + currentSpeed) // Double
        currentAverage = (newTotalSpeed / newTotalTime) // Double
    }

    private fun addSpeedTimePair() {
        speedTimeList.add(SpeedTimePair(currentSpeed, (timeOfNewPoint - timeOfFirstPoint) / 1000))
    }

    private fun calculateAboveBelow() {
        aboveBelow += ((setAverageSpeed - currentSpeed).toLong() * ((timeOfNewPoint - timeOfLastPoint) / 1000))
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
        if (!hasPermission) {
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
