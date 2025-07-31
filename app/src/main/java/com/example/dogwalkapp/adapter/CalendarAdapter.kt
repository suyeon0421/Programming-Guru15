package com.example.dogwalkapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.R
import com.example.dogwalkapp.models.DayItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class CalendarAdapter(
    private var days: List<DayItem>,
    private val onDateClicked: (LocalDate) -> Unit, // 날짜 클릭 시 콜백
    private val isWeeklyView: Boolean = false // 이 어댑터가 주간 뷰에 사용되는지 여부
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    // 현재 선택된 날짜 (없으면 null)
    private var selectedDate: LocalDate? = null

    // RecyclerView에 바인딩될 때 선택된 날짜를 초기화하고 업데이트
    fun setSelectedDate(date: LocalDate?) {
        val oldSelectedDate = selectedDate
        selectedDate = date

        // 불필요한 전체 갱신 대신 필요한 아이템만 갱신하여 성능 최적화
        if (oldSelectedDate != null) {
            val oldPos = days.indexOfFirst { it.date == oldSelectedDate }
            if (oldPos != -1) {
                notifyItemChanged(oldPos)
            }
        }
        if (selectedDate != null) {
            val newPos = days.indexOfFirst { it.date == selectedDate }
            if (newPos != -1) {
                notifyItemChanged(newPos)
            }
        }
    }

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayOfWeekText: TextView?
        val dayNumberText: TextView?
        val tvDay: TextView?
        val dotEvent: View?
        val walkedDot: View?

        init {
            // isWeeklyView 값에 따라 findViewById를 분기 처리
            if (isWeeklyView) {
                // 주간 뷰 (item_day_cell.xml)
                dayOfWeekText = itemView.findViewById(R.id.dayOfWeekText)
                dayNumberText = itemView.findViewById(R.id.dayNumberText)
                walkedDot = itemView.findViewById(R.id.walkedDot)
                tvDay = null
                dotEvent = null
            } else {
                // 월간 뷰 (item_calendar_day.xml 또는 기존 월간 셀 레이아웃)
                tvDay = itemView.findViewById(R.id.tvDay)
                dotEvent = itemView.findViewById(R.id.dotEvent)
                dayOfWeekText = null
                dayNumberText = null
                walkedDot = null
            }

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedItem = days[position]
                    clickedItem.date?.let { date ->
                        onDateClicked(date)
                        setSelectedDate(date)
                    }
                }
            }
        }

        fun bind(item: DayItem, currentSelectedDate: LocalDate?) {
            // 날짜가 유효하지 않은 (빈 칸) 아이템 처리
            if (item.date == null || item.dayNumber == null) {
                dayOfWeekText?.text = ""
                dayNumberText?.text = ""
                tvDay?.text = ""
                itemView.background = null
                dayOfWeekText?.setTextColor(Color.TRANSPARENT)
                dayNumberText?.setTextColor(Color.TRANSPARENT)
                tvDay?.setTextColor(Color.TRANSPARENT)
                walkedDot?.visibility = View.GONE
                dotEvent?.visibility = View.GONE
                itemView.isClickable = false
            } else {
                itemView.isClickable = true

                // 기본 텍스트 색상 설정 (주중/주말) 및 요일 텍스트 표시
                val defaultDayTextColor = Color.parseColor("#ABABAB") // 기본 검은색
                val defaultDayOfWeekColor = Color.parseColor("#ABABAB") // 기본 회색


                // 각 뷰 타입에 따라 텍스트 및 기본 배경 적용
                if (isWeeklyView) {
                    dayNumberText?.text = item.dayNumber.toString()
                    dayOfWeekText?.text = item.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREA)
                    dayNumberText?.setTextColor(defaultDayTextColor)
                    dayOfWeekText?.setTextColor(defaultDayOfWeekColor)
                    itemView.background = null // 주간 뷰는 기본 배경 없음
                } else {
                    tvDay?.text = item.dayNumber.toString()
                    tvDay?.setBackgroundResource(R.drawable.calendar_day_background) // 월간 뷰는 기본 배경 있음

                }

                // 선택된 날짜 강조 (오늘 날짜 강조 위에 덮어씌움)
                if (item.date == currentSelectedDate) {
                    if (isWeeklyView) {
                        // 주간 뷰: 글자 색상만 변경, 배경은 변경하지 않음
                        dayOfWeekText?.setTextColor(Color.parseColor("#6CA1D9"))
                        dayNumberText?.setTextColor(Color.parseColor("#6CA1D9"))
                    } else {
                        tvDay?.setBackgroundResource(R.drawable.calendar_today_background) // 월간 뷰 배경 변경
                        tvDay?.setTextColor(Color.parseColor("#6CA1D9"))
                    }
                }


                // 산책 기록 여부에 따른 점 표시
                if (isWeeklyView) {
                    walkedDot?.visibility = if (item.walked) View.VISIBLE else View.GONE
                } else {
                    dotEvent?.visibility = if (item.walked) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = if (isWeeklyView) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_week_day, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_day_cell, parent, false) // <-- 여기에 정확한 월간 레이아웃 파일명
        }
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position], selectedDate) // 선택된 날짜를 bind 함수에 전달
    }

    override fun getItemCount() = days.size

    fun updateData(newDays: List<DayItem>) {
        days = newDays
        // 데이터가 변경되면 선택된 날짜가 새 데이터 목록에 있는지 확인하고 필요한 경우 다시 선택
        if (selectedDate != null && !days.any { it.date == selectedDate }) {
            selectedDate = null // 이전에 선택된 날짜가 새 데이터에 없으면 선택 해제
        }
        notifyDataSetChanged()
    }
}