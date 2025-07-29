package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
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
        functions = Firebase.functions

        functions.useEmulator("10.0.2.2", 5001) // Android 에뮬레이터에서 로컬 Functions 에뮬레이터 사용

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
        // 카카오 로그인 콜백 함수 정의
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(this, "카카오 로그인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                Toast.makeText(this, "카카오 로그인 성공! Firebase 연동 시도...", Toast.LENGTH_SHORT).show()
                // 획득한 카카오 AccessToken을 Firebase Cloud Function으로 보내 Custom Token 요청
                exchangeKakaoTokenForFirebaseToken(token.accessToken)
            }
        }

        // 카카오톡 설치 여부에 따라 로그인 방식 결정
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            // 카카오톡이 설치되어 있으면 카카오톡 앱으로 로그인
            //this와 callback 함수만 전달
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            // 카카오톡이 설치되어 있지 않으면 카카오계정(웹 브라우저)으로 로그인
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    // Firebase Cloud Functions를 호출하여 Custom Token 받기
    private fun exchangeKakaoTokenForFirebaseToken(kakaoAccessToken: String) {
        // Cloud Function에 전달할 데이터 (카카오 Access Token)
        val data = hashMapOf(
            "accessToken" to kakaoAccessToken
        )

        // 배포된 Cloud Function 'verifyKakaoTokenAndCreateFirebaseToken' 호출
        functions
            .getHttpsCallable("verifyKakaoTokenAndCreateFirebaseToken") // Cloud Functions에서 정의한 함수 이름
            .call(data) // 데이터와 함께 함수 호출
            .addOnSuccessListener { result ->
                // Cloud Function 호출 성공 및 결과 받기
                val firebaseToken = (result.data as? Map<String, Any>)?.get("firebaseToken") as? String
                if (firebaseToken != null) {
                    // Firebase Custom Token을 받았다면, 이 토큰으로 Firebase에 로그인
                    signInFirebaseWithCustomToken(firebaseToken)
                } else {
                    // Custom Token이 제대로 반환되지 않았을 경우
                    Toast.makeText(this, "Firebase Custom Token 획득 실패", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Firebase Cloud Functions 호출 실패: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace() // 디버깅을 위해 스택 트레이스 출력
            }
    }

    // Firebase Custom Token으로 Firebase에 로그인
    private fun signInFirebaseWithCustomToken(firebaseToken: String) {
        auth.signInWithCustomToken(firebaseToken)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase 로그인 성공
                    goToMain()
                } else {
                    // Firebase 로그인 실패
                    Toast.makeText(this, "Firebase Custom Token 로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 메인 화면으로 이동
    private fun goToMain() {
        Toast.makeText(this, "카카오 계정으로 로그인 성공!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}