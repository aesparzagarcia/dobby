package com.ares.ewe.data.repository

import com.ares.ewe.BuildConfig
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.domain.model.Ad
import com.ares.ewe.domain.repository.AdsRepository
import javax.inject.Inject

class AdsRepositoryImpl @Inject constructor(
    private val api: DobbyApi
) : AdsRepository {

    private val imageBaseUrl: String
        get() = BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')

    private fun String?.toFullImageUrl(): String? =
        if (this == null) null
        else if (this.startsWith("http")) this
        else "$imageBaseUrl$this"

    private fun mapAd(dto: com.ares.ewe.data.remote.model.AdDto) = Ad(
        id = dto.id,
        imageUrl = dto.imageUrl.toFullImageUrl(),
        name = dto.advertiserName,
        description = dto.description,
        address = dto.address,
        contactPhone = dto.contactPhone,
        whatsapp = dto.whatsapp,
        email = dto.email,
        facebookUrl = dto.facebookUrl,
        instagramUrl = dto.instagramUrl,
        priority = (dto.priority ?: 0).coerceIn(0, 3),
    )

    override suspend fun getAds(): List<Ad> = api.getAds().map(::mapAd)

    override suspend fun getAd(id: String): Ad? {
        return try {
            mapAd(api.getAd(id))
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun recordAdView(id: String) {
        runCatching { api.recordAdView(id) }
    }

    override suspend fun recordAdClick(id: String) {
        runCatching { api.recordAdClick(id) }
    }
}
