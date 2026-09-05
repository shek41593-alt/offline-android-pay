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

        val recipientContainer = view.findViewById<View>(R.id.recipientContainer)
        val tvRecipientName = view.findViewById<android.widget.TextView>(R.id.tvRecipientName)
        val tvRecipientPhone = view.findViewById<android.widget.TextView>(R.id.tvRecipientPhone)
        val tvRecipientPaymentId = view.findViewById<android.widget.TextView>(R.id.tvRecipientPaymentId)
        val btnContinue = view.findViewById<View>(R.id.btnContinue)

        val amountContainer = view.findViewById<View>(R.id.amountContainer)
        val tvAmountRecipientName = view.findViewById<android.widget.TextView>(R.id.tvAmountRecipientName)
        val tvAmountRecipientDetails = view.findViewById<android.widget.TextView>(R.id.tvAmountRecipientDetails)
        val etAmount = view.findViewById<android.widget.EditText>(R.id.etAmount)
        val tvAvailableBalance = view.findViewById<android.widget.TextView>(R.id.tvAvailableBalance)
        val btnSubmitAmount = view.findViewById<View>(R.id.btnSubmitAmount)

        val reviewContainer = view.findViewById<View>(R.id.reviewContainer)
        val tvReviewName = view.findViewById<android.widget.TextView>(R.id.tvReviewName)
        val tvReviewPaymentId = view.findViewById<android.widget.TextView>(R.id.tvReviewPaymentId)
        val tvReviewAmount = view.findViewById<android.widget.TextView>(R.id.tvReviewAmount)
        val btnConfirmPayment = view.findViewById<View>(R.id.btnConfirmPayment)

        val successContainer = view.findViewById<View>(R.id.successContainer)
        val tvSuccessTitle = view.findViewById<android.widget.TextView>(R.id.tvSuccessTitle)
        val tvSuccessSubtitle = view.findViewById<android.widget.TextView>(R.id.tvSuccessSubtitle)
        val tvSuccessAmount = view.findViewById<android.widget.TextView>(R.id.tvSuccessAmount)
        val tvSuccessName = view.findViewById<android.widget.TextView>(R.id.tvSuccessName)
        val tvSuccessTransactionId = view.findViewById<android.widget.TextView>(R.id.tvSuccessTransactionId)
        val btnDone = view.findViewById<View>(R.id.btnDone)

        btnContinue.setOnClickListener {
            viewModel.proceedToAmount()
        }

        btnSubmitAmount.setOnClickListener {
            val amountStr = etAmount.text.toString()
            val amount = amountStr.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            viewModel.submitAmount(amount)
        }

        var currentIdempotencyKey = ""
        btnConfirmPayment.setOnClickListener {
            btnConfirmPayment.isEnabled = false
            viewModel.confirmPayment(currentIdempotencyKey)
        }

        btnDone.setOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QrPayState.RecipientFound -> {
                            isProcessing = false
                            paymentDialog?.dismiss()
                            barcodeScannerView.pause()
                            
                            recipientContainer.visibility = View.VISIBLE
                            amountContainer.visibility = View.GONE
                            reviewContainer.visibility = View.GONE
                            successContainer.visibility = View.GONE
                            barcodeScannerView.visibility = View.GONE
                            
                            tvRecipientName.text = state.name
                            tvRecipientPhone.text = state.phone
                            tvRecipientPaymentId.text = state.publicPaymentId
                        }
                        is QrPayState.PaymentAmount -> {
                            recipientContainer.visibility = View.GONE
                            amountContainer.visibility = View.VISIBLE
                            reviewContainer.visibility = View.GONE
                            successContainer.visibility = View.GONE
                            
                            tvAmountRecipientName.text = state.name
                            tvAmountRecipientDetails.text = "${state.phone} • ${state.publicPaymentId}"
                            tvAvailableBalance.text = "Available Balance: ₹${state.balance}"
                        }
                        is QrPayState.PaymentReview -> {
                            recipientContainer.visibility = View.GONE
                            amountContainer.visibility = View.GONE
                            reviewContainer.visibility = View.VISIBLE
                            successContainer.visibility = View.GONE
                            
                            btnConfirmPayment.isEnabled = true
                            currentIdempotencyKey = state.idempotencyKey
                            
                            tvReviewName.text = state.name
                            tvReviewPaymentId.text = state.publicPaymentId
                            tvReviewAmount.text = "₹${state.amount}"
                        }
                        is QrPayState.OfflinePaymentReview -> {
                            paymentDialog?.dismiss()
                            recipientContainer.visibility = View.GONE
                            amountContainer.visibility = View.GONE
                            reviewContainer.visibility = View.VISIBLE
                            successContainer.visibility = View.GONE
                            barcodeScannerView.visibility = View.GONE
                            
                            btnConfirmPayment.isEnabled = true
                            currentIdempotencyKey = state.request.clientOperationId
                            
                            tvReviewName.text = state.merchantName
                            tvReviewPaymentId.text = state.request.merchantId
                            tvReviewAmount.text = "₹${state.request.amount}"
                        }
                        is QrPayState.Success -> {
                            paymentDialog?.dismiss()
                            recipientContainer.visibility = View.GONE
                            amountContainer.visibility = View.GONE
                            reviewContainer.visibility = View.GONE
                            successContainer.visibility = View.VISIBLE
                            
                            if (state.isOffline) {
                                tvSuccessTitle.text = "Payment Recorded"
                                tvSuccessTitle.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                                tvSuccessSubtitle.visibility = View.VISIBLE
                            } else {
                                tvSuccessTitle.text = "Payment Successful"
                                tvSuccessSubtitle.visibility = View.GONE
                            }
                            
                            tvSuccessName.text = state.name
                            tvSuccessAmount.text = "₹${state.amount}"
                            tvSuccessTransactionId.text = state.transactionId
                        }
                        is QrPayState.Error -> {
                            isProcessing = false
                            paymentDialog?.dismiss()
                            btnConfirmPayment.isEnabled = true
                            
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            
                            // If we were on amount or review screen, we stay there so they can fix it.
                            if (amountContainer.visibility != View.VISIBLE && reviewContainer.visibility != View.VISIBLE) {
                                recipientContainer.visibility = View.GONE
                                barcodeScannerView.resume()
                                barcodeScannerView.visibility = View.VISIBLE
                                viewModel.resetState()
                            } else {
                                // If error occurred during payment api, we reset to amount so they can retry smoothly
                                viewModel.resetState()
                            }
                        }
                        is QrPayState.Loading -> {
                            // Show loading dialog
                            if (paymentDialog == null || paymentDialog?.isShowing == false) {
                                paymentDialog = AlertDialog.Builder(requireContext())
                                    .setTitle("Resolving recipient...")
                                    .setMessage("Please wait")
                                    .setCancelable(false)
                                    .show()
                            }
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

    // Dialog methods removed for Phase 3
}
