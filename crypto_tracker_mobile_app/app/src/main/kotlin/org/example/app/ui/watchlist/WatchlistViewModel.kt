package org.example.app.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.example.app.data.model.Coin
import org.example.app.data.repository.CoinRepository
import org.example.app.data.repository.UserRepository
import javax.inject.Inject

class WatchlistViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val coinRepository: CoinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init {
        loadWatchlist()
    }

    fun loadWatchlist() {
        viewModelScope.launch {
            _uiState.value = WatchlistUiState.Loading
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                combine(
                    userRepository.getUser(userId),
                    coinRepository.getCoins()
                ) { user, allCoins ->
                    val watchlistCoins = allCoins.filter { coin ->
                        user.watchlist.contains(coin.id)
                    }
                    if (watchlistCoins.isEmpty()) {
                        WatchlistUiState.Empty
                    } else {
                        WatchlistUiState.Success(watchlistCoins)
                    }
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = WatchlistUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun addToWatchlist(coinId: String) {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                userRepository.getUser(userId).collect { user ->
                    val updatedWatchlist = user.watchlist + coinId
                    userRepository.updateWatchlist(userId, updatedWatchlist)
                    loadWatchlist()
                }
            } catch (e: Exception) {
                _uiState.value = WatchlistUiState.Error(e.message ?: "Failed to add to watchlist")
            }
        }
    }

    fun removeFromWatchlist(coinId: String) {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                userRepository.getUser(userId).collect { user ->
                    val updatedWatchlist = user.watchlist - coinId
                    userRepository.updateWatchlist(userId, updatedWatchlist)
                    loadWatchlist()
                }
            } catch (e: Exception) {
                _uiState.value = WatchlistUiState.Error(e.message ?: "Failed to remove from watchlist")
            }
        }
    }
}

sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    object Empty : WatchlistUiState()
    data class Success(val coins: List<Coin>) : WatchlistUiState()
    data class Error(val message: String) : WatchlistUiState()
}
