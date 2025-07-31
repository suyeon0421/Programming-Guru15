package com.example.dogwalkapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionScene.Transition.TransitionOnClick
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dogwalkapp.R
import com.example.dogwalkapp.models.CourseItem
import com.google.android.material.chip.Chip
import java.time.LocalDate
import kotlin.time.Duration

class DiaryCommunitiyAdapter (
    private var posts: List<CourseItem>,
    private val onPostClick: (CourseItem) -> Unit
): RecyclerView.Adapter<DiaryCommunitiyAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLocation: TextView = itemView.findViewById(R.id.text_location)
        val tvAuthor: TextView = itemView.findViewById(R.id.text_user)
        val tvDistance: TextView = itemView.findViewById(R.id.text_distance)
        val tvDuration: TextView = itemView.findViewById(R.id.text_time)
        val tvChipStyle: Chip = itemView.findViewById(R.id.chip_tag1)
        val tvChipPath: Chip =itemView.findViewById(R.id.chip_tag2)
        val ivPostImage: ImageView = itemView.findViewById(R.id.image_map_thumbnail)

        init {
            itemView.setOnClickListener {
                if(adapterPosition != RecyclerView.NO_POSITION) {
                    onPostClick(posts[adapterPosition])
                }
            }
        }

        fun bind(post:CourseItem) {
            tvAuthor.text = post.petName

            //이미지 URL을 불러옴
            if(post.imageUrl != null && post.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(post.imageUrl)
                    .into(ivPostImage)
                ivPostImage.visibility = View.VISIBLE
                //없다면 보여주지 않음
            } else {
                ivPostImage.visibility = View.GONE
            }

            val distanceKm = post.distance / 1000.0

            tvDistance.text = String.format("%.1fkm", distanceKm)
            tvDuration.text = formatDurationToHHMM(post.duration)

            tvChipStyle.text = post.walkStyle
            tvChipPath.text = post.pathReview

        }

        private fun formatDurationToHHMM(duration: Long) : String {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60

            return if (hours > 0) {
                String.format("%02d시간 %02d분", hours, minutes)
            } else {
                String.format("%02d분", minutes)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<CourseItem>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}