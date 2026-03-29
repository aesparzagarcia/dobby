package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.AuthResult
import com.ares.ewe.domain.model.OtpRequestResult
import com.ares.ewe.domain.model.VerifyOtpOutcome
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val isLoggedIn: Flow<Boolean>

    suspend fun requestOtp(phone: String): AuthResult<OtpRequestResult>

    suspend fun verifyOtp(phone: String, code: String): AuthResult<VerifyOtpOutcome>

    suspend fun completeRegistration(
        phone: String,
        name: String,
        lastName: String,
        email: String
    ): AuthResult<Unit>

    suspend fun logout()

    /**
     * If the user has a refresh token, exchanges it for new tokens before the main UI loads.
     * Returns false if the refresh was rejected and the session was cleared.
     */
    suspend fun syncSessionAtLaunch(): Boolean
}
