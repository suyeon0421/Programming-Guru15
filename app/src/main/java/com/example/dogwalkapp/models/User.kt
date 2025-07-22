package com.example.dogwalkapp.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.your_app_package_name.data.model.Pet
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class User(
    @get:Exclude //이메일 주소는 Firebase에 있으므로 중복 저장X
    var email: String = "",
    var password: String = "",

    var pet: Pet = Pet(),

    @get:Exclude
    var uid: String = ""
) : Parcelable {
    constructor() : this("","")
}