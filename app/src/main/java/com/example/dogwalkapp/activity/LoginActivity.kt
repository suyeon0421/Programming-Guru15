package com.example.dogwalkapp.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dogwalkapp.R
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

// 로그인 화면
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var idEdit: EditText
    private lateinit var pwEdit: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnFindPw: Button
    private lateinit var btnSignup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        idEdit = findViewById(R.id.user_email)
        pwEdit = findViewById(R.id.user_pwd)
        btnLogin = findViewById(R.id.btn_login)
        btnFindPw = findViewById(R.id.tv_find_pw)
        btnSignup = findViewById(R.id.tv_signup)

        // 1. 로그인 버튼
        btnLogin.setOnClickListener {
            val id = idEdit.text.toString().trim()
            val pw = pwEdit.text.toString().trim()

            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

                // Firebase 이메일/비밀번호 로그인 시도
                auth.signInWithEmailAndPassword(id, pw)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 로그인 성공 - 메인 화면으로 이동
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // 로그인 실패 - 에러 메시지 출력
                            Toast.makeText(
                                this,
                                "로그인 실패: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }

        // 2. 비밀번호 찾기 버튼
            btnFindPw.setOnClickListener {
                startActivity(Intent(this, FindPasswordActivity::class.java))
            }
        // 3. 회원가입 버튼
            btnSignup.setOnClickListener {
                startActivity(Intent(this, SignupActivity::class.java))
            }
        }
    }