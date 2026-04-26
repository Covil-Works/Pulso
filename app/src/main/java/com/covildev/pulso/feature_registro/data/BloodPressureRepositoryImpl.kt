package com.covildev.pulso.feature_registro.data

import com.covildev.pulso.feature_registro.domain.model.BloodPressureRecord
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BloodPressureRepositoryImpl @Inject constructor(
    private val dao: BloodPressureDao,
) : BloodPressureRepository {
    override fun observeAllRecords(): Flow<List<BloodPressureRecord>> {
        return dao.observeAllRecords().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeRecordsInPeriod(
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<BloodPressureRecord>> {
        return dao.observeRecordsInPeriod(startInclusive, endInclusive)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getAllRecords(): List<BloodPressureRecord> {
        return dao.getAllRecords().map { it.toDomain() }
    }

    override suspend fun insertRecord(record: BloodPressureRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun updateRecord(record: BloodPressureRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun deleteRecord(recordId: Long) {
        dao.deleteById(recordId)
    }
}

private fun BloodPressureEntity.toDomain(): BloodPressureRecord {
    return BloodPressureRecord(
        id = id,
        systolic = systolic,
        diastolic = diastolic,
        timestamp = timestamp,
        notes = notes,
        riskLevel = riskLevel,
    )
}

private fun BloodPressureRecord.toEntity(): BloodPressureEntity {
    return BloodPressureEntity(
        id = id,
        systolic = systolic,
        diastolic = diastolic,
        timestamp = timestamp,
        notes = notes,
        riskLevel = riskLevel,
    )
}
