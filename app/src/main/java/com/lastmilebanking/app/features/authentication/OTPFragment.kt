package com.lastmilebanking.app.features.authentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import kotlinx.coroutines.launch

class OTPFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels() // Linked to LoginFragment's instance
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_otp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        
        view.findViewById<TextView>(R.id.tvPhoneNumber).text = viewModel.phoneNumber.value

        view.findViewById<TextView>(R.id.btnChangeNumber).setOnClickListener {
            findNavController().popBackStack()
        }

        val etOtp = view.findViewById<TextInputEditText>(R.id.etOtp)
        val btnVerifyOtp = view.findViewById<MaterialButton>(R.id.btnVerifyOtp)
        val btnResendOtp = view.findViewById<TextView>(R.id.btnResendOtp)

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.length < 6) { // Appwrite usually uses 6 digit code
                Toast.makeText(requireContext(), "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.verifyOtp(otp)
        }

        btnResendOtp.setOnClickListener {
            etOtp.text?.clear()
            viewModel.requestOtp()
            Toast.makeText(requireContext(), "OTP Resent", Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginState.Loading -> {
                            btnVerifyOtp.isEnabled = false
                            btnVerifyOtp.text = "Verifying..."
                        }
                        is LoginState.VerifiedContinue -> {
                            btnVerifyOtp.isEnabled = true
                            btnVerifyOtp.text = "VERIFY OTP"
                            viewModel.resetState()
                            findNavController().navigate(R.id.action_otp_to_create_password)
                        }
                        is LoginState.RequiresPassword -> {
                            btnVerifyOtp.isEnabled = true
                            btnVerifyOtp.text = "VERIFY OTP"
                            viewModel.resetState()
                            // If sign in flows require password, go to password screen
                            findNavController().navigate(R.id.action_otp_to_create_password)
                        }
                        is LoginState.Success -> {
                            btnVerifyOtp.isEnabled = true
                            btnVerifyOtp.text = "VERIFY OTP"
                            viewModel.resetState()
                            findNavController().navigate(R.id.action_otp_to_home)
                        }
                        is LoginState.Error -> {
                            btnVerifyOtp.isEnabled = true
                            btnVerifyOtp.text = "VERIFY OTP"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
