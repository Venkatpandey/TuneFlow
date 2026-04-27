package com.tuneflow.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

object NetworkFactory {
    private val sharedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

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
            try {
                URI(withScheme)
            } catch (_: URISyntaxException) {
                throw IllegalArgumentException("Enter a valid server URL.")
            }

        val scheme = normalized.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") {
            "Enter a valid server URL."
        }

        val authority = normalized.rawAuthority
        require(!authority.isNullOrBlank()) {
            "Enter a valid server URL."
        }

        require(normalized.rawQuery == null && normalized.rawFragment == null) {
            "Enter a valid server URL."
        }

        val path = normalized.rawPath?.trimEnd('/').orEmpty()

        return buildString {
            append(scheme)
            append("://")
            append(authority)
            if (path.isNotEmpty()) {
                append(path)
            }
        }
    }

    fun createApi(baseUrl: String): NavidromeApi {
        val normalized = "${normalizeBaseUrl(baseUrl)}/"

        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NavidromeApi::class.java)
    }
}
