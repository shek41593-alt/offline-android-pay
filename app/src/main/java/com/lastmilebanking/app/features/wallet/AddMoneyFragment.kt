package com.lastmilebanking.app.features.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.lastmilebanking.app.databinding.FragmentAddMoneyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class AddMoneyFragment : Fragment() {

    private var _binding: FragmentAddMoneyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMoneyViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddMoneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnContinue.setOnClickListener {
            val state = viewModel.uiState.value
            when (state) {
                is AddMoneyUiState.Input -> {
                    viewModel.submitAmount(binding.etAmount.text.toString())
                }
                is AddMoneyUiState.Review -> {
                    viewModel.confirmFunding()
                }
                is AddMoneyUiState.Success -> {
                    findNavController().navigateUp()
                }
                is AddMoneyUiState.Error -> {
                    viewModel.resetState()
                }
                else -> {}
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: AddMoneyUiState) {
        // Hide all major areas first
        binding.llInput.visibility = View.GONE
        binding.llReview.visibility = View.GONE
        binding.llSuccess.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.btnContinue.isEnabled = true

        when (state) {
            is AddMoneyUiState.Input -> {
                binding.llInput.visibility = View.VISIBLE
                binding.btnContinue.text = "Continue"
            }
            is AddMoneyUiState.Review -> {
                binding.llReview.visibility = View.VISIBLE
                binding.tvReviewAmount.text = currencyFormat.format(state.amount)
                binding.btnContinue.text = "Confirm"
            }
            is AddMoneyUiState.Loading -> {
                binding.llReview.visibility = View.VISIBLE
                binding.progressBar.visibility = View.VISIBLE
                binding.btnContinue.isEnabled = false
                binding.btnContinue.text = "Processing..."
            }
            is AddMoneyUiState.Success -> {
                binding.llSuccess.visibility = View.VISIBLE
                binding.tvSuccessAmount.text = "+ ${currencyFormat.format(state.amount)}"
                binding.tvSuccessBalance.text = currencyFormat.format(state.newBalance)
                binding.btnContinue.text = "Done"
            }
            is AddMoneyUiState.Error -> {
                binding.llInput.visibility = View.VISIBLE
                binding.btnContinue.text = "Retry"
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
