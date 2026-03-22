package com.example.monitor.repository

import com.example.monitor.network.*

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(req: LoginRequest) = api.login(req)
    suspend fun register(req: RegisterRequest) = api.register(req)
    suspend fun sendCode(req: SendCodeRequest) = api.sendVerificationCode(req)
    suspend fun forgotPassword(req: ForgotPasswordRequest) = api.forgotPassword(req)
}