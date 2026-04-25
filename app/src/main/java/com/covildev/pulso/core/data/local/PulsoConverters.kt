package com.covildev.pulso.core.data.local

import androidx.room.TypeConverter
import com.covildev.pulso.feature_registro.domain.model.RiskLevel

class PulsoConverters {
    @TypeConverter
    fun fromRiskLevel(value: RiskLevel): String = value.name

    @TypeConverter
    fun toRiskLevel(value: String): RiskLevel = RiskLevel.valueOf(value)

    @TypeConverter
    fun fromIntList(values: List<Int>): String = values.joinToString(separator = ",")

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        if (value.isBlank()) return emptyList()
        return value.split(",").mapNotNull { item ->
            item.trim().toIntOrNull()
        }
    }

    @TypeConverter
    fun fromStringList(values: List<String>): String = values.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
