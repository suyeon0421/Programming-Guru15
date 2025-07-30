package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.Button // ✅ Button import 추가
import com.bumptech.glide.Glide
import com.example.dogwalkapp.R
import com.example.dogwalkapp.base.NavigationActivity
import com.example.dogwalkapp.models.CourseItem
import com.google.android.material.chip.Chip
import java.time.format.DateTimeFormatter
import java.util.Locale
// import android.graphics.Bitmap // 이제 필요 없음 (이미지 캡처 안 함)
import android.net.Uri // Uri는 계속 필요


class DiaryDetailActivity : NavigationActivity() {

    private var post: CourseItem? = null

    companion object {
        const val EXTRA_COURSE_ITEM = "extra_course_item"
        const val EXTRA_MINIMAP_IMAGE_URI = "extra_minimap_image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)

        setupBottomNavigation(R.id.nav_nearby)

        post = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_COURSE_ITEM, CourseItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_COURSE_ITEM) as? CourseItem
        }

        post?.let { p ->
            val mainImageView: ImageView = findViewById(R.id.image_main)
            val locationTextView: TextView = findViewById(R.id.text_location)
            val petNameTextView: TextView = findViewById(R.id.text_user)
            val distanceTextView: TextView = findViewById(R.id.text_distance)
            val timeTextView: TextView = findViewById(R.id.text_time)
            val styleChip: Chip = findViewById(R.id.chip_tag1)
            val pathChip: Chip = findViewById(R.id.chip_tag2)
            val startWalkButton: Button = findViewById(R.id.btnEndWalk) // ✅ 버튼 ID 참조

            // 데이터 채우기
            val imageUrl = p.imageUrl
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .into(mainImageView)
                mainImageView.visibility = View.VISIBLE
            } else {
                mainImageView.visibility = View.GONE
            }

            locationTextView.text = "서울 노원구 공릉동"
            petNameTextView.text = "${p.petName ?: "강아지"} 보호자님"
            val distanceKm = p.distance / 1000.0
            distanceTextView.text = String.format(Locale.getDefault(), "%.1fkm", distanceKm)
            val minutes = (p.duration % 3600) / 60
            timeTextView.text = String.format(Locale.getDefault(), "%02d분", minutes)
            styleChip.text = p.walkStyle ?: "미지정"
            pathChip.text = p.pathReview ?: "미지정"

            startWalkButton.setOnClickListener {
                val intent = Intent(this, WalkActivity::class.java).apply {
                    // 이미지 URL이 있다면 extra에 추가
                    if (!p.imageUrl.isNullOrEmpty()) {
                        putExtra(EXTRA_MINIMAP_IMAGE_URI, p.imageUrl)
                    }
                }
                startActivity(intent)
            }

        } ?: run {
            Toast.makeText(this, "게시물 정보를 불러올 수 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}