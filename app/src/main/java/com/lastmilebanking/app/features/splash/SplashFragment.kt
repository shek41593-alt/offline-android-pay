package com.lastmilebanking.app.features.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lastmilebanking.app.R
import com.lastmilebanking.app.features.authentication.AuthViewModel
import com.lastmilebanking.app.features.authentication.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Authenticated -> {
                            val navController = androidx.navigation.Navigation.findNavController(requireView())
                            navController.navigate(
                                R.id.action_splash_to_home,
                                null,
                                androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
                            )
                        }
                        is AuthState.Unauthenticated -> {
                            val navController = androidx.navigation.Navigation.findNavController(requireView())
                            navController.navigate(
                                R.id.action_splash_to_landing,
                                null,
                                androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
                            )
                        }
                        is AuthState.Checking -> {
                            // Wait
                        }
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            authViewModel.checkAuthStatus()
        }
    }
}
