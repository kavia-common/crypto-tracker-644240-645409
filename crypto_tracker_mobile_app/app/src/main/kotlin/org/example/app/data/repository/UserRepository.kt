package org.example.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.example.app.data.model.User
import org.example.app.data.model.PortfolioItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Flow<FirebaseUser> = flow {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { emit(it) }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun signUp(email: String, password: String): Flow<FirebaseUser> = flow {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { emit(it) }
        } catch (e: Exception) {
            throw e
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getUser(userId: String): Flow<User> = flow {
        try {
            // In a real app, we would fetch user data from Firestore or another database
            val user = User(
                id = userId,
                email = currentUser?.email ?: "",
                watchlist = emptyList(),
                portfolio = emptyList()
            )
            emit(user)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateWatchlist(userId: String, coins: List<String>): Flow<Boolean> = flow {
        try {
            // In a real app, we would update the watchlist in Firestore or another database
            emit(true)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePortfolio(userId: String, portfolio: List<PortfolioItem>): Flow<Boolean> = flow {
        try {
            // In a real app, we would update the portfolio in Firestore or another database
            emit(true)
        } catch (e: Exception) {
            throw e
        }
    }
}
