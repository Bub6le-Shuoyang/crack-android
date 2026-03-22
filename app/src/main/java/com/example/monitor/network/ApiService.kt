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
}