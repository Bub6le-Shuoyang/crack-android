package com.example.monitor.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/user/send-verification-code")
    suspend fun sendVerificationCode(@Body request: SendCodeRequest): Response<BaseResponse>

    @POST("/api/user/register")
    suspend fun register(@Body request: RegisterRequest): Response<BaseResponse>

    @POST("/api/user/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<BaseResponse>
}