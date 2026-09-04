package com.lastmilebanking.app.features.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyQrFragment : Fragment(R.layout.fragment_my_qr) {

    private val viewModel: MyQrViewModel by viewModels()
    private var currentQrPayload: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val cardQr = view.findViewById<MaterialCardView>(R.id.cardQr)
        val llLoading = view.findViewById<LinearLayout>(R.id.llLoading)
        val llError = view.findViewById<LinearLayout>(R.id.llError)
        val ivQrCode = view.findViewById<ImageView>(R.id.ivQrCode)
        val tvPaymentId = view.findViewById<TextView>(R.id.tvPaymentId)
        val btnRetry = view.findViewById<MaterialButton>(R.id.btnRetry)
        val btnShare = view.findViewById<MaterialButton>(R.id.btnShare)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        btnRetry.setOnClickListener {
            viewModel.loadQrData()
        }

        btnShare.setOnClickListener {
            currentQrPayload?.let { payload ->
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, payload)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Payment QR")
                startActivity(shareIntent)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MyQrUiState.Loading -> {
                            llLoading.visibility = View.VISIBLE
                            cardQr.visibility = View.GONE
                            llError.visibility = View.GONE
                            btnShare.isEnabled = false
                        }
                        is MyQrUiState.Success -> {
                            llLoading.visibility = View.GONE
                            cardQr.visibility = View.VISIBLE
                            llError.visibility = View.GONE
                            btnShare.isEnabled = true

                            tvPaymentId.text = state.paymentId
                            currentQrPayload = state.qrContent
                            
                            // Generate QR internal rendering
                            val bmp = generateQrCode(state.qrContent)
                            if (bmp != null) {
                                ivQrCode.setImageBitmap(bmp)
                            }
                        }
                        is MyQrUiState.Error -> {
                            llLoading.visibility = View.GONE
                            cardQr.visibility = View.GONE
                            llError.visibility = View.VISIBLE
                            btnShare.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun generateQrCode(content: String): Bitmap? {
        return try {
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
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
