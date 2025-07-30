package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.R
import com.example.dogwalkapp.adapter.DiaryCommunitiyAdapter
import com.example.dogwalkapp.base.NavigationActivity
import com.example.dogwalkapp.models.CourseItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.w3c.dom.Text
import java.time.LocalDate
import java.util.logging.Handler
import kotlin.math.roundToInt
import com.google.firebase.Timestamp // Timestamp import 추가
import java.time.Instant
import java.time.ZoneId

class DiaryCommunityActivity : NavigationActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DiaryCommunitiyAdapter
    private lateinit var recommendTitle: TextView
    private lateinit var btnFilter: ImageButton

    private lateinit var overlayContainer: FrameLayout
    private lateinit var dialogHandler: ImageView
    private lateinit var filterTitle: TextView

    private lateinit var tvFilterDistance: TextView
    private lateinit var distanceOptions: ChipGroup
    private lateinit var filterChip500under: Chip
    private lateinit var filterchip1000: Chip
    private lateinit var filterchip1500: Chip
    private lateinit var filterchip2000up: Chip

    private lateinit var tvFilterDuration: TextView
    private lateinit var durationOptions: ChipGroup
    private lateinit var filterChip20min: Chip
    private lateinit var filterChip40min: Chip
    private lateinit var filterChip60min: Chip
    private lateinit var filterChip1hOver: Chip

    private lateinit var tvFilterWalkStyle: TextView
    private lateinit var walkStyleOptions: ChipGroup
    private lateinit var filterChipEasy: Chip
    private lateinit var filterChipActive: Chip
    private lateinit var filterChipTough: Chip
    private lateinit var filterChipExplore: Chip

    private lateinit var tvFilterPath: TextView
    private lateinit var pathOptions: ChipGroup
    private lateinit var filterChipGrass: Chip
    private lateinit var filterChipSand: Chip
    private lateinit var filterChipSoil: Chip
    private lateinit var filterChipUphill: Chip

    // 하단 버튼들
    private lateinit var btnFilterReset: Button
    private lateinit var btnFilterApply: Button

    //필터 상태 변수
    private var currentDistance: String = ""
    private var currentDuration: String = ""
    private var currentWalkStyle: String = ""
    private var currentPath: String = ""

    // --- 데이터 ---
    private var allPosts: List<CourseItem> = emptyList() // DB에서 가져온 모든 원본 데이터

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_community)

        setupBottomNavigation(R.id.nav_nearby)

        recyclerView = findViewById(R.id.recycler_walk_list)
        recommendTitle = findViewById(R.id.recommendTitle)
        btnFilter = findViewById(R.id.btn_filter)

        adapter = DiaryCommunitiyAdapter(
            posts = emptyList(),
            onPostClick = { post ->
                showDetail(post) // 프래그먼트 대신 액티비티 호출
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        overlayContainer = findViewById(R.id.filter_overlay_container)
        dialogHandler = overlayContainer.findViewById(R.id.dialog_handle)
        filterTitle = overlayContainer.findViewById(R.id.filter_title)

        tvFilterDistance = overlayContainer.findViewById(R.id.tv_filter_distance)
        distanceOptions = overlayContainer.findViewById(R.id.chip_group_filter_distance_options)
        filterChip500under = overlayContainer.findViewById(R.id.filter_chip_distance_500m_under)
        filterchip1000 = overlayContainer.findViewById(R.id.filter_chip_distance_500m_1km)
        filterchip1500 = overlayContainer.findViewById(R.id.filter_chip_distance_1km_2km)
        filterchip2000up = overlayContainer.findViewById(R.id.filter_chip_distance_2km_over)

        tvFilterDuration = overlayContainer.findViewById(R.id.tv_filter_duration)
        durationOptions = overlayContainer.findViewById(R.id.chip_group_filter_duration_options)
        filterChip20min = overlayContainer.findViewById(R.id.filter_chip_duration_20min_under)
        filterChip40min = overlayContainer.findViewById(R.id.filter_chip_duration_20min_40min)
        filterChip60min = overlayContainer.findViewById(R.id.filter_chip_duration_40min_1hour)
        filterChip1hOver = overlayContainer.findViewById(R.id.filter_chip_duration_1hour_over)

        tvFilterWalkStyle = overlayContainer.findViewById(R.id.tv_filter_style_title)
        walkStyleOptions = overlayContainer.findViewById(R.id.chip_group_filter_walk_style_options)
        filterChipEasy = overlayContainer.findViewById(R.id.filter_chip_style_easy)
        filterChipActive = overlayContainer.findViewById(R.id.filter_chip_style_active)
        filterChipTough = overlayContainer.findViewById(R.id.filter_chip_style_tough)
        filterChipExplore = overlayContainer.findViewById(R.id.filter_chip_style_explore)

        tvFilterPath = overlayContainer.findViewById(R.id.tv_filter_path_feature)
        pathOptions = overlayContainer.findViewById(R.id.chip_group_filter_path_feature_options)
        filterChipGrass = overlayContainer.findViewById(R.id.filter_chip_path_grass)
        filterChipSand = overlayContainer.findViewById(R.id.filter_chip_path_sand)
        filterChipSoil = overlayContainer.findViewById(R.id.filter_chip_path_soil)
        filterChipUphill = overlayContainer.findViewById(R.id.filter_chip_path_uphill)

        btnFilterReset = overlayContainer.findViewById(R.id.btn_filter_reset)
        btnFilterApply = overlayContainer.findViewById(R.id.btn_filter_apply)

        // --- 리스너 설정 ---
        btnFilter.setOnClickListener {
            showFilterOverlay()
        }

        btnFilterReset.setOnClickListener {
            resetFilters()
        }

        btnFilterApply.setOnClickListener {
            applyFilters()
        }

        //백 버튼 눌렀을 때 오버레이 닫기
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (overlayContainer.visibility == View.VISIBLE) {
                    hideFilterOverlay()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        loadPostsFromFirebase()
    }

    private fun loadPostsFromFirebase() {
        db.collection("courses")
            .orderBy("timestamp", Query.Direction.DESCENDING) //최신순 정렬
            .get()
            .addOnSuccessListener { documents ->
                val postList = mutableListOf<CourseItem>()

                for (doc in documents) {
                    val firebaseTimestamp: Timestamp? = doc.getTimestamp("timestamp")
                    val date: LocalDate? = firebaseTimestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

                    if (date == null) {
                        Log.w("Firebase", "문서 ${doc.id}에 timestamp 필드가 없거나 유효하지 않아 스킵합니다.")
                        continue
                    }

                    val post = CourseItem(
                        date = date,
                        distance = doc.getDouble("distance") ?: 0.0,
                        duration = doc.getLong("duration") ?: 0L,
                        calories = (doc.getLong("calories") ?: 0L).toInt(),
                        imageUrl = doc.getString("imageUrl"),
                        petName = doc.getString("petName") ?: "",
                        walkStyle = doc.getString("walkStyle") ?: "",
                        pathReview = doc.getString("pathReview") ?: "",
                        memo = doc.getString("memo") ?: "",
                    )

                    postList.add(post)
                }

                allPosts = postList.sortedByDescending { it.date }
                applyFilters()
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "데이터 로드 실패: ${exception.message}", exception)
                exception.printStackTrace()
            }
    }

    private fun showFilterOverlay() {
        // 오버레이를 표시하기 전에 현재 적용된 필터 값으로 칩들을 미리 선택해 둡니다.
        setChipGroupSelection(distanceOptions, currentDistance)
        setChipGroupSelection(durationOptions, currentDuration)
        setChipGroupSelection(walkStyleOptions, currentWalkStyle)
        setChipGroupSelection(pathOptions, currentPath)

        overlayContainer.visibility = View.VISIBLE
    }

    // --- 필터 오버레이 숨기기 ---
    private fun hideFilterOverlay() {
        overlayContainer.visibility = View.GONE
    }

    // --- 필터 초기화 ---
    private fun resetFilters() {
        // 모든 필터 상태 변수 초기화
        currentDistance = ""
        currentDuration = ""
        currentWalkStyle = ""
        currentPath = ""

        // 오버레이 칩 그룹들의 선택 해제
        distanceOptions.clearCheck()
        durationOptions.clearCheck()
        walkStyleOptions.clearCheck()
        pathOptions.clearCheck()

        applyFilters()
    }

    // --- 필터 적용 ---
    private fun applyFilters() {
        // 각 칩 그룹에서 선택된 현재 값 가져오기
        currentDistance = getSelectedChipText(distanceOptions)
        currentDuration = getSelectedChipText(durationOptions)
        currentWalkStyle = getSelectedChipText(walkStyleOptions)
        currentPath = getSelectedChipText(pathOptions)

        Log.d("Filter", "적용된 필터: 거리($currentDistance), 시간($currentDuration), 스타일($currentWalkStyle), 경로($currentPath)")

        // 모든 원본 게시물을 기반으로 필터링 수행
        val filteredList = allPosts.filter { post ->
            // 거리 필터 (미터 단위로 비교)
            val isDistanceMatch = when (currentDistance) {
                "500m 이하" -> post.distance <= 500.0 // ✅ 0.5km -> 500.0m
                "500m ~ 1km" -> post.distance > 500.0 && post.distance <= 1000.0 // ✅ 0.5km -> 500.0m, 1km -> 1000.0m
                "1km ~ 2km" -> post.distance > 1000.0 && post.distance <= 2000.0 // ✅ 1km -> 1000.0m, 2km -> 2000.0m
                "2km 이상" -> post.distance > 2000.0 // ✅ 2km -> 2000.0m
                else -> true // 필터 선택 안 됨 (모두 포함)
            }

            // 소요 시간 필터
            val isDurationMatch = when (currentDuration) {
                "20분 이하" -> (post.duration / 60.0).roundToInt() <= 20 // 초를 분으로 변환
                "20분 ~ 40분" -> (post.duration / 60.0).roundToInt() > 20 && (post.duration / 60.0).roundToInt() <= 40
                "40분 ~ 1시간" -> (post.duration / 60.0).roundToInt() > 40 && (post.duration / 60.0).roundToInt() <= 60
                "1시간 이상" -> (post.duration / 60.0).roundToInt() > 60
                else -> true // 필터 선택 안 됨 (모두 포함)
            }

            // 산책 스타일 필터
            val isWalkStyleMatch = if (currentWalkStyle.isNotEmpty()) {
                post.walkStyle == currentWalkStyle
            } else true // 필터 선택 안 됨 (모두 포함)

            // 경로 특징 필터
            val isPathFeatureMatch = if (currentPath.isNotEmpty()) {
                post.pathReview == currentPath
            } else true // 필터 선택 안 됨 (모두 포함)

            isDistanceMatch && isDurationMatch && isWalkStyleMatch && isPathFeatureMatch
        }

        // 필터링된 데이터로 RecyclerView 업데이트
        adapter.updatePosts(filteredList)

        // 오버레이 숨기기
        hideFilterOverlay()
    }

    // 기존 ChipGroup에서 선택된 Chip의 텍스트를 반환하는 헬퍼 함수
    private fun getSelectedChipText(chipGroup: ChipGroup): String {
        val checkedChipId = chipGroup.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            val chip = chipGroup.findViewById<Chip>(checkedChipId)
            chip?.text.toString()
        } else ""
    }

    // ChipGroup 내에서 텍스트와 같은 Chip을 찾아 선택 상태로 만드는 헬퍼 함수
    private fun setChipGroupSelection(chipGroup: ChipGroup, text: String) {
        chipGroup.clearCheck() // 기존 선택 초기화
        if (text.isEmpty()) return // 텍스트가 없으면 아무것도 선택하지 않음

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.text == text) {
                chip.isChecked = true
                break
            }
        }
    }

    // DiaryDetailActivity를 시작하는 함수
    fun showDetail(post: CourseItem) {
        val intent = Intent(this, DiaryDetailActivity::class.java).apply {
            putExtra(DiaryDetailActivity.EXTRA_COURSE_ITEM, post)
        }
        startActivity(intent)
    }
}