package org.example.app.ui.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.app.data.model.Coin
import org.example.app.data.repository.CoinRepository
import javax.inject.Inject

class MarketsViewModel @Inject constructor(
    private val repository: CoinRepository
) : ViewModel() {

    val uiState: StateFlow<MarketsUiState> = repository.coins
        .map { coins -> MarketsUiState.Success(coins) as MarketsUiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MarketsUiState.Loading
        )

    init {
        loadCoins()
    }

    fun loadCoins() {
        viewModelScope.launch {
            try {
                repository.startPriceUpdates()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopPriceUpdates()
    }
}

sealed class MarketsUiState {
    object Loading : MarketsUiState()
    data class Success(val coins: List<Coin>) : MarketsUiState()
    data class Error(val message: String) : MarketsUiState()
}
