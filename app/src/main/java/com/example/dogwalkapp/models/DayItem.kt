package com.example.dogwalkapp.models

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.type.DayOfWeek
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Parcelize
@IgnoreExtraProperties
class DayItem(
    val date: LocalDate?,
    val dayNumber: Int?,
    val walked: Boolean,
    ) : Parcelable