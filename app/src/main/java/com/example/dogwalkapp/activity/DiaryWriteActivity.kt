package com.example.dogwalkapp.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


public class DiaryWriteActivity : AppCompatActivity() {

    private lateinit var mapImageView: ImageView

    private lateinit var chipStyleGroup: ChipGroup
    private lateinit var chipPathGroup: ChipGroup
    private lateinit var etMemo: EditText
    private lateinit var btnSave: Button

    private lateinit var courseItem: CourseItem
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var storageRef: StorageReference

    private var currentPetName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_write)

        storageRef = FirebaseStorage.getInstance().reference

        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvCalories = findViewById<TextView>(R.id.tvCalories)
        mapImageView = findViewById(R.id.imageMapResult)

        val mapImageUriStr = intent.getStringExtra("mapImageUri")
        val mapImageUri = mapImageUriStr?.toUri()

        loadDogNameFromFirestore()

        courseItem = intent.getParcelableExtra<CourseItem>("courseItem") ?: run {
            Toast.makeText(this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (mapImageUri != null) {
            Glide.with(this)
                .load(mapImageUri) // 클래스 멤버 변수 사용
                .into(mapImageView) // mapImageView에 직접 로드
        } else {
            // 이미지가 없는 경우를 대비한 처리 (예: 기본 이미지 표시 또는 메시지)
            mapImageView.setImageResource(R.drawable.ic_launcher_background) // 예시
            Toast.makeText(this, "지도 이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e("DiaryWriteActivity", "WalkActivity로부터 유효한 mapImageUri를 받지 못함.")
        }


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
                memo = memo,
                petName = currentPetName ?: "알 수 없음",
                timestamp = com.google.firebase.Timestamp.now()
            )

            // 이미지 업로드 후 DB 저장
            if (mapImageUri != null) {
                uploadImageToFirebaseStorage(mapImageUri!!, updatedCourseItem)
            } else {
                // 이미지가 없는 경우, 이미지 없이 데이터만 저장
                saveCourseToFirebase(updatedCourseItem)
            }
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

    //URL 얻는 함수
    private fun uploadImageToFirebaseStorage(imageUri: android.net.Uri, course: CourseItem) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "사용자 인증 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "walk_maps/${uid}_${System.currentTimeMillis()}.png"
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d("DiaryWriteActivity", "Storage 업로드 성공, 다운로드 URL: $downloadUri")
                    // CourseItem에 다운로드 URL 업데이트
                    val updatedCourseWithImageUrl = course.copy(imageUrl = downloadUri.toString())
                    saveCourseToFirebase(updatedCourseWithImageUrl) // 업데이트된 CourseItem으로 DB 저장
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiaryWriteActivity", "Storage 업로드 실패: ${e.message}")
                Toast.makeText(this, "지도 이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                // 이미지 업로드 실패 시에도 나머지 데이터는 저장할지 결정.
                // 이 경우 imageUrl은 null이거나 이전 값 그대로 남게 됩니다.
                saveCourseToFirebase(course)
            }
    }

    private fun loadDogNameFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        //pet 필드가 Map 형태로 저장
                        val petMap = document.get("pet") as? Map<String, Any>
                        val dogName = petMap?.get("name") as? String
                        currentPetName = dogName
                    } else {
                        currentPetName = "알 수 없음"
                        Log.w("DiaryWriteActivity", "사용자 문서에 pet 정보가 없습니다.")
                    }
                }
                .addOnFailureListener { e ->
                    currentPetName = "알 수 없음"
                    Log.e("DiaryWriteActivity", "강아지 이름 로드 실패: ${e.message}")
                }
        }
    }

    private fun saveCourseToFirebase(course: CourseItem) {
        val uid = auth.currentUser?.uid ?: return
        val documentRef = db.collection("courses").document() // 자동 ID 생성

        val dataToSave = hashMapOf(
            "calories" to course.calories,
            "createdBy" to course.createdBy,
            "date" to hashMapOf( // LocalDate를 Map으로 변환
                "year" to course.date.year,
                "month" to course.date.month.toString(),
                "monthValue" to course.date.monthValue,
                "dayOfMonth" to course.date.dayOfMonth,
                "dayOfWeek" to course.date.dayOfWeek.toString(),
                "dayOfYear" to course.date.dayOfYear,
                "era" to course.date.era.toString(),
                "chronology" to hashMapOf("id" to "ISO", "calendarType" to "iso8601"),
                "leapYear" to course.date.isLeapYear,
            ),
            "distance" to course.distance,
            "duration" to course.duration,
            "imageUrl" to course.imageUrl,
            "memo" to course.memo,
            "pathPoints" to course.pathPoints.map { GeoPoint(it.latitude, it.longitude) }, // List<LatLng> to List<GeoPoint>
            "pathReview" to course.pathReview,
            "petName" to course.petName,
            "timestamp" to course.timestamp,
            "title" to course.title, // CourseItem에 title이 있다면 추가
            "walkStyle" to course.walkStyle
            )

        documentRef.set(dataToSave, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "산책 기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish() // 저장 후 종료
            }
            .addOnFailureListener {
                Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
