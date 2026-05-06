package com.covildev.pulso.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.covildev.pulso.feature_metas.data.GoalDao
import com.covildev.pulso.feature_metas.data.GoalEntity
import com.covildev.pulso.feature_perfil.data.UserDao
import com.covildev.pulso.feature_perfil.data.UserEntity
import com.covildev.pulso.feature_registro.data.BloodPressureDao
import com.covildev.pulso.feature_registro.data.BloodPressureEntity

@Database(
    entities = [
        UserEntity::class,
        BloodPressureEntity::class,
        GoalEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(PulsoConverters::class)
abstract class PulsoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun goalDao(): GoalDao
}
