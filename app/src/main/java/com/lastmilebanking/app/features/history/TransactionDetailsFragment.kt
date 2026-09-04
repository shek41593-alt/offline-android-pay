package com.lastmilebanking.app.features.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.lastmilebanking.app.R
import com.lastmilebanking.app.databinding.FragmentTransactionDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment() {

    private var _binding: FragmentTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionDetailsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val transactionId = arguments?.getString("transactionId") ?: return

        binding.btnRetry.setOnClickListener {
            viewModel.loadTransaction(transactionId)
        }

        viewModel.loadTransaction(transactionId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TransactionDetailUiState.Loading -> showLoading()
                        is TransactionDetailUiState.Success -> showSuccess(state)
                        is TransactionDetailUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
    }

    private fun showSuccess(state: TransactionDetailUiState.Success) {
        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE

        val detail = state.detail
        val isSent = state.isSent

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountValue = detail.amount ?: java.math.BigDecimal.ZERO
        val amountStr = currencyFormat.format(amountValue)

        if (detail.paymentMode == "WALLET_FUNDING") {
            binding.tvHeaderTitle.text = "Wallet Funded"
            binding.tvAmount.text = "+ $amountStr"
            binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))

            binding.tvCounterpartyLabel.text = "Source"
            binding.tvCounterpartyName.text = "Development Test Funding"
            binding.tvPaymentId.text = "-"
        } else if (isSent) {
            binding.tvHeaderTitle.text = "Payment Successful"
            binding.tvAmount.text = "- $amountStr"
            binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))

            binding.tvCounterpartyLabel.text = "Paid to"
            binding.tvCounterpartyName.text = detail.recipient?.name ?: "Unknown"
            binding.tvPaymentId.text = detail.recipient?.publicPaymentId ?: "Unknown"
        } else {
            binding.tvHeaderTitle.text = "Payment Received"
            binding.tvAmount.text = "+ $amountStr"
            binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))

            binding.tvCounterpartyLabel.text = "Received from"
            binding.tvCounterpartyName.text = detail.sender?.name ?: "Unknown"
            binding.tvPaymentId.text = detail.sender?.publicPaymentId ?: "Unknown"
        }

        val dateStr = detail.createdAt ?: ""
        var formattedDate = dateStr
        try {
            val instant = Instant.parse(dateStr)
            val date = Date.from(instant)
            formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(date)
        } catch (e: Exception) {
        }
        binding.tvDate.text = formattedDate
        binding.tvTransactionId.text = detail.reference ?: detail.transactionId ?: "Unknown"
        binding.tvStatus.text = detail.status ?: "UNKNOWN"
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
