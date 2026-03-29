package com.ares.ewe.data.repository

import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.session.SessionEventBus
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.model.CompleteRegistrationRequest
import com.ares.ewe.data.remote.model.RequestOtpRequest
import com.ares.ewe.data.remote.model.VerifyOtpRequest
import com.ares.ewe.domain.model.AuthResult
import com.ares.ewe.domain.model.OtpRequestResult
import com.ares.ewe.domain.model.VerifyOtpOutcome
import com.ares.ewe.domain.repository.AuthRepository
import com.ares.ewe.data.remote.ConsumerLaunchRefreshOutcome
import com.ares.ewe.data.remote.ConsumerTokenRefreshService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: DobbyApi,
    private val sessionManager: SessionManager,
    private val consumerTokenRefreshService: ConsumerTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    override suspend fun requestOtp(phone: String): AuthResult<OtpRequestResult> {
        return try {
            val response = api.requestOtp(RequestOtpRequest(phone = phone))
            AuthResult.Success(OtpRequestResult(userExists = response.userExists))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send code")
        }
    }

    override suspend fun verifyOtp(phone: String, code: String): AuthResult<VerifyOtpOutcome> {
        return try {
            val response = api.verifyOtp(VerifyOtpRequest(phone = phone, code = code))
            if (response.requiresRegistration) {
                AuthResult.Success(VerifyOtpOutcome.RequiresRegistration)
            } else {
                val access = response.token
                val refresh = response.refreshToken
                if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
                    return AuthResult.Error("Respuesta de sesión inválida")
                }
                sessionManager.saveSession(access, refresh, response.user?.id)
                sessionEventBus.resetExpiredGate()
                AuthResult.Success(VerifyOtpOutcome.LoggedIn)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Invalid code")
        }
    }

    override suspend fun completeRegistration(
        phone: String,
        name: String,
        lastName: String,
        email: String
    ): AuthResult<Unit> {
        return try {
            val response = api.completeRegistration(
                CompleteRegistrationRequest(
                    phone = phone,
                    name = name,
                    lastName = lastName,
                    email = email
                )
            )
            val refresh = response.refreshToken
            if (refresh.isNullOrBlank()) {
                return AuthResult.Error("Respuesta de sesión inválida")
            }
            sessionManager.saveSession(response.token, refresh, response.user?.id)
            sessionEventBus.resetExpiredGate()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun syncSessionAtLaunch(): Boolean {
        if (!sessionManager.isLoggedIn.first()) return false
        return when (consumerTokenRefreshService.refreshStoredSession(sessionManager)) {
            ConsumerLaunchRefreshOutcome.SessionDead -> false
            ConsumerLaunchRefreshOutcome.Skipped,
            ConsumerLaunchRefreshOutcome.Refreshed,
            ConsumerLaunchRefreshOutcome.Unchanged -> true
        }
    }
}
