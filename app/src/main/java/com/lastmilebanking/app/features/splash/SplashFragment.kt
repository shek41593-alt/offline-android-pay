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

import io.appwrite.services.Account
import com.lastmilebanking.app.data.network.auth.TokenStorage
import com.lastmilebanking.app.data.repository.AuthenticationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    @Inject
    lateinit var authRepository: AuthenticationRepository
    @Inject
    lateinit var appwriteAccount: Account
    @Inject
    lateinit var tokenStorage: TokenStorage
    
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
            val navController = androidx.navigation.Navigation.findNavController(requireView())
            navController.navigate(
                R.id.action_splash_to_home,
                null,
                androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
            )
        }
    }
}
