package com.covildev.pulso.feature_registro.domain.repository

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import kotlinx.coroutines.flow.Flow

interface BloodPressureRepository {
    fun observeAllRecords(): Flow<List<BloodPressureRecord>>
    fun observeRecordsInPeriod(startInclusive: Long, endInclusive: Long): Flow<List<BloodPressureRecord>>
    suspend fun getAllRecords(): List<BloodPressureRecord>
    suspend fun insertRecord(record: BloodPressureRecord)
    suspend fun updateRecord(record: BloodPressureRecord)
    suspend fun deleteRecord(recordId: Long)
}
