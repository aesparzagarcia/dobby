package com.ares.ewe.domain.repository

import com.ares.ewe.data.remote.model.GamificationDto

interface ProfileRepository {
    suspend fun getGamification(): Result<GamificationDto>
}
