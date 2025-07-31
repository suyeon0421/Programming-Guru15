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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLngBounds
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class WalkActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private lateinit var tvTimer: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvKcal: TextView
    private lateinit var btnStart: Button
    private lateinit var minimapImageView: ImageView

    private var tracking = false
    private var polyline: Polyline? = null
    private val pathPoints = mutableListOf<LatLng>()

    private var totalDistance = 0.0
    private var startTime = 0L
    private var caloriesValue = 0
    private var timerRunning = false



    private val handler = Handler(Looper.getMainLooper()) // Looper.getMainLooper() 명시
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

        minimapImageView = findViewById(R.id.minimap_imageView)

        tvTimer = findViewById(R.id.tv_timer)
        tvSpeed = findViewById(R.id.tv_speed)
        tvDistance = findViewById(R.id.tv_distance)
        tvKcal = findViewById(R.id.tv_kcal)
        btnStart = findViewById(R.id.btn_start_walk)

        val minimapImageUriString = intent.getStringExtra(DiaryDetailActivity.EXTRA_MINIMAP_IMAGE_URI)
        if (!minimapImageUriString.isNullOrEmpty()) {
            // DiaryDetailActivity에서 보낸 URI가 있을 경우에만 실행
            val minimapUri = Uri.parse(minimapImageUriString)
            Glide.with(this)
                .load(minimapUri)
                .into(minimapImageView)
            minimapImageView.visibility = View.VISIBLE // 이미지가 있다면 보이도록 설정
        } else {
            // URI가 없을 경우
            minimapImageView.visibility = View.GONE // 미니맵을 숨김
        }

        // LocationCallback 초기화
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    pathPoints.add(latLng)

                    if (polyline == null) {
                        polyline = map.addPolyline(
                            PolylineOptions()
                                .color(Color.BLUE)
                                .width(10f)
                        )
                        Log.d("PolylineDebug", "Polyline 생성됨")
                    }
                    polyline?.points = pathPoints
                    Log.d("PolylineDebug", "Polyline 점 갯수: ${pathPoints.size}")

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

                    if (pathPoints.size >= 2) {
                        val last = pathPoints[pathPoints.size - 2]
                        val current = pathPoints.last()
                        val distanceResult = FloatArray(1)
                        Location.distanceBetween(
                            last.latitude, last.longitude,
                            current.latitude, current.longitude,
                            distanceResult
                        )
                        totalDistance += distanceResult[0].toDouble()
                        updateMetrics()
                    }
                }
            }
        }

        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnStart.setOnClickListener {
            if (!tracking) {
                //위치 권한이 있다면
                if (checkLocationPermission()) {
                    startTracking()
                    btnStart.text = "산책 끝내기" // 시작 시 버튼 텍스트 변경
                    //위치 권한이 없다면
                } else {
                    Toast.makeText(this, "위치 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
                }
            } else {
                stopTracking()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // 권한이 있다면 내 위치 표시 및 버튼 활성화
        if (checkLocationPermission()) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        } else {
            // 권한이 없다면 내 위치 기능 비활성화 또는 메시지 표시
            Log.w("WalkActivity", "Location permission not granted. MyLocation layer will be disabled.")
        }
    }

    // 권한 확인 함수
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // checkLocationPermission()으로 권한을 확인했으므로 lint 경고 무시
    private fun startTracking() {
        if (!::map.isInitialized) {
            Toast.makeText(this, "지도가 준비되지 않았습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // MainActivity에서 권한을 받았다는 가정하에, 여기서 다시 한 번 확인합니다.
        if (!checkLocationPermission()) {
            Toast.makeText(this, "위치 권한이 없어 산책을 시작할 수 없습니다. 앱 설정에서 권한을 확인해주세요.", Toast.LENGTH_LONG).show()
            return
        }

        map.clear()
        polyline = null // 새로운 산책 시작 시 이전 polyline 제거
        pathPoints.clear()
        totalDistance = 0.0
        caloriesValue = 0
        startTime = SystemClock.elapsedRealtime()
        timerRunning = true
        handler.post(timerRunnable)

        tracking = true
        // 위치 업데이트 요청 시작
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Toast.makeText(this, "산책을 시작합니다!", Toast.LENGTH_SHORT).show()
    }


    //트래킹 멈추기
    private fun stopTracking() {
        tracking = false
        timerRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(timerRunnable)

        val elapsedTime = SystemClock.elapsedRealtime() - startTime
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "test-uid"

        //카메라 위치 조정
        if (pathPoints.isEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            for (point in pathPoints) {
                boundsBuilder.include(point)
            }
            val bounds = boundsBuilder.build()

            val padding = 500 //픽셀 단위
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,padding)

            map.moveCamera(cameraUpdate)
        } else {
            // 경로가 없다면 기본 줌 레벨로 변경
            if (map.cameraPosition.zoom > 15f) {
                val currentLatLng = map.cameraPosition.target
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }

        // 지도 캡처 후 Bitmap 저장 → Uri로 변환 → Intent로 전달
        map.snapshot { bitmap ->
            if (bitmap == null) {
                Toast.makeText(this, "지도 캡처에 실패했어요.", Toast.LENGTH_SHORT).show()
                return@snapshot
            }

            val imageFile = saveBitmapToFile(bitmap)
            val imageUri = imageFile?.toUri()

            if (imageUri == null) {
                Toast.makeText(this, "이미지 저장에 실패했어요.", Toast.LENGTH_SHORT).show()
                return@snapshot
            }

            val courseItem = CourseItem(
                date = LocalDate.now(), // 현재 날짜 사용
                pathPoints = pathPoints.toList(),
                distance = totalDistance,
                duration = elapsedTime / 1000,
                calories = caloriesValue,
                createdBy = uid,
                timestamp = Timestamp.now()
            )

            val intent = Intent(this, DiaryWriteActivity::class.java).apply {
                putExtra("courseItem", courseItem)
                putExtra("mapImageUri", imageUri.toString())
            }

            startActivity(intent)
            finish()
        }
        Toast.makeText(this, "산책을 종료합니다!", Toast.LENGTH_SHORT).show()
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        val fileName = "walk_map_${System.currentTimeMillis()}.png"
        val file = File(cacheDir, fileName)
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WalkActivity", "비트맵 파일 저장 실패: ${e.message}")
            null
        }
    }

    private fun updateMetrics() {

        //산책한 총 거리, km단위로 나타냄
        val distanceKm = totalDistance / 1000.0

        //현재시간 - 시작시간 -> 산책이 진행된 시간이 나타남
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0

        //이동 거리(M/s) -> 초당 몇 미터 이동했는지 계산
        val speedMps = if (elapsedSeconds > 0) (totalDistance / elapsedSeconds) else 0.0

        //M/s -> Km/h로 변환
        val speedKmph = speedMps * 3.6

        //킬로미터 당 소요시간(M/km)
        val paceMinutesPerKm = if (speedKmph > 0) (60.0 / speedKmph) else 0.0

        //킬로미터 당 약 60kcal 소모한다고 가정
        val kcal = (distanceKm * 60).roundToInt()

        caloriesValue = kcal

        tvDistance.text = String.format("%.2f km", distanceKm)
        val minutes = paceMinutesPerKm.toInt()
        val seconds = ((paceMinutesPerKm - minutes) * 60).roundToInt()
        tvSpeed.text = String.format("%d'%02d' ", minutes, seconds)
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