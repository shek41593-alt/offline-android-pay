package com.lastmilebanking.app.features.onboarding

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
import com.lastmilebanking.app.R
import com.lastmilebanking.app.features.authentication.AuthViewModel
import com.lastmilebanking.app.features.authentication.LoginState
import kotlinx.coroutines.launch

class SyncProgressFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sync_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Give the UI a moment before triggering
        view.postDelayed({
            viewModel.createAccountAndSync()
        }, 1000)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginState.Success -> {
                            findNavController().navigate(R.id.action_sync_to_home)
                            viewModel.resetState()
                        }
                        is LoginState.Error -> {
                            Toast.makeText(requireContext(), "Registration Failed: ${state.message}", Toast.LENGTH_LONG).show()
                            findNavController().popBackStack()
                            viewModel.resetState()
                        }
                        else -> {
                            // Loading UI is active by default
                        }
                    }
                }
            }
        }
    }
}
