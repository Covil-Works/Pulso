package com.covildev.pulso.feature_perfil.data

import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import com.covildev.pulso.feature_perfil.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> {
        return userDao.observeProfile().map { it?.toDomain() }
    }

    override suspend fun getProfile(): UserProfile? = userDao.getProfile()?.toDomain()

    override suspend fun saveProfile(profile: UserProfile) {
        userDao.upsert(profile.toEntity())
    }
}

private fun UserEntity.toDomain(): UserProfile = UserProfile(name = name, age = age)

private fun UserProfile.toEntity(): UserEntity = UserEntity(name = name, age = age)
