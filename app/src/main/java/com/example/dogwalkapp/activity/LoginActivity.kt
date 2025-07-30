package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.google.firebase.functions.FirebaseFunctions // Firebase Functions SDK import
import com.google.firebase.functions.ktx.functions // KTX 확장 함수용
import com.google.firebase.ktx.Firebase // Firebase Root 객체용

// 로그인 화면
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var idEdit: EditText
    private lateinit var pwEdit: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnFindPw: Button
    private lateinit var btnSignup: Button

    private lateinit var btnKakaoLogin: Button

    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        idEdit = findViewById(R.id.user_email)
        pwEdit = findViewById(R.id.user_pwd)
        btnLogin = findViewById(R.id.btn_login)
        btnFindPw = findViewById(R.id.tv_find_pw)
        btnSignup = findViewById(R.id.tv_signup)
        btnKakaoLogin = findViewById(R.id.btn_kakao_login)

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

        // 4. 카카오 로그인
        btnKakaoLogin.setOnClickListener {
            kakaoLogin()
        }
    }

    // 카카오 로그인 전체 흐름 처리
    private fun kakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(this, "카카오 로그인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("KAKAO_LOGIN", "로그인 실패", error)
            } else if (token != null) {
                Toast.makeText(this, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show()
                Log.i("KAKAO_LOGIN", "토큰: ${token.accessToken}")
                goToMain()
            }
        }

        // 무조건 웹 브라우저 로그인만 시도 (에뮬레이터용)
        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
    }


    private fun goToMain() {
        startActivity(Intent(this, PetInfoActivity::class.java))
        finish()
    }
}