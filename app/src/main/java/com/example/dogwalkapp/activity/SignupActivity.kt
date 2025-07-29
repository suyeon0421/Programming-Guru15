package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dogwalkapp.R
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

//회원가입 화면
class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var editEmail: EditText
    private lateinit var editPw: EditText
    private lateinit var editPwCheck: EditText
    private lateinit var btnCheckEmail: Button
    private lateinit var btnNext: Button
    private lateinit var completeEmail: TextView

    private var isEmailChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        editEmail = findViewById(R.id.edit_email)
        editPw = findViewById(R.id.edit_pw)
        editPwCheck = findViewById(R.id.edit_pw_check)
        btnCheckEmail = findViewById(R.id.btn_check_email)
        btnNext = findViewById(R.id.btn_next)

        completeEmail = findViewById(R.id.complete_email) // XML에서 TextView 연결
        completeEmail.visibility = View.GONE // 처음엔 안 보이게

        btnCheckEmail.setOnClickListener {
            val email = editEmail.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                isEmailChecked = false
                return@setOnClickListener
            }
            // Firebase 중복 체크: 등록 회원 여부 확인
            auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result?.signInMethods
                        if (result.isNullOrEmpty()) {
                            Toast.makeText(this, "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show()
                            isEmailChecked = true
                        } else {
                            Toast.makeText(this, "이미 가입된 이메일입니다.", Toast.LENGTH_SHORT).show()
                            isEmailChecked = false
                        }
                    } else {
                        Toast.makeText(this, "이메일 중복 확인 실패", Toast.LENGTH_SHORT).show()
                        isEmailChecked = false
                    }
                }
        }
        btnNext.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val pw = editPw.text.toString().trim()
            val pwCheck = editPwCheck.text.toString().trim()
            if (!isEmailChecked) {
                Toast.makeText(this, "이메일 중복 확인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pw != pwCheck) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 실제 회원가입
            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    // 반려견 정보 입력 화면으로 이동
                    startActivity(Intent(this, PetInfoActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}
