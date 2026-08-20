package com.lastmilebanking.app.features.payments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsPayFragment : Fragment() {

    private val viewModel: SmsPayViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, trigger the action if fields are filled
                val phone = view?.findViewById<TextInputEditText>(R.id.etRecipientPhone)?.text?.toString()?.trim() ?: ""
                val amountStr = view?.findViewById<TextInputEditText>(R.id.etAmount)?.text?.toString()?.trim() ?: ""
                if (phone.isNotEmpty() && amountStr.isNotEmpty()) {
                    amountStr.toDoubleOrNull()?.let { amount ->
                        if (amount > 0) {
                            viewModel.processSmsPayment(phone, amount)
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Permission is required to send payment SMS", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sms_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etPhone = view.findViewById<TextInputEditText>(R.id.etRecipientPhone)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val btnSendSms = view.findViewById<MaterialButton>(R.id.btnSendSms)

        btnSendSms.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()

            if (phone.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!hasSmsPermission()) {
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                return@setOnClickListener
            }

            // Let ViewModel process the business logic (validation, transaction, wallet)
            viewModel.processSmsPayment(phone, amount)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SmsPayState.Idle -> {
                            btnSendSms.isEnabled = true
                            btnSendSms.text = "Send Payment SMS"
                        }
                        is SmsPayState.Loading -> {
                            btnSendSms.isEnabled = false
                            btnSendSms.text = "Processing..."
                        }
                        is SmsPayState.Success -> {
                            btnSendSms.isEnabled = true
                            btnSendSms.text = "Send Payment SMS"
                            
                            val phone = etPhone.text.toString().trim()
                            sendSmsInternal(phone, state.payload)
                            
                            // Reset state for MVP
                            etPhone.text?.clear()
                            etAmount.text?.clear()
                        }
                        is SmsPayState.Error -> {
                            btnSendSms.isEnabled = true
                            btnSendSms.text = "Send Payment SMS"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun sendSmsInternal(phone: String, payload: String) {
        try {
            val smsManager = requireContext().getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, payload, null, null)
            Toast.makeText(requireContext(), "Payment SMS sent to $phone", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }
}
