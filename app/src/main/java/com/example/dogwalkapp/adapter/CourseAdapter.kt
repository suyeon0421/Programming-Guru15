package com.example.dogwalkapp.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.models.CourseItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.dogwalkapp.R
import kotlin.collections.mutableListOf

class CourseAdapter(
    private val onItemClick: (CourseItem) -> Unit
): RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private val items: MutableList<CourseItem> = mutableListOf()

            fun setItems(newItems: List<CourseItem>) {
                items.clear()
                items.addAll(newItems)
                notifyDataSetChanged()
            }

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText = itemView.findViewById<TextView>(R.id.courseTitle)
        private val distanceText = itemView.findViewById<TextView>(R.id.courseDistance)
        private val durationText = itemView.findViewById<TextView>(R.id.courseDuration)
        private val walkButton = itemView.findViewById<TextView>(R.id.courseWalkButton)

        fun bind(item: CourseItem) {
            titleText.text = item.title
            distanceText.text = "${item.distance}km"
            durationText.text = "${item.duration / 60}분"

            itemView.setOnClickListener { onItemClick(item) }
            walkButton.setOnClickListener { onItemClick(item) }

        }
    }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
    }
