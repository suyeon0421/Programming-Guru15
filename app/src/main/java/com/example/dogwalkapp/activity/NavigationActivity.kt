package com.example.dogwalkapp.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R
import com.example.dogwalkapp.activity.* // 필요한 모든 액티비티 import
import com.google.android.material.bottomnavigation.BottomNavigationView

open class NavigationActivity : AppCompatActivity() {

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            val targetClass: Class<*> = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java // MainActivity가 아니라 HomeActivity가 메인이라면
                R.id.nav_record -> DiaryCalendarActivity::class.java
                R.id.nav_nearby -> DiaryCommunityActivity::class.java
                R.id.nav_profile -> MyPageActivity::class.java
                else -> return@setOnItemSelectedListener false // 처리할 수 없는 아이템인 경우
            }


            if (selectedItemId != item.itemId) {
                val intent = Intent(this, targetClass).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            true // 이벤트 소비
        }
    }
}