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
            
            val hasJwt = tokenStorage.hasToken()
            
            val hasAppwriteSession = try {
                val session = appwriteAccount.getSession("current")
                session.current
            } catch (e: io.appwrite.exceptions.AppwriteException) {
                if (e.code == 401) false else true // If offline, assume true for now
            } catch (e: Exception) {
                true 
            }
            
            // Allow offline MVP bypass for dev fallback
            val isDevAuth = com.lastmilebanking.app.BuildConfig.DEV_AUTH_FALLBACK_ENABLED && tokenStorage.getToken() == "LOCAL_DEV_OFFLINE_SESSION"
            
            val navController = androidx.navigation.Navigation.findNavController(requireView())
            
            if (isDevAuth || (hasJwt && hasAppwriteSession)) {
                // Fully Authenticated -> Home
                navController.navigate(
                    R.id.action_splash_to_home,
                    null,
                    androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
                )
            } else {
                // No valid session, or expired -> Landing
                authRepository.logout() // clear any stale state
                navController.navigate(
                    R.id.action_splash_to_landing,
                    null,
                    androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
                )
            }
        }
    }
}
