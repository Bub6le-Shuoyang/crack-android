package com.example.monitor.network

data class User(
    val name: String,
    val email: String,
    val role_id: String? = null,
    val roleid: String? = null
)

data class LoginResponse(
    val token: String?,
    val user: User?,
    val code: String?,
    val message: String?
)

data class BaseResponse(
    val ok: Boolean?,
    val message: String?,
    val code: String?,
    val user: User? = null
)

data class SendCodeRequest(
    val email: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val verificationCode: String
)

data class ForgotPasswordRequest(
    val email: String,
    val verificationCode: String,
    val newPassword: String
)

data class LoginRequest(
    val email: String,
    val password: String
)