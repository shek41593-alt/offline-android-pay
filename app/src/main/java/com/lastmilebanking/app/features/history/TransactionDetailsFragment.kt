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
        val amountValue = detail.amount
        val amountStr = currencyFormat.format(amountValue)

        if (detail.paymentMode == "WALLET_FUNDING") {
            binding.tvHeaderTitle.text = "Wallet Funded"
            binding.tvAmount.text = "+ $amountStr"
            binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))

            binding.tvCounterpartyLabel.text = "Source"
            binding.tvCounterpartyName.text = "Development Test Funding"
            binding.tvPaymentId.text = "-"
        } else if (isSent) {
            val title = if (detail.status == "SETTLED" || detail.status == "COMPLETED") "Payment Successful" else "Payment recorded offline"
            binding.tvHeaderTitle.text = title
            binding.tvAmount.text = "- $amountStr"
            binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))

            binding.tvCounterpartyLabel.text = "Paid to"
            binding.tvCounterpartyName.text = detail.receiverName.ifBlank { "Unknown" }
            binding.tvPaymentId.text = detail.receiverId.ifBlank { "Unknown" }
        } else {
            val title = if (detail.status == "SETTLED" || detail.status == "COMPLETED") "Payment Received" else "Payment recorded offline"
            binding.tvHeaderTitle.text = title
            binding.tvAmount.text = "+ $amountStr"
            binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))

            binding.tvCounterpartyLabel.text = "Received from"
            binding.tvCounterpartyName.text = "Unknown"
            binding.tvPaymentId.text = detail.senderId.ifBlank { "Unknown" }
        }

        val dateStr = detail.createdAt.toString()
        var formattedDate = dateStr
        try {
            val date = Date(detail.createdAt)
            formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(date)
        } catch (e: Exception) {
        }
        binding.tvDate.text = formattedDate
        binding.tvTransactionId.text = detail.transactionHash.ifBlank { detail.transactionId }
        
        val statusText = when (detail.status) {
            "PENDING_SYNC" -> "Waiting for synchronization"
            "SYNCING" -> "Synchronizing"
            "SETTLED", "SYNCED" -> "Payment settled"
            "CONFLICT" -> "Payment requires attention"
            "ACTION_REQUIRED" -> "Action required"
            "FAILED" -> "Failed"
            else -> detail.status
        }
        binding.tvStatus.text = statusText
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
