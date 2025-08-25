package org.example.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.app.data.api.ChartData
import org.example.app.data.model.Coin
import org.example.app.data.repository.CoinRepository
import org.example.app.data.repository.UserRepository
import javax.inject.Inject

class CoinDetailViewModel @Inject constructor(
    private val coinRepository: CoinRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val coinId: String = checkNotNull(savedStateHandle["coinId"])
    private val _uiState = MutableStateFlow<CoinDetailUiState>(CoinDetailUiState.Loading)
    val uiState: StateFlow<CoinDetailUiState> = _uiState

    private val _inWatchlist = MutableStateFlow(false)
    val inWatchlist: StateFlow<Boolean> = _inWatchlist

    init {
        loadCoinDetails()
        checkWatchlistStatus()
    }

    fun loadCoinDetails() {
        viewModelScope.launch {
            _uiState.value = CoinDetailUiState.Loading
            try {
                coinRepository.coins.collect { coins ->
                    val coin = coins.find { it.id == coinId }
                    if (coin != null) {
                        loadChartData(coin, 1)
                    } else {
                        _uiState.value = CoinDetailUiState.Error("Coin not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CoinDetailUiState.Error(e.message ?: "Failed to load coin details")
            }
        }
    }

    fun loadChartData(coin: Coin, days: Int) {
        viewModelScope.launch {
            try {
                coinRepository.getCoinChart(coinId, days).collect { chartData ->
                    _uiState.value = CoinDetailUiState.Success(coin, chartData)
                }
            } catch (e: Exception) {
                _uiState.value = CoinDetailUiState.Error(e.message ?: "Failed to load chart data")
            }
        }
    }

    private fun checkWatchlistStatus() {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                userRepository.getUser(userId).collect { user ->
                    _inWatchlist.value = user.watchlist.contains(coinId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                userRepository.getUser(userId).collect { user ->
                    val updatedWatchlist = if (_inWatchlist.value) {
                        user.watchlist - coinId
                    } else {
                        user.watchlist + coinId
                    }
                    userRepository.updateWatchlist(userId, updatedWatchlist)
                    _inWatchlist.value = !_inWatchlist.value
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

sealed class CoinDetailUiState {
    object Loading : CoinDetailUiState()
    data class Success(
        val coin: Coin,
        val chartData: ChartData
    ) : CoinDetailUiState()
    data class Error(val message: String) : CoinDetailUiState()
}
