package com.lastmilebanking.app.features.authentication

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels() // Share with OTP fragment
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLogin = arguments?.getBoolean("isLogin", true) ?: true
        viewModel.isLoginFlow = isLogin

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        
        view.findViewById<android.widget.TextView>(R.id.tvTitle).text = if (isLogin) "Sign In" else "Create Account"

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etPhone)
        val btnSendOtp = view.findViewById<MaterialButton>(R.id.btnSendOtp)

        btnSendOtp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Valid email required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.length != 10) {
                Toast.makeText(requireContext(), "Enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val e164Phone = "+91$phone"
            viewModel.setPhoneNumberAndEmail(e164Phone, email)
            viewModel.requestOtp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginState.Loading -> {
                            btnSendOtp.isEnabled = false
                            btnSendOtp.text = "Sending..."
                        }
                        is LoginState.OtpSent -> {
                            btnSendOtp.isEnabled = true
                            btnSendOtp.text = "SEND OTP"
                            viewModel.resetState()
                            findNavController().navigate(R.id.action_login_to_otp)
                        }
                        is LoginState.Error -> {
                            btnSendOtp.isEnabled = true
                            btnSendOtp.text = "SEND OTP"
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
