package com.example.dogwalkapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.dogwalkapp.R

class PetInfoActivity : AppCompatActivity() {

    private lateinit var editPetName: EditText
    private lateinit var editPetBirth: EditText
    private lateinit var editPetWeight: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rgNeuter: RadioGroup
    private lateinit var rgType: RadioGroup
    private lateinit var autoBreed: AutoCompleteTextView
    private lateinit var btnNext: Button
    private lateinit var btnRegisterLater: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_info)

        // View 바인딩
        editPetName = findViewById(R.id.edit_pet_name)
        editPetBirth = findViewById(R.id.edit_pet_birth)
        editPetWeight = findViewById(R.id.edit_pet_weight)
        rgGender = findViewById(R.id.rg_gender)
        rgNeuter = findViewById(R.id.rg_neuter)
        rgType = findViewById(R.id.rg_type)
        autoBreed = findViewById(R.id.auto_breed)
        btnNext = findViewById(R.id.btn_next)
        btnRegisterLater = findViewById(R.id.btn_register_later)

        // 견종 오토컴플릿 연결
        val breedList = resources.getStringArray(R.array.breed_list)
        val breedAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, breedList)
        autoBreed.setAdapter(breedAdapter)
        autoBreed.threshold = 1

        btnNext.setOnClickListener {
            val name = editPetName.text.toString().trim()
            val birth = editPetBirth.text.toString().trim()
            val weightStr = editPetWeight.text.toString().trim()
            val breed = autoBreed.text.toString().trim()

            // 성별 선택값
            val selectedGenderId = rgGender.checkedRadioButtonId
            val gender = when(selectedGenderId) {
                R.id.rb_male -> "남아"
                R.id.rb_female -> "여아"
                else -> ""
            }

            // 중성화 여부 선택값
            val selectedNeuterId = rgNeuter.checkedRadioButtonId
            val neutered = when(selectedNeuterId) {
                R.id.rb_neutered -> true
                R.id.rb_not_neutered -> false
                else -> null
            }

            // 유형(소/중/대형) 선택값
            val selectedTypeId = rgType.checkedRadioButtonId
            val type = when(selectedTypeId) {
                R.id.rb_small -> "소형견"
                R.id.rb_medium -> "중형견"
                R.id.rb_large -> "대형견"
                else -> ""
            }

            // 유효성 체크
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (birth.isEmpty()) {
                Toast.makeText(this, "생년월일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (weightStr.isEmpty()) {
                Toast.makeText(this, "몸무게를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (breed.isEmpty()) {
                Toast.makeText(this, "견종을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (gender.isEmpty()) {
                Toast.makeText(this, "성별을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (neutered == null) {
                Toast.makeText(this, "중성화 여부를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (type.isEmpty()) {
                Toast.makeText(this, "유형(소/중/대형)을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 몸무게 숫자확인
            val weight = weightStr.toFloatOrNull()
            if (weight == null || weight <= 0) {
                Toast.makeText(this, "올바른 몸무게를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 입력한 반려견 정보 DB 저장, 혹은 ViewModel, 서버 연동 등의 구현 필요
            // FirebaseFirestore에 오류 남.. 제가 json파일 없어서 그런거 같기도 하고 확인해주세요!
//                // 여기에 저장 코드 삽입 (Firestore 예시)
//                val petInfo = hashMapOf(
//                    "name" to name,
//                    "birth" to birth,
//                    "weight" to weight,
//                    "breed" to breed,
//                    "gender" to gender,
//                    "neutered" to neutered,
//                    "type" to type
//                )
//
//                FirebaseFirestore.getInstance().collection("pets")
//                    .add(petInfo)
//                    .addOnSuccessListener {
//                        Toast.makeText(this, "반려견 정보가 저장되었습니다!", Toast.LENGTH_SHORT).show()
//                        val intent = Intent(this, SignupCompleteActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    }
//                    .addOnFailureListener { e ->
//                        Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
//                    }
//            }


            // 등록 성공 안내
            Toast.makeText(this, "반려견 정보가 저장되었습니다!", Toast.LENGTH_SHORT).show()

            // 회원가입 완료 화면으로 이동
            val intent = Intent(this, SignupCompleteActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnRegisterLater.setOnClickListener {
            // '나중에 등록하기' 클릭 시: 정보 저장 없이 회원가입 완료 화면으로 이동
            val intent = Intent(this, SignupCompleteActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
