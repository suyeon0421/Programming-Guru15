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

        // 이메일 설정
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvEmail.text = currentUser.email ?: "이메일 정보 없음"

            // Firestore에서 이름 가져오기
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "사용자 이름 없음"
                        tvOwnerName.text = "${name} 보호자님"
                    } else {
                        tvOwnerName.text = "이름 정보 없음"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MypageActivity", "Firestore 오류: ${e.message}")
                    tvOwnerName.text = "이름 불러오기 실패"
                }
        } else {
            tvEmail.text = "로그인 필요"
            tvOwnerName.text = "이름 정보 없음"
        }

        // 클릭 이벤트
    //   layoutPasswordSetting.setOnClickListener {
    //       startActivity(Intent(this, PasswordSettingActivity::class.java))
    //   }

    //   layoutDogInfo.setOnClickListener {
    //       startActivity(Intent(this, DogInfoActivity::class.java))
    //   }
    }
}
