package com.example.dogwalkapp.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dogwalkapp.models.CourseItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.dogwalkapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.GeoPoint
import com.google.type.LatLng


public class DiaryWriteActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    private lateinit var chipStyleGroup: ChipGroup
    private lateinit var chipPathGroup: ChipGroup
    private lateinit var etMemo: EditText
    private lateinit var btnSave: Button

    private lateinit var course: CourseItem

    private lateinit var courseItem: CourseItem
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_write)

        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvCalories = findViewById<TextView>(R.id.tvCalories)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        courseItem = intent.getParcelableExtra<CourseItem>("courseItem") ?: run {
            Toast.makeText(this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mapView.getMapAsync { googleMap ->
            // courseItem.pathPoints는 com.google.type.LatLng 타입일 거니까
            val pathPointsGms: List<com.google.android.gms.maps.model.LatLng> = courseItem.pathPoints.map { p ->
                com.google.android.gms.maps.model.LatLng(p.latitude, p.longitude)
            }

            showPathOnMap(googleMap, pathPointsGms)
        }


        //지도에 pathPoints 표시
        val pathPoins = courseItem.pathPoints

        //거리: Km 단위
        val distanceKm = courseItem.distance / 1000.0
        tvDistance.text = String.format("%.2f km", distanceKm)

        //시간: HH:MM:SS 단위
        val duration = courseItem.duration
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        tvDuration.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        tvCalories.text = "${courseItem.calories} kcal"


        // View 연결
        chipStyleGroup = findViewById(R.id.chipGroupStyle)
        chipPathGroup = findViewById(R.id.chipGroupPath)
        etMemo = findViewById(R.id.etMemo)
        btnSave = findViewById(R.id.btnEndWalk)


        // 기존에 저장된 칩 선택과 메모가 있으면 화면에 반영
        setChipGroupSelection(chipStyleGroup, courseItem.walkStyle)
        setChipGroupSelection(chipPathGroup, courseItem.pathReview)
        etMemo.setText(courseItem.memo)

        btnSave.setOnClickListener {
            // 사용자가 선택한 칩 텍스트 가져오기
            val selectedStyle = getSelectedChipText(chipStyleGroup)
            val selectedPath = getSelectedChipText(chipPathGroup)
            val memo = etMemo.text.toString()

            if (selectedStyle.isEmpty() || selectedPath.isEmpty()) {
                Toast.makeText(this, "모든 항목을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // courseItem 복사해서 업데이트할 값 반영
            val updatedCourseItem = courseItem.copy(
                walkStyle = selectedStyle,
                pathReview = selectedPath,
                memo = memo
            )

            saveCourseToFirebase(updatedCourseItem)

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    // ChipGroup에서 선택된 Chip의 텍스트를 반환하는 함수
    private fun getSelectedChipText(chipGroup: ChipGroup): String {
        val checkedChipId = chipGroup.checkedChipId
        if (checkedChipId != View.NO_ID) {
            val chip = chipGroup.findViewById<Chip>(checkedChipId)
            return chip?.text.toString()
        }
        return ""
    }


    // ChipGroup 내에서 텍스트와 같은 Chip을 찾아 선택 상태로 만드는 함수
    private fun setChipGroupSelection(chipGroup: ChipGroup, text: String) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.text == text) {
                chip.isChecked = true
                break
            }
        }
    }

    // 경로 그리는 함수
    private fun showPathOnMap(map: GoogleMap, pathPoints: List<com.google.android.gms.maps.model.LatLng>) {

    val polylineOptions = PolylineOptions()
            .color(Color.BLUE)
            .width(8f)
            .addAll(courseItem.pathPoints)

        map.addPolyline(polylineOptions)

        if (pathPoints.isNotEmpty()) {
            val bounds = LatLngBounds.builder()
            pathPoints.forEach { bounds.include(it) }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        }
    }

    private fun saveCourseToFirebase(course: CourseItem) {
        val uid = auth.currentUser?.uid ?: return
        val documentRef = db.collection("courses").document() // 자동 ID 생성

        documentRef.set(course, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "산책 기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                finish() // 저장 후 종료
            }
            .addOnFailureListener {
                Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

        override fun onResume() {
            super.onResume()
            mapView.onResume()
        }

        override fun onPause() {
            super.onPause()
            mapView.onPause()
        }

        override fun onDestroy() {
            super.onDestroy()
            mapView.onDestroy()
        }

        override fun onLowMemory() {
            super.onLowMemory()
            mapView.onLowMemory()
        }
    }
