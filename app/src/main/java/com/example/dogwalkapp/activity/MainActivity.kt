package com.example.dogwalkapp.activity

import android.app.Activity
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.example.dogwalkapp.adapter.CourseAdapter
import com.example.dogwalkapp.adapter.WeekAdapter
import com.example.dogwalkapp.models.CourseItem
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.DayOfWeek


//메인 화면
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    //미니맵
    private lateinit var miniMapView: MapView
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"

    //산책 하러 가기 뷰
    private lateinit var courseAdapter: CourseAdapter
    private val db = FirebaseFirestore.getInstance()

    //위치 권한 활성화
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    //주간 달력 뷰
    private lateinit var weekAdapter: WeekAdapter

    val location = "서울시 노원구"
    val dogName = "시고르자브종"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupCourseRecyclerView()
        setupWeekRecyclerView()

        val locationTextView = findViewById<TextView>(R.id.locationText)
        val dogNameTextView = findViewById<TextView>(R.id.dogNameText)

        dogNameTextView.text = "$dogName,\n산책을 기다려요!"
        locationTextView.text = "$location"


        var mapViewBundel: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundel = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        miniMapView = findViewById(R.id.miniMapView)
        miniMapView.onCreate(mapViewBundel)
        miniMapView.getMapAsync(this)

        findViewById<Button>(R.id.startWalkButton).setOnClickListener {
            val intent = Intent(this, WalkActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.recommendArrow).setOnClickListener {
            val intent = Intent(this, DiaryCommunityActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.weeklyStatusArrow).setOnClickListener {
            val intent = Intent(this, DiaryCalendarActivity::class.java)
            startActivity(intent)
        }



    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true

            // 현재 위치로 이동하려면 LocationServices 이용
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val curLatLng = LatLng(loc.latitude, loc.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLatLng, 15f))
                }
            }

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        googleMap.uiSettings.isScrollGesturesEnabled = false
        googleMap.uiSettings.isZoomGesturesEnabled = false
    }


    // MapView 생명주기 연결
    override fun onResume() {
        super.onResume()
        miniMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        miniMapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        miniMapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        miniMapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY) ?: Bundle()
        miniMapView.onSaveInstanceState(mapViewBundle)
        outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
    }

    private fun setupCourseRecyclerView() {
        courseAdapter = CourseAdapter { course ->
            // 아이템 클릭 리스너 (기존과 동일)
            Toast.makeText(this, "${course.title} 코스를 선택하셨습니다.", Toast.LENGTH_SHORT).show()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = courseAdapter

        // --- 여기부터 임시 데이터 생성 및 제출 ---
        val dummyCourses = listOf(
            CourseItem(
                title = "우리 동네 한바퀴 산책",
                distance = 2.5,
                duration = 1800
            ),
            CourseItem(
                title = "공원 옆 숲길 코스",
                distance = 4.0,
                duration = 3000
            ),
            CourseItem(
                title = "강변 따라 걷기",
                distance = 6.2,
                duration = 10800
            ),
            CourseItem(
                title = "카페거리 탐방",
                distance = 3.0,
                duration = 3600
            )
        )
        courseAdapter.setItems(dummyCourses)
    }

 //  private fun setupCourseRecyclerView() {
 //      courseAdapter = CourseAdapter { course ->
 //          Toast.makeText(this, "${course.title} 코스를 선택하셨습니다.", Toast.LENGTH_SHORT).show()

 //      }

 //      val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)

 //      recyclerView.layoutManager =
 //          LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
 //      recyclerView.adapter = courseAdapter

 //      fetchCourses()
 //  }

    private fun setupWeekRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.weekRecyclerView)

        // 가로 스크롤 LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 날짜 리스트, 산책한 날짜 리스트 초기화
        val weekDates = getThisWeekDates()
        val walkedDates = listOf<LocalDate>() // 실제 산책 날짜 리스트 넣어주세요

        // 어댑터 생성 (클릭 이벤트 포함)
        weekAdapter = WeekAdapter(weekDates, walkedDates) { selectedDate ->
            Toast.makeText(this, "선택한 날짜: $selectedDate", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = weekAdapter

        // 아이템 너비를 7개 균등 분할해서 적용하기 위해 뷰가 레이아웃된 후 실행
        recyclerView.post {
            val totalWidth = recyclerView.width - recyclerView.paddingStart - recyclerView.paddingEnd
            val itemWidth = totalWidth / 7

            weekAdapter.setItemWidth(itemWidth)  // 아래에 setItemWidth 함수 구현 필요
        }
    }


    private fun fetchCourses() {


        db.collection("courses")
            .whereEqualTo("region", "공릉동")
            .limit(3)
            .get()
            .addOnSuccessListener { result ->
                val courseList = result.documents.mapNotNull { it.toObject(CourseItem::class.java) }
                courseAdapter.setItems(courseList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "산책 코스를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    fun getThisWeekDates(): List<LocalDate> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value
        val monday = today.minusDays((dayOfWeek - 1).toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
}