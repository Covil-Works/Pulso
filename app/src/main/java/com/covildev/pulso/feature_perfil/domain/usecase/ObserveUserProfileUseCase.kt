package com.covildev.pulso.feature_perfil.domain.usecase

import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import com.covildev.pulso.feature_perfil.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeProfile()
}
