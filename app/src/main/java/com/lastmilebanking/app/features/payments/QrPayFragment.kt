package com.lastmilebanking.app.features.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lastmilebanking.app.R

class QrPayFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_qr_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val qrImageView: ImageView = view.findViewById(R.id.ivQrCode)
        
        try {
            // Generating a secure offline payload QR using ZXing
            val barcodeEncoder = BarcodeEncoder()
            val offlinePayload = "LMB:OFFLINE_TXN:UID1234:SECURE_HASH:500.00"
            val bitmap = barcodeEncoder.encodeBitmap(offlinePayload, BarcodeFormat.QR_CODE, 600, 600)
            qrImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
