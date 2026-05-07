package com.covildev.pulso.feature_perfil.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import com.covildev.pulso.feature_perfil.domain.usecase.ObserveUserProfileUseCase
import com.covildev.pulso.feature_perfil.domain.usecase.SaveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = observeUserProfileUseCase()
        .map { profile ->
            ProfileUiState(
                isLoading = false,
                profile = profile,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )

    suspend fun saveProfile(name: String, ageInput: String, additionalInfo: String): Result<Unit> {
        val age = ageInput.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("A idade deve ser numerica."))
        return saveUserProfileUseCase(
            name = name,
            age = age,
            additionalInfo = additionalInfo,
        )
    }
}
