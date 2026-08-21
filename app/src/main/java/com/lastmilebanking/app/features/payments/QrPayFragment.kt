package com.lastmilebanking.app.features.payments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QrPayFragment : Fragment() {

    private val viewModel: QrPayViewModel by viewModels()
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private var isProcessing = false
    private var paymentDialog: AlertDialog? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                barcodeScannerView.resume()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
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
        
        barcodeScannerView = view.findViewById(R.id.barcodeScannerView)
        
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            findNavController().popBackStack()
        }

        checkCameraPermission()

        barcodeScannerView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result?.text != null && !isProcessing) {
                    isProcessing = true
                    barcodeScannerView.pause()
                    viewModel.onScanResult(result.text)
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QrPayState.Verify -> {
                            showConfirmationDialog(state.receiverId, state.amount)
                            viewModel.resetState()
                        }
                        is QrPayState.Success -> {
                            isProcessing = false
                            showSuccessDialog(state.transactionId)
                            viewModel.resetState()
                        }
                        is QrPayState.Error -> {
                            isProcessing = false
                            showErrorDialog(state.message)
                            viewModel.resetState()
                        }
                        is QrPayState.Loading -> {
                            // Handled by disabling button in the dialog
                        }
                        else -> { }
                    }
                }
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeScannerView.pause()
    }

    private fun showConfirmationDialog(receiverId: String, amount: Double) {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Payment")
            .setMessage("Pay $$amount to recipient $receiverId?")
            .setCancelable(false)
            .setPositiveButton("Confirm Payment", null) // Set to null first to override click block
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                isProcessing = false
                barcodeScannerView.resume()
            }
        
        paymentDialog = builder.create()
        paymentDialog?.setOnShowListener {
            val positiveButton = paymentDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.setOnClickListener {
                positiveButton.isEnabled = false // prevent double click
                positiveButton.text = "Processing..."
                viewModel.processQrPayment(receiverId, amount)
            }
        }
        paymentDialog?.show()
    }

    private fun showSuccessDialog(transactionId: String) {
        paymentDialog?.dismiss()
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Successful")
            .setMessage("Transaction completed.\nTransaction ID: $transactionId")
            .setCancelable(false)
            .setPositiveButton("DONE") { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        paymentDialog?.dismiss()
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Failed")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("TRY AGAIN") { _, _ ->
                isProcessing = false
                barcodeScannerView.resume()
            }
            .setNegativeButton("BACK") { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }
}
