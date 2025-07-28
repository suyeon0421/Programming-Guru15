package com.example.dogwalkapp.activity

import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dogwalkapp.activity.DiaryWriteActivity
import com.example.dogwalkapp.R
import com.example.dogwalkapp.models.CourseItem
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

class WalkActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var tvTimer: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvKcal: TextView
    private lateinit var btnStart: Button

    private var tracking = false
    private var polyline: Polyline? = null
    private val pathPoints = mutableListOf<LatLng>()

    private var totalDistance = 0.0
    private var startTime = 0L
    private var caloriesValue = 0
    private var timerRunning = false

    private val handler = Handler()
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (timerRunning) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                updateTimerText(elapsed)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walk)

        tvTimer = findViewById(R.id.tv_timer)
        tvSpeed = findViewById(R.id.tv_speed)
        tvDistance = findViewById(R.id.tv_distance)
        tvKcal = findViewById(R.id.tv_kcal)
        btnStart = findViewById(R.id.btn_start_walk)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnStart.setOnClickListener {
            if (!tracking) {
                startTracking()
                btnStart.text = "산책 끝내기"
            } else {
                stopTracking()
                // btnStart.text = "산책 시작하기" // finish() 이후니까 무의미
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        map.clear()
        pathPoints.clear()
        totalDistance = 0.0
        caloriesValue = 0
        startTime = SystemClock.elapsedRealtime()
        timerRunning = true
        handler.post(timerRunnable)

        polyline = map.addPolyline(
            PolylineOptions()
                .color(Color.BLUE)
                .width(10f)
        )

        tracking = true

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    pathPoints.add(latLng)
                    polyline?.points = pathPoints
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

                    if (pathPoints.size >= 2) {
                        val last = pathPoints[pathPoints.size - 2]
                        val current = pathPoints.last()
                        val result = FloatArray(1)
                        Location.distanceBetween(
                            last.latitude, last.longitude,
                            current.latitude, current.longitude,
                            result
                        )
                        totalDistance += result[0].toDouble()
                        updateMetrics()
                    }
                }
            }
        }

        val request = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopTracking() {
        tracking = false
        timerRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(timerRunnable)

        val elapsedTime = SystemClock.elapsedRealtime() - startTime
       // val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "test-uid"

        val courseItem = CourseItem(
            pathPoints = pathPoints.toList(),
            distance = totalDistance,
            duration = elapsedTime / 1000, // 밀리초를 초 단위로 변환
            calories = caloriesValue,
            createdBy = uid,
            timestamp = Timestamp.now()
        )

        val intent = Intent(this, DiaryWriteActivity::class.java)
        intent.putExtra("courseItem", courseItem)
        startActivity(intent)
        finish()
    }


    private fun updateMetrics() {
        val distanceKm = totalDistance / 1000.0
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0
        val speed = if (elapsedSeconds > 0) (totalDistance / elapsedSeconds) else 0.0
        val pace = if (speed > 0) (1000 / speed) / 60 else 0.0
        val kcal = (distanceKm * 60).roundToInt()

        caloriesValue = kcal

        tvDistance.text = String.format("%.2f km", distanceKm)
        tvSpeed.text = if (pace > 0) String.format("%.0f’%02.0f’’", pace.toInt(), (pace * 60 % 60)) else "0’00’’"
        tvKcal.text = "$kcal kcal"
    }

    private fun updateTimerText(elapsedMillis: Long) {
        val totalSeconds = elapsedMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        handler.removeCallbacks(timerRunnable)
    }
}
