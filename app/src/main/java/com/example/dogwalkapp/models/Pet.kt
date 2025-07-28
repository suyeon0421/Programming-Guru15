// data/model/Pet.kt
package com.your_app_package_name.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Pet(
    var name: String = "", // 반려동물 이름
    var selectedDogBreed: String = "", // 반려동물 견종
    var birthdate: String = "", // 반려동물 생년월일
    var gender: String = "", // 반려동물 성별
    var weight: Double = 0.0, // 반려동물 몸무게
    var isNeutered: Boolean = false // 중성화 여부
) : Parcelable {
    constructor() : this("", "", "", "", 0.0, false)
}