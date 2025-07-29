package com.example.dogwalkapp.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
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

    private lateinit var mapImageView: ImageView

    private lateinit var chipStyleGroup: ChipGroup
    private lateinit var chipPathGroup: ChipGroup
    private lateinit var etMemo: EditText
    private lateinit var btnSave: Button

    private lateinit var courseItem: CourseItem
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_write)

        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvCalories = findViewById<TextView>(R.id.tvCalories)
        mapImageView = findViewById(R.id.imageMapResult)

        val mapImageUriStr = intent.getStringExtra("mapImageUri")
        val mapImageUri = mapImageUriStr?.toUri()

        courseItem = intent.getParcelableExtra<CourseItem>("courseItem") ?: run {
            Toast.makeText(this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Glide.with(this)
            .load(mapImageUri)
            .into(findViewById(R.id.imageMapResult))


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
        return if (checkedChipId != -1) {
            val chip = chipGroup.findViewById<Chip>(checkedChipId)
            chip?.text.toString()
        } else ""
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

    private fun saveCourseToFirebase(course: CourseItem) {
        val uid = auth.currentUser?.uid ?: return
        val documentRef = db.collection("courses").document() // 자동 ID 생성

        val data = hashMapOf(
            "createdBy" to uid,
            "distance" to course.distance,
            "duration" to course.duration,
            "calories" to course.calories,
            "imageUrl" to course.imageUrl,
            "petName" to course.petName,
            "walkStyle" to course.walkStyle,
            "pathReview" to course.pathReview,
            "memo" to course.memo,
            "timestamp" to course.timestamp,
            // 변환된 필드들
            "pathPoints" to course.pathPoints.map { GeoPoint(it.latitude, it.longitude) },
            "date" to course.date.toString() // 예: "2025-07-28"
        )

        documentRef.set(course, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "산책 기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                finish() // 저장 후 종료
            }
            .addOnFailureListener {
                Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
