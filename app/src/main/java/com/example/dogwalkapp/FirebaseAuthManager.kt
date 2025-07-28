package com.example.dogwalkapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _firebaseAuthResult = Channel<Result<FirebaseUser>>()
    val firebaseAuthResult = _firebaseAuthResult.receiveAsFlow()

    private val _passwordResetResult = Channel<Result<Unit>>()
    val passwordResetResult = _passwordResetResult.receiveAsFlow()

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    //일반 회원가입, 이메일과 비밀번호로 Firebase 계정 생성
    suspend fun createAccount(email: String, password: String) {
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if(user != null) {
                Log.i(TAG, "Firebase 계정 생성 성공 ${user.uid}")
                _firebaseAuthResult.trySend(Result.success(user))
            } else {
                _firebaseAuthResult.trySend(Result.failure(FirebaseAuthError.UnknownError("Firebase 생성 후 계정이 없습니다.")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 계정 생성 실패", e)
            _firebaseAuthResult.trySend(Result.failure(e))
        }
    }

    //이메일과 비밀번호로 로그인
    suspend fun signInAccount(email: String, password: String) {
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if(user != null) {
                Log.i(TAG, "Firebase 로그인 성공: ${user.uid}")
                _firebaseAuthResult.trySend(Result.success(user))
            } else {
                _firebaseAuthResult.trySend(Result.failure(FirebaseAuthError.UnknownError("Firebase 생성 후 계정이 없습니다.")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 로그인 실패", e)
            _firebaseAuthResult.trySend(Result.failure(e))
        }
    }

    //입력된 이메일 주소로 비밀번호 재설정 이메일을 보냄
    suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.i(TAG, "비밀번호 재설정 이메일 전송 성공: $email")
            _passwordResetResult.trySend(Result.success(Unit)) // 성공 시 Unit 반환
        } catch (e: Exception) {
            Log.e(TAG, "비밀번호 재설정 이메일 전송 실패", e)
            _passwordResetResult.trySend(Result.failure(e)) // 실패 시 예외 반환
        }
    }

    //현재 로그인된 Firebase 사용자를 가져옴
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun signOut() {
        firebaseAuth.signOut()
        Log.i(TAG, "Firebase 로그아웃 성공")
    }

    sealed class FirebaseAuthError: Throwable() {
        data class UnknownError(override val message: String) : FirebaseAuthError()
        data class AuthenticationFailed(override val message: String) : FirebaseAuthError()
    }
}