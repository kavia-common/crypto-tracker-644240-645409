package org.example.app.feedback

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    private val context: Context
) {
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
    private var cachedReviewInfo: ReviewInfo? = null

    init {
        preloadReviewFlow()
    }

    private fun preloadReviewFlow() {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cachedReviewInfo = task.result
            }
        }
    }

    fun showFeedbackDialog(activity: Activity) {
        val cachedInfo = cachedReviewInfo
        if (cachedInfo != null) {
            launchReviewFlow(activity, cachedInfo)
        } else {
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    launchReviewFlow(activity, task.result)
                }
            }
        }
    }

    private fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo) {
        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
        flow.addOnCompleteListener { _ ->
            // Review flow completed
        }
    }
}
