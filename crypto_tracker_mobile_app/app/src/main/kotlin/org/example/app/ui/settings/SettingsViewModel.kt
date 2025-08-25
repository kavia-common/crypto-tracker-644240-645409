package org.example.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.app.data.repository.UserRepository
import org.example.app.notifications.NotificationManager
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val user = userRepository.currentUser
                if (user != null) {
                    _uiState.value = SettingsUiState.Success(
                        email = user.email ?: "",
                        priceAlertsEnabled = notificationManager.isPriceAlertsEnabled(),
                        newsAlertsEnabled = notificationManager.isNewsAlertsEnabled(),
                        selectedCurrency = "USD" // TODO: Load from preferences
                    )
                } else {
                    _uiState.value = SettingsUiState.Error("User not authenticated")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun updatePriceAlerts(enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationManager.updatePriceAlerts(enabled)
                updateUiState { it.copy(priceAlertsEnabled = enabled) }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update price alerts")
            }
        }
    }

    fun updateNewsAlerts(enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationManager.updateNewsAlerts(enabled)
                updateUiState { it.copy(newsAlertsEnabled = enabled) }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update news alerts")
            }
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            try {
                // TODO: Save currency preference
                updateUiState { it.copy(selectedCurrency = currency) }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update currency")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                userRepository.signOut()
                _uiState.value = SettingsUiState.SignedOut
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to sign out")
            }
        }
    }

    private fun updateUiState(update: (SettingsUiState.Success) -> SettingsUiState.Success) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = update(currentState)
        }
    }
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    object SignedOut : SettingsUiState()
    data class Success(
        val email: String,
        val priceAlertsEnabled: Boolean,
        val newsAlertsEnabled: Boolean,
        val selectedCurrency: String
    ) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
