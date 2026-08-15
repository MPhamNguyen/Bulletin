package com.jdrms.bulletin.core.common

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error"
    ) : Result<Nothing>

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
    }
}
