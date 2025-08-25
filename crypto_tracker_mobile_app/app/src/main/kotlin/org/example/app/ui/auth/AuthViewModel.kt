package org.example.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.app.data.repository.UserRepository
import javax.inject.Inject

class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = userRepository.currentUser
        if (currentUser != null) {
            _uiState.value = AuthUiState.Authenticated
        }
    }

    fun login(email: String, password: String) {
        if (!validateInput(email, password)) {
            _uiState.value = AuthUiState.Error("Please enter valid email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                userRepository.signIn(email, password).collect {
                    _uiState.value = AuthUiState.Authenticated
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (!validateInput(email, password)) {
            _uiState.value = AuthUiState.Error("Please enter valid email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                userRepository.signUp(email, password).collect {
                    _uiState.value = AuthUiState.Authenticated
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotBlank() && password.length >= 6
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Authenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
