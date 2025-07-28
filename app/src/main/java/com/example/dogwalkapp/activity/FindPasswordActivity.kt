package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R
import com.google.firebase.auth.FirebaseAuth

class FindPasswordActivity : AppCompatActivity() {

    private lateinit var editEmail: EditText
    private lateinit var btnSendLink: Button
    private lateinit var btnLogin: Button

    private val auth = FirebaseAuth.getInstance()
    private var countDownTimer: CountDownTimer? = null
    private val cooldownMillis = 60000L // 1분 (60초)
    private var isFirstSend = true      // 최초 발송 여부 플래그

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_password)

        editEmail = findViewById(R.id.edit_email)
        btnSendLink = findViewById(R.id.btn_send_link)
        btnLogin = findViewById(R.id.btn_login)

        btnSendLink.setOnClickListener {
            val email = editEmail.text.toString().trim()

            // 이메일 빈값 확인
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 이메일 포맷 확인
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "유효한 이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비밀번호 재설정 이메일 전송 요청
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "비밀번호 재설정 메일을 전송했습니다.\n이메일을 확인해 주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                        // 버튼 텍스트를 '링크 재발송' 으로 변경
                        btnSendLink.text = "링크 재발송"

                        if (isFirstSend) {
                            // 최초 발송 시는 쿨타임 없이 바로 버튼 활성화 유지
                            isFirstSend = false
                            btnSendLink.isEnabled = true
                        } else {
                            // 두 번째 이후부터 쿨타임 시작
                            startCooldown()
                        }

                    } else {
                        Toast.makeText(
                            this,
                            "메일 전송 실패: 등록된 이메일인지 확인해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        btnLogin.setOnClickListener {
            // 로그인 화면으로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // 1분 쿨타임 동안 버튼 비활성화 및 남은 시간 표시
    private fun startCooldown() {
        btnSendLink.isEnabled = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(cooldownMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                btnSendLink.text = "${secondsRemaining}s"
            }

            override fun onFinish() {
                btnSendLink.isEnabled = true
                btnSendLink.text = "링크 재발송"
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
