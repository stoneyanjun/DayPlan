package com.honger.dayplan.data.glasses

data class GlassesDeviceDto (
    val id: String?,
    val displayName: String?,
    val batteryPercent: Int?,
    val firmwareVersion: String?,
    val connectType: String?,
    val lastSeenEpochMillis: Long?
)