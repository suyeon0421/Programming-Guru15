package com.example.dogwalkapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.R
import com.example.dogwalkapp.models.DayItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class WeekCalendarAdapter(
    private var days: List<DayItem>,
    private val onDateClicked: (LocalDate) -> Unit
) : RecyclerView.Adapter<WeekCalendarAdapter.DayViewHolder>() {

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayOfWeekText: TextView = itemView.findViewById(R.id.dayOfWeekText)
        private val dayNumberText: TextView = itemView.findViewById(R.id.dayNumberText)
        private val walkedDot: View = itemView.findViewById(R.id.walkedDot)

        fun bind(item: DayItem) {
            dayOfWeekText.text = item.date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.KOREAN) ?: ""
            dayNumberText.text = item.dayNumber?.toString() ?: ""
            walkedDot.visibility = if (item.walked) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                item.date?.let { onDateClicked(it) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateData(newDays: List<DayItem>) {
        days = newDays
        notifyDataSetChanged()
    }
}
