package com.honger.dayplan.data.glasses

import com.honger.dayplan.domain.glasses.GlassesDevice
import com.honger.dayplan.domain.glasses.Transport

fun GlassesDeviceDto.toDomainOrNull(): GlassesDevice? {
    val normalizedId = id?.trim()?.takeIf(String::isNotEmpty)?: return null
    return GlassesDevice(
        id = normalizedId,
        displayName = displayName?.trim().orEmpty().ifBlank { "Unnamed glasses" },
        batteryPercent = batteryPercent?.coerceIn(0..100),
        firmwareVersion = firmwareVersion?.trim()?.takeIf (String::isNotEmpty),
        transport = when (connectType?.lowercase()) {
            "ble" -> Transport.BLE
            "usb" -> Transport.USB
            else -> Transport.UNKNOWN
        },
        lastSeenEpochMillis = lastSeenEpochMillis,

    )
}