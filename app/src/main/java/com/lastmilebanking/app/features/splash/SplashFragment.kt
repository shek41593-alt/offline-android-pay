package com.lastmilebanking.app.features.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lastmilebanking.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.lastmilebanking.app.data.repository.AuthenticationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    @Inject
    lateinit var authRepository: AuthenticationRepository
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            if (authRepository.isAuthenticated()) {
                // Determine if profile exists to either go to Home or skeleton flow
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.action_splash_to_home)
            } else {
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.action_splash_to_landing)
            }
        }
    }
}
