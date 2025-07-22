package com.example.dogwalkapp

import android.app.Application
import com.kakao.sdk.common.KakaoSdk



class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "c6652648e6d66eb28c5a75fec2e683cb")
    }
}