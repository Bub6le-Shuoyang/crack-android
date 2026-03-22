package com.example.monitor.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    @POST("/api/user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/user/send-verification-code")
    suspend fun sendVerificationCode(@Body request: SendCodeRequest): Response<BaseResponse>

    @POST("/api/user/register")
    suspend fun register(@Body request: RegisterRequest): Response<BaseResponse>

    @POST("/api/user/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<BaseResponse>

    @Multipart
    @POST("/api/file/upload-image")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody? = null
    ): Response<ImageUploadResponse>

    @GET("/api/file/list-images")
    suspend fun listImages(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = 1,
        @Query("pageSize") pageSize: Int? = 10,
        @Query("fileType") fileType: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("label") label: String? = null
    ): Response<ImageListResponse>

    @DELETE("/api/file/delete-image/{imageId}")
    suspend fun deleteImage(
        @Header("Authorization") token: String,
        @Path("imageId") imageId: Long
    ): Response<BaseResponse>

    @GET("/api/file/get-image-by-id/{imageId}")
    suspend fun getImageDetail(
        @Header("Authorization") token: String,
        @Path("imageId") imageId: Long
    ): Response<ImageDetailResponse>

    @POST("/api/file/batch-delete-images")
    suspend fun batchDeleteImages(
        @Header("Authorization") token: String,
        @Body request: BatchDeleteRequest
    ): Response<BatchDeleteResponse>

    @POST("/model/detectBatch")
    suspend fun detectBatch(
        @Body request: DetectBatchRequest
    ): Response<Map<String, List<DetectionResult>>>

    @GET("/api/user/info")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): Response<UserInfoResponse>

    @POST("/api/user/update-profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @Multipart
    @POST("/api/user/update-avatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Response<AvatarUploadResponse>

    @POST("/api/user/update-password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<BaseResponse>

    @POST("/api/user/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<BaseResponse>

    @Multipart
    @POST("/api/file/upload-video")
    suspend fun uploadVideo(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody? = null,
        @Part("generateCover") generateCover: RequestBody? = null
    ): Response<VideoUploadResponse>

    @GET("/api/file/list-videos")
    suspend fun listVideos(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = 1,
        @Query("pageSize") pageSize: Int? = 10,
        @Query("fileType") fileType: String? = null
    ): Response<VideoListResponse>

    @DELETE("/api/file/delete-video/{videoId}")
    suspend fun deleteVideo(
        @Header("Authorization") token: String,
        @Path("videoId") videoId: Long
    ): Response<BaseResponse>

    @POST("/model/detectVideo/{videoId}")
    suspend fun detectVideo(
        @Path("videoId") videoId: Long
    ): Response<BaseResponse>

    @GET("/model/detectVideo/progress/{videoId}")
    suspend fun getVideoProgress(
        @Path("videoId") videoId: Long
    ): Response<VideoProgressResponse>

    @GET("/api/statistics/image-overview")
    suspend fun getImageOverview(
        @Header("Authorization") token: String
    ): Response<ImageOverviewResponse>

    @GET("/api/statistics/anomaly-type-distribution")
    suspend fun getAnomalyTypeDistribution(
        @Header("Authorization") token: String,
        @Query("mediaType") mediaType: String? = "image",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<AnomalyDistributionResponse>
}