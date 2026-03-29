package com.ares.ewe.domain.model

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

data class OtpRequestResult(val userExists: Boolean)

sealed class VerifyOtpOutcome {
    object LoggedIn : VerifyOtpOutcome()
    object RequiresRegistration : VerifyOtpOutcome()
}
