package com.honger.dayplan.domain.glasses

enum class Transport { BLE, USB, UNKNOWN }

data class GlassesDevice (
    val id: String,
    val displayName: String,
    val batteryPercent: Int?,
    val firmwareVersion: String?,
    val transport: Transport,
    val lastSeenEpochMillis: Long?,
)