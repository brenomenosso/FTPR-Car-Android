package com.example.myapitest.repositories

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

object RetrofitClient {

    private const val BASE_URL = "http://localhost:3000"
    private val loggingInterceptor = HttpLoggingInterceptor()
        .apply { level = HttpLoggingInterceptor.Level.BODY }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService = instance.create(ApiService::class.java)
}