package org.example.app.data.model

data class User(
    val id: String,
    val email: String,
    var watchlist: List<String> = emptyList(),
    var portfolio: List<PortfolioItem> = emptyList()
)

data class PortfolioItem(
    val coinId: String,
    val quantity: Double,
    val purchasePrice: Double,
    val purchaseDate: Long
)
