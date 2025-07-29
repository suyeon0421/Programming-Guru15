package com.example.dogwalkapp.activity

import android.app.Activity
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import android.os.Bundle
import android.util.Log
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
import com.example.dogwalkapp.base.NavigationActivity
import com.example.dogwalkapp.models.CourseItem
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp // Timestamp import 추가
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId // ZoneId import 추가


//메인 화면
class MainActivity : NavigationActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    //미니맵
    private lateinit var miniMapView: MapView
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"

    //산책 하러 가기 뷰
    private lateinit var courseAdapter: CourseAdapter

    private lateinit var dogNameTextView: TextView
    private lateinit var locationTextView: TextView


    private lateinit var auth: FirebaseAuth
    private var db = FirebaseFirestore.getInstance()

    //위치 권한 활성화
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    //주간 달력 뷰
    private lateinit var weekAdapter: WeekAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById<TextView>(R.id.locationText)
        dogNameTextView = findViewById<TextView>(R.id.dogNameText)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupBottomNavigation(R.id.nav_home)

        setupCourseRecyclerView()
        setupWeekRecyclerView()


        locationTextView.text = "서울시 노원구"

        loadDogNameFromFirestore()


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

        db.collection("courses")
            .get()
            .addOnSuccessListener { result ->
                Log.d("FirestoreTest", "데이터 조회 성공, 문서 수: ${result.size()}")
                // 데이터 처리 코드...
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreTest", "데이터 조회 실패", exception)
            }




    }

    private fun loadDogNameFromFirestore() {
        val currentUser = auth.currentUser
        if(currentUser != null) {
            val uid = currentUser.uid

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        //pet 필드가 Map 형태로 저장
                        val petMap = document.get("pet") as? Map<String, Any>
                        val dogName = petMap?.get("name") as? String

                        if (!dogName.isNullOrEmpty()) {
                            dogNameTextView.text = "$dogName, \n산책을 기다려요!"
                        } else {
                            dogNameTextView.text = "사랑하는 반려견이\n산책을 기다려요!"
                        }
                    } else {
                        dogNameTextView.text = "사용자 정보를 불러올 수 없습니다."
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "반려견 정보 로드 실패: ${exception.message}", Toast.LENGTH_LONG).show()
                }
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
           val intent = Intent(this, DiaryCommunityActivity::class.java)
           startActivity(intent)
       }

       val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)

       recyclerView.layoutManager =
           LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
       recyclerView.adapter = courseAdapter

       fetchCourses()
   }

    private fun setupWeekRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.weekRecyclerView)

        // 가로 스크롤 LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 날짜 리스트, 산책한 날짜 리스트 초기화
        val weekDates = getThisWeekDates()
        val walkedDates = listOf<LocalDate>() // 실제 산책 날짜 리스트 넣어주세요

        // 어댑터 생성 (클릭 이벤트 포함)
        weekAdapter = WeekAdapter(weekDates, walkedDates) { selectedDate ->
            val intent = Intent(this, DiaryCalendarActivity::class.java)
            startActivity(intent)
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
        val dummyCourseList = listOf(
            CourseItem(
                date = LocalDate.of(2025, 7, 27),
                distance = 1500.0,
                duration = 1800L,
                petName = "치치",
                title = "가벼운 한 바퀴👣"
            ),
            CourseItem(
                date = LocalDate.of(2025, 7, 25),
                distance = 2000.0,
                duration = 3600L,
                petName = "별이",
                title = "푸르른 산책길🌳"
            ),
            CourseItem(
                date = LocalDate.of(2025, 7, 23),
                distance = 2200.0,
                duration = 3000L,
                petName = "베티",
                title = "마라톤 선수급😓"
            ),
        )
        courseAdapter.setItems(dummyCourseList)
    }

    fun getThisWeekDates(): List<LocalDate> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value
        val monday = today.minusDays((dayOfWeek - 1).toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
}