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
    val results: List<DetectionResult>?,
    val location: LocationData? = null
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

data class LocationRequest(
    val imageId: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

data class LocationResponse(
    val ok: Boolean?,
    val message: String?,
    val data: LocationData?
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

data class DetectBatchRequest(
    val imageIds: List<Long>
)

data class DetectBatchResponse(
    val ok: Boolean?,
    val message: String?,
    val data: Map<String, List<DetectionResult>>?
)

data class BatchDeleteRequest(
    val imageIds: List<Long>
)

data class BatchDeleteResponse(
    val ok: Boolean?,
    val message: String?,
    val data: BatchDeleteData?
)

data class BatchDeleteData(
    val successIds: List<Long>?,
    val failIds: List<Long>?,
    val failReasons: Map<String, String>?
)

data class UserInfoResponse(
    val ok: Boolean?,
    val data: UserInfoData?
)

data class UserInfoData(
    val userId: Long,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val roleId: String?,
    val roleName: String?,
    val status: Int?,
    val lastLoginAt: String?,
    val createdAt: String?
)

data class UpdateProfileRequest(
    val name: String?,
    val email: String?,
    val password: String? = null
)

data class UpdateProfileResponse(
    val ok: Boolean?,
    val message: String?,
    val data: Map<String, String>?
)

data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class AvatarUploadResponse(
    val ok: Boolean?,
    val message: String?,
    val data: AvatarData?
)

data class AvatarData(
    val avatarId: Long,
    val avatarUrl: String
)

data class VideoUploadResponse(
    val ok: Boolean?,
    val message: String?,
    val data: VideoUploadData?
)

data class VideoUploadData(
    val videoId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val duration: Double,
    val coverPath: String,
    val isDetected: Int
)

data class VideoListResponse(
    val ok: Boolean?,
    val data: VideoListData?
)

data class VideoListData(
    val list: List<VideoItem>?,
    val total: Int?,
    val page: Int?,
    val pageSize: Int?
)

data class VideoItem(
    val id: Long,
    val videoId: Long,
    val userId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val duration: Double?,
    val coverPath: String?,
    val status: Int,
    val isDetected: Int,
    val createdAt: String,
    val anomalyCount: Int?,
    val anomalyFrames: Any? // Since the backend returns either a number or a list, we use Any? to prevent parsing exceptions
)

data class VideoProgressResponse(
    val ok: Boolean?,
    val progress: Int?,
    val error: Boolean?,
    val message: String?,
    val data: VideoDetectionData?
)

data class VideoDetectionData(
    val videoId: Long,
    val totalFramesProcessed: Int,
    val anomalyCount: Int,
    val anomalyFrames: List<AnomalyFrame>
)

data class AnomalyFrame(
    val time: Double,
    val timeFormatted: String,
    val frameNumber: Int,
    val detections: List<DetectionResult>
)

data class ImageOverviewResponse(
    val ok: Boolean?,
    val data: ImageOverviewData?
)

data class ImageOverviewData(
    val totalImages: Int,
    val totalAnomalyImages: Int,
    val anomalyRate: Double,
    val topAnomalyType: List<AnomalyTypeData>,
    val dailyTrend: List<DailyTrendData>
)

data class AnomalyTypeData(
    val label: String,
    val count: Int,
    val rate: Double?
)

data class DailyTrendData(
    val date: String,
    val total: Int,
    val anomaly: Int
)

data class AnomalyDistributionResponse(
    val ok: Boolean?,
    val data: AnomalyDistributionData?
)

data class AnomalyDistributionData(
    val mediaType: String,
    val totalAnomalies: Int,
    val distribution: List<DistributionItem>,
    val confidenceDistribution: List<ConfidenceDistributionItem>
)

data class DistributionItem(
    val label: String,
    val count: Int,
    val percentage: Double
)

data class ConfidenceDistributionItem(
    val range: String,
    val count: Int
)