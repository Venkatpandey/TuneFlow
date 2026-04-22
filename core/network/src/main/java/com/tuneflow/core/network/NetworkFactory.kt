package com.tuneflow.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory {
    fun createApi(baseUrl: String): NavidromeApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val client =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NavidromeApi::class.java)
    }
}
