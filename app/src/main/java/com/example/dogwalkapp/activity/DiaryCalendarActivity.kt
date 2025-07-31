package com.example.dogwalkapp.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dogwalkapp.R
import com.example.dogwalkapp.adapter.CalendarAdapter
import com.example.dogwalkapp.base.NavigationActivity
import com.example.dogwalkapp.models.CourseItem
import com.example.dogwalkapp.models.DayItem
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class DiaryCalendarActivity : NavigationActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var monthlyCalendarRecyclerView: RecyclerView
    private lateinit var weeklyCalendarRecyclerView: RecyclerView
    private lateinit var calendarHeader: View

    private lateinit var monthlyCalendarAdapter: CalendarAdapter
    private lateinit var weeklyCalendarAdapter: CalendarAdapter

    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton

    private lateinit var btnCalendarView: ImageButton
    private lateinit var btnListView: ImageButton

    private lateinit var tvToday: ImageView
    private lateinit var tvCurrentMonth: TextView

    private lateinit var walkRecordView: View
    private lateinit var walkEmptyView: View

    private lateinit var tvDistance: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvMemoContent: TextView
    private lateinit var walkStyleChip: Chip
    private lateinit var walkRouteChip: Chip
    private lateinit var imageMapResult: ImageView

    private lateinit var walkDataMap: Map<LocalDate, CourseItem>
    private var currentCalendar: Calendar = Calendar.getInstance()
    private var isMonthlyView: Boolean = true
    private var currentSelectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_calendar)

        // XML 뷰 초기화
        monthlyCalendarRecyclerView = findViewById(R.id.monthlyCalendarRecyclerView)
        weeklyCalendarRecyclerView = findViewById(R.id.weeklyCalendarRecyclerView)
        calendarHeader = findViewById(R.id.layout_calendar_header)

        prevMonthButton = findViewById(R.id.btnPreviousMonth)
        nextMonthButton = findViewById(R.id.btnNextMonth)

        btnCalendarView = findViewById(R.id.btnCalendarView)
        btnListView = findViewById(R.id.btnListView)

        tvToday = findViewById(R.id.tvToday)
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)

        walkRecordView = findViewById(R.id.layout_walk_record)
        walkEmptyView = findViewById(R.id.layout_empty)

        tvDistance = findViewById(R.id.tvDistance)
        tvDuration = findViewById(R.id.tvDuration)
        tvCalories = findViewById(R.id.tvCalories)
        tvMemoContent = findViewById(R.id.tvMemoContent)
        walkStyleChip = findViewById(R.id.walkStyleChip)
        walkRouteChip = findViewById(R.id.walkRouteChip)
        imageMapResult = findViewById(R.id.imageMapResult)

        setupBottomNavigation(R.id.nav_record)

        walkDataMap = emptyMap()
        loadWalkDataFromFirebase()

        // 어댑터 설정
        monthlyCalendarAdapter = CalendarAdapter(emptyList(), onDateClicked = { selectedDate ->
            this.currentSelectedDate = selectedDate // Activity의 선택 날짜 업데이트
            showWalkViewForDate(selectedDate)
            monthlyCalendarAdapter.setSelectedDate(selectedDate) // 어댑터에 선택 날짜 전달
            weeklyCalendarAdapter.setSelectedDate(selectedDate) // 다른 어댑터에도 전달 (동기화)
        }, isWeeklyView = false)
        monthlyCalendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        monthlyCalendarRecyclerView.adapter = monthlyCalendarAdapter

        weeklyCalendarAdapter = CalendarAdapter(emptyList(), onDateClicked = { selectedDate ->
            this.currentSelectedDate = selectedDate // Activity의 선택 날짜 업데이트
            showWalkViewForDate(selectedDate)
            monthlyCalendarAdapter.setSelectedDate(selectedDate) // 어댑터에 선택 날짜 전달
            weeklyCalendarAdapter.setSelectedDate(selectedDate) // 다른 어댑터에도 전달
        }, isWeeklyView = true)
        weeklyCalendarRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        weeklyCalendarRecyclerView.adapter = weeklyCalendarAdapter

        // 초기 상태 설정
        currentSelectedDate = LocalDate.now() // 초기 선택 날짜를 오늘로 설정
        currentCalendar.set(currentSelectedDate.year, currentSelectedDate.monthValue - 1, currentSelectedDate.dayOfMonth)

        // 초기화 시 월간 달력 표시
        showMonthlyCalendar()

        // 오늘 날짜의 산책 기록 표시
        showWalkViewForDate(currentSelectedDate)

        // 초기 선택 날짜를 어댑터에 전달하여 UI 반영
        monthlyCalendarAdapter.setSelectedDate(currentSelectedDate)
        weeklyCalendarAdapter.setSelectedDate(currentSelectedDate)

        // 월간 달력 이전 버튼
        prevMonthButton.setOnClickListener {
            if (isMonthlyView) {
                currentCalendar.add(Calendar.MONTH, -1)
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            }
            updateCalendarView()
        }

        // 월간 달력 다음 버튼
        nextMonthButton.setOnClickListener {
            if (isMonthlyView) {
                currentCalendar.add(Calendar.MONTH, 1)
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            updateCalendarView()
        }


        tvToday.setOnClickListener {
            currentSelectedDate = LocalDate.now() // 오늘 날짜로 선택 날짜 변경
            currentCalendar.set(currentSelectedDate.year, currentSelectedDate.monthValue - 1, currentSelectedDate.dayOfMonth)
            showWalkViewForDate(currentSelectedDate)
            monthlyCalendarAdapter.setSelectedDate(currentSelectedDate)
            weeklyCalendarAdapter.setSelectedDate(currentSelectedDate)
            updateCalendarView()
        }

        btnCalendarView.setOnClickListener {
            if (!isMonthlyView) {
                showMonthlyCalendar()
            }
        }

        btnListView.setOnClickListener {
            if (isMonthlyView) {
                showWeeklyCalendar()
            }
        }
    }

    //파이어베이스에서 날짜별 기록 불러오기
    private fun loadWalkDataFromFirebase() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("courses")
            .whereEqualTo("createdBy", uid)
            .get()
            .addOnSuccessListener { documents ->
                val resultMap = mutableMapOf<LocalDate, CourseItem>()

                for (doc in documents) {
                    // date 필드를 Map 형태로 가져옴
                    val dateMap = doc.get("date") as? Map<String, Any>

                    val walkDate: LocalDate
                    if (dateMap != null) {
                        val year = (dateMap["year"] as? Long)?.toInt()
                        val month = (dateMap["monthValue"] as? Long)?.toInt()
                        val dayOfMonth = (dateMap["dayOfMonth"] as? Long)?.toInt()


                        // null이 아닐 시에 할당
                        if (year != null && month != null && dayOfMonth != null) {
                            walkDate = LocalDate.of(year, month, dayOfMonth)
                        } else {
                            Log.e("DiaryCalendarActivity", "${doc.id}: 년/월/일 정보가 완전하지 않습니다. 이 문서는 건너뜁니다.")
                            continue // 이 문서의 날짜 정보가 불완전하므로 다음 문서로 넘어감
                        }
                    } else {
                        Log.e("DiaryCalendarActivity", "${doc.id}: date 필드가 Map 형태로 존재하지 않습니다. 이 문서는 건너뜁니다.")
                        continue // dateMap이 null이므로 다음 문서로 넘어감
                    }

                    val item = CourseItem(
                        date = walkDate,
                        distance = doc.getDouble("distance") ?: 0.0,
                        duration = doc.getLong("duration") ?: 0L,
                        calories = (doc.getLong("calories") ?: 0L).toInt(),
                        imageUrl = doc.getString("imageUrl"),
                        petName = doc.getString("petName") ?: "",
                        walkStyle = doc.getString("walkStyle") ?: "",
                        pathReview = doc.getString("pathReview") ?: "",
                        memo = doc.getString("memo") ?: ""
                    )
                    resultMap[walkDate] = item
                }

                walkDataMap = resultMap
                updateCalendarView()
                showWalkViewForDate(currentSelectedDate)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun updateCalendarView() {
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val tempDate = LocalDate.of(currentYear, currentMonth, currentDay)

        //yyyy년 MM월로 포맷
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월")
        tvCurrentMonth.text = formatter.format(tempDate)

        if (isMonthlyView) {
            val dayItems = generateDaysForMonth(currentCalendar, walkDataMap)
            monthlyCalendarAdapter.updateData(dayItems)
            monthlyCalendarAdapter.setSelectedDate(currentSelectedDate) // 데이터 갱신 후 선택 날짜 다시 설정
        } else {
            val dayItems = generateDaysForWeek(tempDate, walkDataMap)
            weeklyCalendarAdapter.updateData(dayItems)
            weeklyCalendarAdapter.setSelectedDate(currentSelectedDate) // 데이터 갱신 후 선택 날짜 다시 설정

            // 주간 뷰 스크롤 위치 조정
            val selectedIndex = dayItems.indexOfFirst { it.date == currentSelectedDate }
            if (selectedIndex != -1) {
                (weeklyCalendarRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(selectedIndex, 0)
            }
        }
    }

    //기록이 있으면 walkRecordView를 보여주고 없다면 walkEmptyView를 보여줌
    private fun showWalkViewForDate(date: LocalDate) {
        val data = walkDataMap[date]
        if (data != null) {
            walkRecordView.visibility = View.VISIBLE
            walkEmptyView.visibility = View.GONE
            bindWalkRecord(data)
        } else {
            walkRecordView.visibility = View.GONE
            walkEmptyView.visibility = View.VISIBLE
        }
    }

    //월간 달력 버튼을 클릭하면 monthlyCalendarRecyclerView을 보여줌
    private fun showMonthlyCalendar() {
        isMonthlyView = true
        monthlyCalendarRecyclerView.visibility = View.VISIBLE
        weeklyCalendarRecyclerView.visibility = View.GONE
        calendarHeader.visibility = View.VISIBLE

        prevMonthButton.isEnabled = true
        nextMonthButton.isEnabled = true

        updateCalendarView()
    }

    //주간 달력 버튼을 클릭하면 weeklyCalendarRecyclerView 보여줌
    private fun showWeeklyCalendar() {
        isMonthlyView = false
        monthlyCalendarRecyclerView.visibility = View.GONE
        weeklyCalendarRecyclerView.visibility = View.VISIBLE
        calendarHeader.visibility = View.GONE

        prevMonthButton.isEnabled = false
        nextMonthButton.isEnabled = false

        updateCalendarView()
    }

    //거리, 시간, 칼로리를 단위에 맞게 포맷팅
    private fun bindWalkRecord(data: CourseItem) {
        val distanceKm = data.distance / 1000.0
        tvDistance.text = String.format("%05.2f km", distanceKm)
        tvDuration.text = formatDurationToHHMMSS(data.duration)
        tvCalories.text = "${data.calories} kcal"
        tvMemoContent.text = data.memo
        walkStyleChip.text = data.walkStyle
        walkRouteChip.text = data.pathReview

        if(!data.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(data.imageUrl)
                .into(imageMapResult)
            imageMapResult.visibility = View.VISIBLE
        }
    }


    private fun formatDurationToHHMMSS(duration: Long): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    //월간 달력과 주간 달력 생성하기
    private fun generateDaysForMonth(
        calendar: Calendar,
        walkMap: Map<LocalDate, CourseItem>
    ): List<DayItem> {
        val result = mutableListOf<DayItem>()
        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)

        val maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = (temp.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7

        for (i in 0 until firstDayOfWeek) {
            result.add(DayItem(null, null, false))
        }

        for (day in 1..maxDay) {
            val date = LocalDate.of(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH) + 1, day)
            val walked = walkMap.containsKey(date)
            result.add(DayItem(date, day, walked))
        }

        return result
    }

    private fun generateDaysForWeek(
        startDateForWeek: LocalDate,
        walkMap: Map<LocalDate, CourseItem>
    ): List<DayItem> {
        val result = mutableListOf<DayItem>()
        var current = startDateForWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        for (i in 0 until 7) {
            val walked = walkMap.containsKey(current)
            result.add(DayItem(current, current.dayOfMonth, walked))
            current = current.plusDays(1)
        }
        return result
    }
}