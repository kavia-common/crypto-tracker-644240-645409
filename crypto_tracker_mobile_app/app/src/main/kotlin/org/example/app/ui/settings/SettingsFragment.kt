package org.example.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.example.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCurrencySpinner()
        setupListeners()
        observeUiState()
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("USD", "EUR", "GBP", "JPY")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.currencySpinner.adapter = adapter
    }

    private fun setupListeners() {
        binding.signOutButton.setOnClickListener {
            viewModel.signOut()
        }

        binding.priceAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePriceAlerts(isChecked)
        }

        binding.newsAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNewsAlerts(isChecked)
        }

        binding.currencySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currency = parent?.getItemAtPosition(position) as String
                viewModel.updateCurrency(currency)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is SettingsUiState.Loading -> {
                        // Show loading state
                    }
                    is SettingsUiState.Success -> {
                        binding.emailText.text = state.email
                        binding.priceAlertSwitch.isChecked = state.priceAlertsEnabled
                        binding.newsAlertSwitch.isChecked = state.newsAlertsEnabled
                        val currencyPosition = (binding.currencySpinner.adapter as ArrayAdapter<String>)
                            .getPosition(state.selectedCurrency)
                        binding.currencySpinner.setSelection(currencyPosition)
                    }
                    is SettingsUiState.SignedOut -> {
                        // Navigate to login screen
                        findNavController().navigate(/* TODO: Add navigation action */)
                    }
                    is SettingsUiState.Error -> {
                        // Show error message
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
