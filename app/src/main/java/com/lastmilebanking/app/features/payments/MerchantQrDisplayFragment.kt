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
import com.lastmilebanking.app.R

class MerchantQrDisplayFragment : Fragment(R.layout.fragment_merchant_qr_display) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val canonicalPayload = arguments?.getString("canonicalPayload") ?: ""
        val amount = arguments?.getString("amount") ?: "0.00"
        val merchantName = arguments?.getString("merchantName") ?: "Merchant"

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val tvAmount = view.findViewById<TextView>(R.id.tvAmount)
        val tvMerchantName = view.findViewById<TextView>(R.id.tvMerchantName)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val ivQrCode = view.findViewById<ImageView>(R.id.ivQrCode)
        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        btnCancel.setOnClickListener {
            // Cancel does not mutate financial state
            findNavController().navigateUp()
        }

        tvAmount.text = "₹$amount"
        tvMerchantName.text = merchantName
        tvStatus.text = "Waiting for customer"

        try {
            val bmp = generateQrCode(canonicalPayload)
            ivQrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Error generating QR Code."
        }
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
