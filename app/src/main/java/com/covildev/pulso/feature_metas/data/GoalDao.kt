package com.covildev.pulso.feature_metas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal_settings WHERE id = 1")
    fun observeGoals(): Flow<GoalEntity?>

    @Query("SELECT * FROM goal_settings WHERE id = 1")
    suspend fun getGoals(): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goals: GoalEntity)
}
