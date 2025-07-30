package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.dogwalkapp.base.NavigationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPageActivity : NavigationActivity() {

    // 유저 정보 예시 (실제 앱에서는 로그인된 사용자 정보로 교체)
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvOwnerName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var layoutPasswordSetting: ConstraintLayout
    private lateinit var layoutDogInfo: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        setupBottomNavigation(R.id.nav_profile)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI 요소 바인딩
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvEmail = findViewById(R.id.tvEmail)
        layoutPasswordSetting = findViewById(R.id.layoutPasswordSetting)
        layoutDogInfo = findViewById(R.id.layoutDogInfo)

        loadUserProfile()


        //비밀번호 재설정
       layoutPasswordSetting.setOnClickListener {
           startActivity(Intent(this, FindPasswordActivity::class.java))
       }

        //반려견 정보 설정
       layoutDogInfo.setOnClickListener {
           startActivity(Intent(this, PetInfoActivity::class.java))
       }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if(currentUser != null) {
            tvEmail.text = currentUser.email ?: "이메일 정보 없음"

            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if(document != null && document.exists()) {
                        val petMap = document.get("pet") as? Map<String, Any>
                        val dogName = petMap?.get("name") as? String

                        if (!dogName. isNullOrEmpty()) {
                            tvOwnerName.text = "${dogName} 보호자님"
                        } else {
                            tvOwnerName.text = "사용자 정보 없음"
                        }
                    } else {
                        tvOwnerName.text = "사용자 정보 없음"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MyPageActivity", "Firestore에서 사용자 정보 로드 오류: ${e.message}", e)
                    tvOwnerName.text = "사용자 정보 없음"
                }
        }
    }
}
