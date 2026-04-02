package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class GamificationDto(
    @SerializedName("dobby_xp") val dobbyXp: Int,
    @SerializedName("level_key") val levelKey: String,
    @SerializedName("level_name") val levelName: String,
    @SerializedName("xp_at_level_start") val xpAtLevelStart: Int,
    @SerializedName("xp_for_next_level") val xpForNextLevel: Int?,
    @SerializedName("order_streak_days") val orderStreakDays: Int,
    @SerializedName("total_orders_delivered") val totalOrdersDelivered: Int,
    val name: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String = "",
    val phone: String? = null,
    @SerializedName("recent_events") val recentEvents: List<GamificationEventDto> = emptyList(),
)

data class GamificationEventDto(
    val delta: Int,
    val reason: String,
    @SerializedName("created_at") val createdAt: String,
)
