package com.lastmilebanking.app.features.payments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle

@AndroidEntryPoint
class MerchantReceivePaymentFragment : Fragment(R.layout.fragment_merchant_receive_payment) {

    private val viewModel: MerchantQrViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val btnGenerateQr = view.findViewById<MaterialButton>(R.id.btnGenerateQr)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        btnGenerateQr.setOnClickListener {
            val amountStr = etAmount.text.toString()
            viewModel.generatePaymentRequest(amountStr)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MerchantQrState.Error -> {
                            tvError.visibility = View.VISIBLE
                            tvError.text = state.message
                        }
                        is MerchantQrState.QrReady -> {
                            tvError.visibility = View.GONE
                            val bundle = Bundle().apply {
                                putString("canonicalPayload", state.canonicalPayload)
                                putString("amount", state.amount)
                                putString("merchantName", state.merchantName)
                            }
                            // Navigate to Display QR fragment, handling one-off nav correctly
                            findNavController().navigate(R.id.action_receive_to_display, bundle)
                            // Reset state internally so back navigation doesn't instantly retrigger
                            viewModel.resetState()
                        }
                        else -> {
                            tvError.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}
