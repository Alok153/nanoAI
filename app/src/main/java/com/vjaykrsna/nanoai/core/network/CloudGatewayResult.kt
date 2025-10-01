package com.vjaykrsna.nanoai.core.network

/**
 * Result wrapper for cloud gateway requests capturing success and error modes.
 */
sealed class CloudGatewayResult<out T> {
    data class Success<T>(val data: T, val latencyMs: Long) : CloudGatewayResult<T>()
    data class HttpError(val statusCode: Int, val message: String? = null) : CloudGatewayResult<Nothing>()
    object Unauthorized : CloudGatewayResult<Nothing>()
    object RateLimited : CloudGatewayResult<Nothing>()
    data class NetworkError(val throwable: Throwable) : CloudGatewayResult<Nothing>()
    data class UnknownError(val throwable: Throwable) : CloudGatewayResult<Nothing>()
}
