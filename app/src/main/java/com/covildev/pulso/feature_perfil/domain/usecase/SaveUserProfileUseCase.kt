package com.covildev.pulso.feature_perfil.domain.usecase

import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import com.covildev.pulso.feature_perfil.domain.repository.ProfileRepository
import javax.inject.Inject

class SaveUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(name: String, age: Int, additionalInfo: String): Result<Unit> {
        val normalizedName = name.trim()
        val normalizedAdditionalInfo = additionalInfo.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Informe um nome valido."))
        }
        if (age !in 1..120) {
            return Result.failure(IllegalArgumentException("A idade deve estar entre 1 e 120."))
        }
        repository.saveProfile(
            UserProfile(
                name = normalizedName,
                age = age,
                additionalInfo = normalizedAdditionalInfo,
            ),
        )
        return Result.success(Unit)
    }
}
