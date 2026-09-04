package com.lastmilebanking.app.features.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lastmilebanking.app.R
import com.lastmilebanking.app.databinding.FragmentHistoryBinding
import com.lastmilebanking.app.features.home.TransactionAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionAdapter = TransactionAdapter { transactionId ->
            val bundle = android.os.Bundle().apply { putString("transactionId", transactionId) }
            findNavController().navigate(R.id.action_history_to_transactionDetails, bundle)
        }

        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HistoryUiState.Loading -> {
                            binding.rvTransactions.visibility = View.GONE
                            binding.tvNoTransactions.visibility = View.GONE
                        }
                        is HistoryUiState.Success -> {
                            if (state.transactions.isEmpty()) {
                                binding.rvTransactions.visibility = View.GONE
                                binding.tvNoTransactions.visibility = View.VISIBLE
                            } else {
                                binding.rvTransactions.visibility = View.VISIBLE
                                binding.tvNoTransactions.visibility = View.GONE
                                transactionAdapter.submitList(state.transactions)
                            }
                        }
                        is HistoryUiState.Error -> {
                            binding.rvTransactions.visibility = View.GONE
                            binding.tvNoTransactions.visibility = View.VISIBLE
                            binding.tvNoTransactions.text = state.message
                        }
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
