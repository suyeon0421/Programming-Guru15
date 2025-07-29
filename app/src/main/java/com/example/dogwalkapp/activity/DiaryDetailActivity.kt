package com.example.dogwalkapp.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout // LinearLayout import 추가
import com.bumptech.glide.Glide
import com.example.dogwalkapp.R
import com.example.dogwalkapp.base.NavigationActivity
import com.example.dogwalkapp.models.CourseItem
import com.google.android.material.chip.Chip
import java.time.format.DateTimeFormatter
import java.util.Locale

class DiaryDetailActivity : NavigationActivity() {

    private var post: CourseItem? = null

    companion object {
        const val EXTRA_COURSE_ITEM = "extra_course_item"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)


        // 하단 내비게이션 바는 '내주변' 탭이 선택된 상태로 유지
        setupBottomNavigation(R.id.nav_nearby)

        // Intent에서 CourseItem 데이터 받기
        post = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_COURSE_ITEM, CourseItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_COURSE_ITEM) as? CourseItem
        }

        post?.let { p ->

            val recommendTitle: TextView = findViewById(R.id.recommendTitle)
            val mainImageView: ImageView = findViewById(R.id.image_main)
            val locationTextView: TextView = findViewById(R.id.text_location)
            val petNameTextView: TextView = findViewById(R.id.text_user)
            val distanceTextView: TextView = findViewById(R.id.text_distance)
            val timeTextView: TextView = findViewById(R.id.text_time)
            val styleChip: Chip = findViewById(R.id.chip_tag1)
            val pathChip: Chip = findViewById(R.id.chip_tag2)


            // 데이터 채우기
            val imageUrl = p.imageUrl
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .into(mainImageView)
            } else {
                mainImageView.visibility = View.GONE
            }

            locationTextView.text = "서울 노원구 공릉동" // CourseItem에 위치 정보 필드가 없다면 이대로 유지
            petNameTextView.text = "${p.petName ?: "강아지"} 보호자님"
            distanceTextView.text = String.format(Locale.getDefault(), "%.1fkm", p.distance)

            val minutes = (p.duration % 3600) / 60
            timeTextView.text = String.format(Locale.getDefault(), "%02d분", minutes)

            styleChip.text = p.walkStyle ?: "미지정"
            pathChip.text = p.pathReview ?: "미지정"

        } ?: run {
            // post 데이터가 없는 경우 처리
            Toast.makeText(this, "게시물 정보를 불러올 수 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            finish() // 액티비티 종료
        }
    }
}