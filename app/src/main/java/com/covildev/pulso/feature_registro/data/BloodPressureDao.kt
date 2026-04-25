package com.covildev.pulso.feature_registro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodPressureDao {
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC")
    fun observeAllRecords(): Flow<List<BloodPressureEntity>>

    @Query(
        "SELECT * FROM blood_pressure " +
            "WHERE timestamp BETWEEN :startInclusive AND :endInclusive " +
            "ORDER BY timestamp DESC",
    )
    fun observeRecordsInPeriod(startInclusive: Long, endInclusive: Long): Flow<List<BloodPressureEntity>>

    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<BloodPressureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BloodPressureEntity)
}
