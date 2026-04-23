package com.tuneflow.core.network

import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory {
    fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        require(trimmed.isNotBlank()) { "Server URL is required." }

        val withScheme =
            if (trimmed.contains("://")) {
                trimmed
            } else {
                "http://$trimmed"
            }

        val normalized =
            withScheme.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Enter a valid server URL.")

        return normalized.toString().trimEnd('/')
    }

    fun createApi(baseUrl: String): NavidromeApi {
        val normalized = "${normalizeBaseUrl(baseUrl)}/"

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
