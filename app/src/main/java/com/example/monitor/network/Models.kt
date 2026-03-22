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

data class ImageUploadResponse(
    val ok: Boolean?,
    val message: String?,
    val data: ImageUploadData?
)

data class ImageUploadData(
    val imageId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val width: Int,
    val height: Int
)

data class ImageListResponse(
    val ok: Boolean?,
    val message: String?,
    val data: ImageListData?
)

data class ImageListData(
    val list: List<ImageItem>?,
    val total: Int?,
    val page: Int?,
    val pageSize: Int?
)

data class ImageItem(
    val id: Long,
    val userId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val width: Int,
    val height: Int,
    val status: Int,
    val createdAt: String,
    val updatedAt: String
)

data class ImageDetailResponse(
    val ok: Boolean?,
    val data: ImageDetailData?
)

data class ImageDetailData(
    val imageId: Long,
    val userId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val width: Int,
    val height: Int,
    val status: Int,
    val createdAt: String,
    val accessUrl: String?,
    val isDetected: Boolean,
    val detectionDate: String?,
    val modelName: String?,
    val results: List<DetectionResult>?
)

data class DetectionResult(
    val label: String,
    val score: Double,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val classId: Int
)