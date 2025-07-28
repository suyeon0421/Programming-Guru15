package com.example.dogwalkapp.models

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize


@Parcelize
data class CourseItem(
    val title: String = "",                  // 사용자 지정 이름
    val pathPoints: List<LatLng> = listOf(), // 경로 좌표 리스트
    val distance: Double = 0.0,              // 거리 (단위: km)
    val duration: Long = 0L,                 // 시간 (단위: 초)
    val createdBy: String = "",
    val calories: Int = 0,              // 작성자 UID
    @IgnoredOnParcel
    val timestamp: Timestamp = Timestamp.now(), // 생성 시간


    //산책 기록 작성에서 추가한 필드
    val walkStyle: String = "",        // 산책 스타일
    val pathReview: String = "",       // 경로 평가
    val memo: String = ""              // 산책 메모
) : Parcelable
