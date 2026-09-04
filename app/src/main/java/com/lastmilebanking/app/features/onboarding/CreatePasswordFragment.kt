package com.lastmilebanking.app.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import com.lastmilebanking.app.features.authentication.AuthViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class CreatePasswordFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvTitle)
        val tvSubtitle = view.findViewById<android.widget.TextView>(R.id.tvSubtitle)
        val tilConfirmPassword = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilConfirmPassword)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnContinue = view.findViewById<MaterialButton>(R.id.btnContinue)

        if (viewModel.isLoginFlow) {
            tvTitle.text = "Enter your password"
            tvSubtitle.text = "Please enter your registered password"
            tilConfirmPassword.visibility = View.GONE
        }

        btnContinue.setOnClickListener {
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!viewModel.isLoginFlow) {
                if (password.length < 8) {
                    Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (password != confirm) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            viewModel.setPassword(password)
            
            if (viewModel.isLoginFlow) {
                viewModel.loginOnly()
            } else {
                findNavController().navigate(R.id.action_password_to_personal_info)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is com.lastmilebanking.app.features.authentication.LoginState.ExistingUserAuthenticated -> {
                            val navController = findNavController()
                            if (navController.currentDestination?.id == R.id.createPasswordFragment) {
                                navController.navigate(R.id.action_password_to_home)
                                viewModel.resetState()
                            }
                        }
                        is com.lastmilebanking.app.features.authentication.LoginState.Loading -> {
                            btnContinue.isEnabled = false
                            btnContinue.text = "Authenticating..."
                        }
                        is com.lastmilebanking.app.features.authentication.LoginState.Error -> {
                            btnContinue.isEnabled = true
                            btnContinue.text = "CONTINUE"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        else -> {
                            btnContinue.isEnabled = true
                            btnContinue.text = "CONTINUE"
                        }
                    }
                }
            }
        }
    }
}
