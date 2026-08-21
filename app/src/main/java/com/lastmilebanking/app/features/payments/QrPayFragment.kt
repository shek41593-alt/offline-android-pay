package com.lastmilebanking.app.features.payments

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QrPayFragment : Fragment() {

    private val viewModel: QrPayViewModel by viewModels()

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            viewModel.onScanResult(result.contents)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchScanner()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_qr_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Retain generating the offline dummy QR for demonstration, but allow real scanning
        val qrImageView: ImageView = view.findViewById(R.id.ivQrCode)
        try {
            val barcodeEncoder = com.journeyapps.barcodescanner.BarcodeEncoder()
            val offlinePayload = "LMB:OFFLINE_TXN:UID1234:SECURE_HASH:500.00"
            val bitmap = barcodeEncoder.encodeBitmap(offlinePayload, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600)
            qrImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        view.findViewById<MaterialButton>(R.id.btnScanQr).setOnClickListener {
            // First check camera permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QrPayState.Verify -> {
                            showConfirmationDialog(state.receiverId, state.amount)
                            viewModel.resetState()
                        }
                        is QrPayState.Success -> {
                            Toast.makeText(requireContext(), "Transaction Successful! ID: ${state.transactionId}", Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        is QrPayState.Error -> {
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        is QrPayState.Loading -> {
                            // Show loader if needed
                        }
                        else -> { }
                    }
                }
            }
        }
    }

    private fun launchScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a Merchant QR Code")
        options.setCameraId(0) // Use a specific camera of the device
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(false)
        barcodeLauncher.launch(options)
    }

    private fun showConfirmationDialog(receiverId: String, amount: Double) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Payment")
            .setMessage("Pay $$amount to recipient $receiverId?")
            .setPositiveButton("Confirm Payment") { dialog, _ ->
                viewModel.processQrPayment(receiverId, amount)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
