package com.example.dogwalkapp

import android.content.Context
import android.util.Log
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class KakaoAuthManager(private val context: Context) {

    private val _kakaoLoginResult = Channel<Result<User>>()
    val kakaoLoginResult = _kakaoLoginResult.receiveAsFlow()

    companion object {
        private const val TAG = "KakaoAuthManager"
    }
    //카카로 로그인 콜백 함수
    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e(TAG, "카카오 로그인 실패", error)
            if(error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                //로그인을 취소함
                _kakaoLoginResult.trySend(Result.failure(KakaoAuthError.UserCancelled))
            } else {
                //다른 에러라면
                _kakaoLoginResult.trySend(Result.failure(error))
            }
            //성공 시
        } else if (token != null) {
            Log.i(TAG, "카카오 로그인 성공: ${token.accessToken}")
            getUserInfo()
        }
    }

    //사용자 정보를 가져오는 함수
    private fun getUserInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "카카오 사용자 정보 요청 실패", error)
                _kakaoLoginResult.trySend(Result.failure(error))
            } else if (user != null) {
                Log.i(TAG, "카카오 사용자 정보 요청 성공: ${user.id}")
                _kakaoLoginResult.trySend(Result.success(user))
            }
        }
    }

    //로그아웃
    fun logout() {
        UserApiClient.instance.logout { error ->
            if(error != null) {
                Log.e(TAG, "카카오 로그아웃 실패", error)
            } else {
                Log.i(TAG, "카카오 로그아웃 성공")
            }
        }
    }

    //회원 탈퇴
    fun unlink() {
        UserApiClient.instance.unlink { error ->
            if(error != null) {
                Log.e(TAG, "회원 탈퇴 실패", error)
            } else {
                Log.i(TAG, "회원 탈퇴 성공")
            }
        }
    }

    //사용자 직접 취소 등 다른 에러 정의
    sealed class KakaoAuthError: Throwable() {
        object UserCancelled: KakaoAuthError()
    }
}