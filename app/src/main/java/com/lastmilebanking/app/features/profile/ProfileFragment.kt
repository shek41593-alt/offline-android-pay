package com.lastmilebanking.app.features.profile

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        val tvUserId = view.findViewById<TextView>(R.id.tvUserId)
        val cvDetails = view.findViewById<MaterialCardView>(R.id.cvDetails)
        val llLoading = view.findViewById<LinearLayout>(R.id.llLoading)
        val llError = view.findViewById<LinearLayout>(R.id.llError)
        val btnRetry = view.findViewById<MaterialButton>(R.id.btnRetry)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        btnRetry.setOnClickListener {
            viewModel.loadProfile()
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("LOG OUT") { _, _ ->
                    viewModel.logout()
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ProfileUiState.Loading -> {
                            llLoading.visibility = View.VISIBLE
                            cvDetails.visibility = View.GONE
                            llError.visibility = View.GONE
                            btnLogout.isEnabled = false
                        }
                        is ProfileUiState.Success -> {
                            llLoading.visibility = View.GONE
                            cvDetails.visibility = View.VISIBLE
                            llError.visibility = View.GONE
                            btnLogout.isEnabled = true
                            
                            tvName.text = state.name
                            tvEmail.text = state.email
                            // ensure we have +91 prefix matching requirement if needed, otherwise use returned value
                            tvPhone.text = if (state.phone.startsWith("+")) state.phone else "+91 ${state.phone}"
                            tvUserId.text = state.userId
                        }
                        is ProfileUiState.Error -> {
                            llLoading.visibility = View.GONE
                            cvDetails.visibility = View.GONE
                            llError.visibility = View.VISIBLE
                            btnLogout.isEnabled = true // Always allow logout even on error
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logoutEvent.collect { isLoggedOut ->
                    if (isLoggedOut) {
                        val navController = findNavController()
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .build()
                        navController.navigate(R.id.landingFragment, null, navOptions)
                    }
                }
            }
        }
    }
}
