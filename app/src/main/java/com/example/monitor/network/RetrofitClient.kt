package com.example.monitor.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 使用 10.0.2.2 以便 Android 模拟器访问本地宿主机的 127.0.0.1
    private const val BASE_URL = "http://10.0.2.2:7022/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}