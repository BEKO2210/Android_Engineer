package dev.slate.ai.core.common

/**
 * A generic result wrapper for operations that can fail.
 */
sealed interface SlateResult<out T> {
    data class Success<T>(val data: T) : SlateResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : SlateResult<Nothing>
    data object Loading : SlateResult<Nothing>
}
