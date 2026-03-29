package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.Ad

interface AdsRepository {
    suspend fun getAds(): List<Ad>
    suspend fun getAd(id: String): Ad?
}
