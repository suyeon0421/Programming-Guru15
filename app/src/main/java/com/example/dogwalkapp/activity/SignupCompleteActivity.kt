package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R

class SignupCompleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_complete)

        val btnDone = findViewById<Button>(R.id.btn_done)
        btnDone.setOnClickListener {
            // 완료 버튼 클릭 시 MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)

            // 이전 Activity 스택 모두 클리어 (뒤로가기 방지)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
            finish()
        }
    }
}
