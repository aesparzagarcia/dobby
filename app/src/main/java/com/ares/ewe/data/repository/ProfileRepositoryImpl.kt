package com.ares.ewe.data.repository

import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.model.GamificationDto
import com.ares.ewe.domain.repository.ProfileRepository
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val api: DobbyApi,
) : ProfileRepository {

    override suspend fun getGamification(): Result<GamificationDto> = runCatching {
        api.getGamification()
    }
}
