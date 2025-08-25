package org.example.app.feedback

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.example.app.analytics.AnalyticsManager
import org.example.app.databinding.DialogFeedbackBinding
import javax.inject.Inject

class FeedbackDialog : DialogFragment() {

    private var _binding: DialogFeedbackBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var feedbackManager: FeedbackManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.submitButton.setOnClickListener {
            val rating = binding.ratingBar.rating
            val feedback = binding.feedbackInput.editText?.text.toString()
            
            analyticsManager.logEvent("feedback_submitted", mapOf(
                "rating" to rating.toString(),
                "has_comment" to (feedback.isNotEmpty()).toString()
            ))

            if (rating >= 4) {
                feedbackManager.showFeedbackDialog(requireActivity())
            }
            
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            analyticsManager.logEvent("feedback_cancelled", emptyMap())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FeedbackDialog"
        
        fun newInstance() = FeedbackDialog()
    }
}
