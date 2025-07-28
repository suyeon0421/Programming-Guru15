package com.example.dogwalkapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class WeekAdapter(
    private val days: List<LocalDate>,
    private val walkedDates: List<LocalDate>,
    private val onClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<WeekAdapter.DayViewHolder>() {
    private var selectedPosition = -1

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayOfWeekText: TextView = itemView.findViewById(R.id.dayOfWeekText)
        val dayNumberText: TextView = itemView.findViewById(R.id.dayNumberText)

    }

    private var itemWidth: Int = 0

    fun setItemWidth(width: Int) {
        itemWidth = width
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_week_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        // 아이템 너비 지정
        if (itemWidth > 0) {
            val params = holder.itemView.layoutParams
            params.width = itemWidth
            holder.itemView.layoutParams = params
        }

        holder.dayOfWeekText.text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        holder.dayNumberText.text = day.dayOfMonth.toString()

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            val newPos = holder.adapterPosition
            if (newPos == RecyclerView.NO_POSITION) return@setOnClickListener

            selectedPosition = newPos
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)

            onClick(days[newPos])
        }
    }

    override fun getItemCount() = days.size
}
