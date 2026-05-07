package com.covildev.pulso.feature_alarm.model

enum class AlarmType(
    val rawValue: String,
    val stableCode: Int,
) {
    BLOOD_PRESSURE(
        rawValue = "blood_pressure",
        stableCode = 1,
    ),
    MEDICATION(
        rawValue = "medication",
        stableCode = 2,
    ),
    OTHER(
        rawValue = "other",
        stableCode = 3,
    ),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AlarmType {
            return entries.firstOrNull { it.rawValue == rawValue } ?: OTHER
        }
    }
}
