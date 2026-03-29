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

    override suspend fun getAds(): List<Ad> =
        api.getAds().map { dto ->
            Ad(
                id = dto.id,
                imageUrl = dto.imageUrl.toFullImageUrl(),
                name = dto.advertiserName,
                description = dto.description,
                address = dto.address,
                contactPhone = dto.contactPhone,
                whatsapp = dto.whatsapp,
                email = dto.email,
                facebookUrl = dto.facebookUrl,
                instagramUrl = dto.instagramUrl
            )
        }

    override suspend fun getAd(id: String): Ad? {
        return try {
            val dto = api.getAd(id)
            Ad(
                id = dto.id,
                imageUrl = dto.imageUrl.toFullImageUrl(),
                name = dto.advertiserName,
                description = dto.description,
                address = dto.address,
                contactPhone = dto.contactPhone,
                whatsapp = dto.whatsapp,
                email = dto.email,
                facebookUrl = dto.facebookUrl,
                instagramUrl = dto.instagramUrl
            )
        } catch (_: Exception) {
            null
        }
    }
}
