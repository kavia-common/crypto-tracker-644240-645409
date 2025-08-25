package org.example.app.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.example.app.data.model.PortfolioItem
import org.example.app.data.repository.CoinRepository
import org.example.app.data.repository.UserRepository
import javax.inject.Inject

class PortfolioViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val coinRepository: CoinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState: StateFlow<PortfolioUiState> = _uiState

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.value = PortfolioUiState.Loading
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                combine(
                    userRepository.getUser(userId),
                    coinRepository.getCoins()
                ) { user, allCoins ->
                    val portfolioItems = user.portfolio.map { portfolioItem ->
                        val coin = allCoins.find { it.id == portfolioItem.coinId }
                        PortfolioItemUi(
                            coinId = portfolioItem.coinId,
                            symbol = coin?.symbol ?: "",
                            name = coin?.name ?: "",
                            image = coin?.image ?: "",
                            quantity = portfolioItem.quantity,
                            currentPrice = coin?.currentPrice ?: 0.0,
                            totalValue = portfolioItem.quantity * (coin?.currentPrice ?: 0.0),
                            profitLoss = (coin?.currentPrice ?: 0.0) * portfolioItem.quantity - 
                                       portfolioItem.purchasePrice * portfolioItem.quantity,
                            profitLossPercentage = ((coin?.currentPrice ?: 0.0) - portfolioItem.purchasePrice) / 
                                                 portfolioItem.purchasePrice * 100
                        )
                    }
                    PortfolioUiState.Success(
                        items = portfolioItems,
                        totalValue = portfolioItems.sumOf { it.totalValue },
                        totalProfitLoss = portfolioItems.sumOf { it.profitLoss }
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = PortfolioUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun addPortfolioItem(item: PortfolioItem) {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUser?.uid ?: return@launch
                userRepository.getUser(userId).collect { user ->
                    val updatedPortfolio = user.portfolio + item
                    userRepository.updatePortfolio(userId, updatedPortfolio)
                    loadPortfolio()
                }
            } catch (e: Exception) {
                _uiState.value = PortfolioUiState.Error(e.message ?: "Failed to add portfolio item")
            }
        }
    }
}

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(
        val items: List<PortfolioItemUi>,
        val totalValue: Double,
        val totalProfitLoss: Double
    ) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}

data class PortfolioItemUi(
    val coinId: String,
    val symbol: String,
    val name: String,
    val image: String,
    val quantity: Double,
    val currentPrice: Double,
    val totalValue: Double,
    val profitLoss: Double,
    val profitLossPercentage: Double
)
