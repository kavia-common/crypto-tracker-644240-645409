package org.example.app

import android.app.Application
import com.google.firebase.FirebaseApp

class CryptoTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
