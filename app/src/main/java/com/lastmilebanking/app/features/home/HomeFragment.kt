package com.lastmilebanking.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.lastmilebanking.app.R
import com.lastmilebanking.app.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnQrPay.setOnClickListener {
            androidx.navigation.Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_qr)
        }
        binding.btnBluetoothPay.setOnClickListener {
            androidx.navigation.Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_bluetooth)
        }
        binding.btnSmsPay.setOnClickListener {
            androidx.navigation.Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_sms)
        }
        binding.btnToggleBalance.setOnClickListener {
            viewModel.toggleBalanceVisibility()
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is HomeUiState.Loading -> showLoading()
                            is HomeUiState.Success -> showSuccess(state)
                            is HomeUiState.Error -> showError(state.message)
                        }
                    }
                }
                launch {
                    viewModel.isBalanceVisible.collect { visible ->
                        updateBalanceVisibility(visible)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showSuccess(state: HomeUiState.Success) {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false

        binding.tvUserName.text = "Hello, ${state.userName.split(" ").first()} 👋"
        binding.tvBalance.text = currencyFormat.format(state.wallet.availableBalance)
        binding.tvOfflineBalance.text = "Offline: ${currencyFormat.format(state.wallet.offlineBalance)}"

        // Pending sync badge
        if (state.pendingSyncCount > 0) {
            binding.tvPendingSync.visibility = View.VISIBLE
            binding.tvPendingSync.text = "${state.pendingSyncCount} pending sync"
        } else {
            binding.tvPendingSync.visibility = View.GONE
        }

        // Offline indicator
        binding.ivOfflineIndicator.visibility = if (state.isOffline) View.VISIBLE else View.GONE

        // Transactions
        if (state.recentTransactions.isEmpty()) {
            binding.tvNoTransactions.visibility = View.VISIBLE
            binding.rvTransactions.visibility = View.GONE
        } else {
            binding.tvNoTransactions.visibility = View.GONE
            binding.rvTransactions.visibility = View.VISIBLE
            transactionAdapter.submitList(state.recentTransactions)
        }
    }

    private fun showError(message: String) {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
        binding.tvUserName.text = "Error loading dashboard"
    }

    private fun updateBalanceVisibility(visible: Boolean) {
        val currentState = viewModel.uiState.value
        if (currentState is HomeUiState.Success) {
            binding.tvBalance.text = if (visible) {
                currencyFormat.format(currentState.wallet.availableBalance)
            } else {
                "₹ ••••••"
            }
            binding.btnToggleBalance.setImageResource(
                if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
