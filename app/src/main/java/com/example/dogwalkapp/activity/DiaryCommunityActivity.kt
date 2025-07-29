package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dogwalkapp.R
import com.example.dogwalkapp.adapter.DiaryCommunitiyAdapter
import com.example.dogwalkapp.base.NavigationActivity
import com.example.dogwalkapp.models.CourseItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.LocalDate

class DiaryCommunityActivity : NavigationActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DiaryCommunitiyAdapter
    private lateinit var recommendTitle: TextView


    private var allPosts: List<CourseItem> = emptyList()
    private var currentPosts: List<CourseItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_community)

        setupBottomNavigation(R.id.nav_nearby)

        recyclerView = findViewById(R.id.recycler_walk_list)
        recommendTitle = findViewById(R.id.recommendTitle)

        adapter = DiaryCommunitiyAdapter(
            posts = currentPosts,
            onPostClick = { post ->
                showDetail(post) // 프래그먼트 대신 액티비티 호출
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadPostsFromFirebase()
    }

    private fun loadPostsFromFirebase() {
        db.collection("courses")
            .orderBy("timestamp", Query.Direction.DESCENDING) //최신순 정렬
            .get()
            .addOnSuccessListener { documents ->
                val postList = mutableListOf<CourseItem>()

                for (doc in documents) {
                    val dateString = doc.getString("date") ?: continue
                    val date = LocalDate.parse(dateString)


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
                currentPosts = allPosts

                adapter.updatePosts(currentPosts)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
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