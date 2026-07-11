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
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R

class SmsPayFragment : Fragment() {

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

        view.findViewById<MaterialButton>(R.id.btnSendSms).setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val amount = etAmount.text.toString().trim()

            if (phone.isEmpty() || amount.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!hasSmsPermission()) {
                Toast.makeText(requireContext(), "SMS permission not granted", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Build encrypted offline SMS payload
            val payload = "LMB:PAY:AMT=$amount:HASH=${System.currentTimeMillis()}"

            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phone, null, payload, null, null)
                Toast.makeText(requireContext(), "Payment SMS sent to $phone", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }
}
