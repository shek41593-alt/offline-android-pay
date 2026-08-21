package com.lastmilebanking.app.features.payments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsPayFragment : Fragment() {

    private val viewModel: SmsPayViewModel by viewModels()
    private var paymentDialog: AlertDialog? = null
    
    // Store variables temporarilly for permission continuation
    private var pendingPhone: String? = null
    private var pendingAmount: Double? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                pendingPhone?.let { phone ->
                    pendingAmount?.let { amount ->
                        viewModel.prepareSmsPayment(phone, amount)
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "SMS permission is required to send payment",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
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

        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

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

            viewModel.verifySmsPayment(phone, amount)
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
                        is SmsPayState.Verify -> {
                            showConfirmationDialog(state.recipientPhone, state.amount)
                            // State remains verify until dismissed or confirmed
                        }
                        is SmsPayState.ReadyToSend -> {
                            sendSmsInternal(state.recipientPhone, state.amount, state.payload)
                        }
                        is SmsPayState.Success -> {
                            btnSendSms.isEnabled = true
                            btnSendSms.text = "Send Payment SMS"
                            showSuccessDialog(state.transactionId)
                            etPhone.text?.clear()
                            etAmount.text?.clear()
                            viewModel.resetState()
                        }
                        is SmsPayState.Error -> {
                            btnSendSms.isEnabled = true
                            btnSendSms.text = "Send Payment SMS"
                            showErrorDialog(state.message)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }
    
    private fun showConfirmationDialog(receiverId: String, amount: Double) {
        if (paymentDialog?.isShowing == true) return
        
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Confirm SMS Payment")
            .setMessage("Send SMS payment of $$amount to $receiverId?")
            .setCancelable(false)
            .setPositiveButton("Confirm Payment", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                viewModel.resetState()
            }
        
        paymentDialog = builder.create()
        paymentDialog?.setOnShowListener {
            val positiveButton = paymentDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.setOnClickListener {
                positiveButton.isEnabled = false
                positiveButton.text = "Processing..."
                
                if (hasSmsPermission()) {
                    viewModel.prepareSmsPayment(receiverId, amount)
                } else {
                    pendingPhone = receiverId
                    pendingAmount = amount
                    requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }
                
                paymentDialog?.dismiss()
            }
        }
        paymentDialog?.show()
    }

    private fun sendSmsInternal(phone: String, amount: Double, payload: String) {
        try {
            val smsManager = requireContext().getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, payload, null, null)
            
            // Assuming success because we didn't get an exception
            viewModel.completeSmsPayment(phone, amount, payload)
        } catch (e: SecurityException) {
            viewModel.abortSmsPayment("SMS permission denied")
        } catch (e: Exception) {
            viewModel.abortSmsPayment("Failed to send SMS: ${e.message}")
        }
    }

    private fun showSuccessDialog(transactionId: String) {
        paymentDialog?.dismiss()
        AlertDialog.Builder(requireContext())
            .setTitle("Payment SMS Sent")
            .setMessage("Transaction recorded.\nTransaction ID: $transactionId")
            .setCancelable(false)
            .setPositiveButton("DONE", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        paymentDialog?.dismiss()
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Failed")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }
}
