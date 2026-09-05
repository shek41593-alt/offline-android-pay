package com.lastmilebanking.app.features.payments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.google.gson.Gson
import com.lastmilebanking.app.R

class MerchantQrDisplayFragment : Fragment(R.layout.fragment_merchant_qr_display) {

    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private var isScanning = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                barcodeScannerView.resume()
                isScanning = true
            } else {
                Toast.makeText(requireContext(), "Camera permission required to scan proof", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val canonicalPayloadString = arguments?.getString("canonicalPayload") ?: ""
        val originalReq = try { Gson().fromJson(canonicalPayloadString, com.lastmilebanking.app.domain.payment.qr.OfflineQrPaymentRequest::class.java) } catch (e: Exception) { null }

        val amount = arguments?.getString("amount") ?: "0.00"
        val merchantName = arguments?.getString("merchantName") ?: "Merchant"

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val tvAmount = view.findViewById<TextView>(R.id.tvAmount)
        val tvMerchantName = view.findViewById<TextView>(R.id.tvMerchantName)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val ivQrCode = view.findViewById<ImageView>(R.id.ivQrCode)
        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val tvTransactionId = view.findViewById<TextView>(R.id.tvTransactionId)
        val btnScanProof = view.findViewById<View>(R.id.btnScanProof)
        val mainContainer = view.findViewById<View>(R.id.mainContainer)
        barcodeScannerView = view.findViewById(R.id.barcodeScannerView)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        tvAmount.text = "₹$amount"
        tvMerchantName.text = merchantName
        tvStatus.text = "Waiting for customer"

        try {
            val bmp = generateQrCode(canonicalPayloadString)
            ivQrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Error generating QR Code."
        }
        
        btnScanProof.setOnClickListener {
            mainContainer.visibility = View.GONE
            barcodeScannerView.visibility = View.VISIBLE
            btnScanProof.visibility = View.GONE
            checkCameraPermission()
        }
        
        barcodeScannerView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result?.text != null && isScanning) {
                    isScanning = false
                    barcodeScannerView.pause()
                    
                    val parseResult = com.lastmilebanking.app.domain.payment.qr.OfflineQrPaymentProofParser().parse(result.text)
                    if (parseResult.isSuccess) {
                        val proof = parseResult.getOrNull()!!
                        
                        val verifier = com.lastmilebanking.app.domain.payment.qr.AndroidKeystoreQrVerifier { 
                            // Dummy fallback
                            com.lastmilebanking.app.domain.payment.qr.AndroidKeystoreQrSigner("LMB_CUSTOMER_PROOF_KEY").getPublicKey()
                        }
                        
                        val validator = com.lastmilebanking.app.domain.payment.qr.OfflineQrPaymentProofValidator(verifier, originalReq)
                        val valRes = validator.validate(proof)
                        
                        if (valRes is com.lastmilebanking.app.domain.payment.qr.QrValidationResult.Valid) {
                            barcodeScannerView.visibility = View.GONE
                            mainContainer.visibility = View.VISIBLE
                            
                            tvStatus.text = "✓ Payment proof verified\nWaiting for synchronization"
                            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                            tvTransactionId.visibility = View.VISIBLE
                            tvTransactionId.text = "TXN: ${proof.transactionId}"
                            ivQrCode.visibility = View.GONE
                        } else {
                            Toast.makeText(requireContext(), "Proof Validation Failed: ${(valRes as com.lastmilebanking.app.domain.payment.qr.QrValidationResult.Invalid).reason}", Toast.LENGTH_LONG).show()
                            isScanning = true
                            barcodeScannerView.resume()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Invalid Payment Proof QR", Toast.LENGTH_SHORT).show()
                        isScanning = true
                        barcodeScannerView.resume()
                    }
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume()
            isScanning = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    override fun onPause() {
        super.onPause()
        barcodeScannerView.pause()
    }

    private fun generateQrCode(content: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
