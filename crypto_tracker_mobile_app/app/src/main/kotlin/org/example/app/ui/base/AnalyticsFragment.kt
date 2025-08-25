package org.example.app.ui.base

import androidx.fragment.app.Fragment
import org.example.app.analytics.AnalyticsManager
import javax.inject.Inject

abstract class AnalyticsFragment : Fragment() {
    
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onResume() {
        super.onResume()
        logScreenView()
    }

    private fun logScreenView() {
        analyticsManager.logScreenView(
            screenName = getScreenName(),
            screenClass = this::class.java.simpleName
        )
    }

    abstract fun getScreenName(): String
}
