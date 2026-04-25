package com.covildev.pulso.feature_perfil.domain.repository

import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
}
