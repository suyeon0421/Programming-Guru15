package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(mainLooper).postDelayed({
            // 로그인 화면으로 이동
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1500) // 1.5초 후 이동
    }
}