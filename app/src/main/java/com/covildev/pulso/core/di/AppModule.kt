package com.covildev.pulso.core.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.covildev.pulso.core.data.local.PulsoDatabase
import com.covildev.pulso.core.notification.AlarmReminderScheduler
import com.covildev.pulso.core.notification.ReminderScheduler
import com.covildev.pulso.feature_metas.data.GoalDao
import com.covildev.pulso.feature_metas.data.GoalRepositoryImpl
import com.covildev.pulso.feature_metas.domain.repository.GoalRepository
import com.covildev.pulso.feature_perfil.data.ProfileRepositoryImpl
import com.covildev.pulso.feature_perfil.data.UserDao
import com.covildev.pulso.feature_perfil.domain.repository.ProfileRepository
import com.covildev.pulso.feature_registro.data.BloodPressureDao
import com.covildev.pulso.feature_registro.data.BloodPressureRepositoryImpl
import com.covildev.pulso.feature_registro.domain.repository.BloodPressureRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindBloodPressureRepository(impl: BloodPressureRepositoryImpl): BloodPressureRepository

    @Binds
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goal_settings ADD COLUMN alarmNote TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN additionalInfo TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): PulsoDatabase {
            return Room.databaseBuilder(
                context,
                PulsoDatabase::class.java,
                "pulso.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        }

        @Provides
        fun provideUserDao(database: PulsoDatabase): UserDao = database.userDao()

        @Provides
        fun provideBloodPressureDao(database: PulsoDatabase): BloodPressureDao = database.bloodPressureDao()

        @Provides
        fun provideGoalDao(database: PulsoDatabase): GoalDao = database.goalDao()
    }
}
