package com.tuneflow.core.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    data class Error(
        val message: String,
        val kind: NetworkErrorKind = NetworkErrorKind.Server,
        val code: Int? = null,
        val httpCode: Int? = null,
    ) : NetworkResult<Nothing>
}

enum class NetworkErrorKind {
    Network,
    Http,
    Server,
    Parsing,
}
