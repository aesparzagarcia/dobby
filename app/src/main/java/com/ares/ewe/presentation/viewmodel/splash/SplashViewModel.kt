package com.ares.ewe.presentation.viewmodel.splash

import androidx.lifecycle.ViewModel
import com.ares.ewe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Always open Home (guest browse). Sync session when already logged in. */
    suspend fun shouldOpenHomeAfterSplash(): Boolean {
        if (authRepository.isLoggedIn.first()) {
            authRepository.syncSessionAtLaunch()
        }
        return true
    }
}
