package com.lastmilebanking.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var sessionManager: com.lastmilebanking.app.data.network.auth.SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        bottomNavView.setupWithNavController(navController)
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.walletFragment,
                R.id.historyFragment,
                R.id.profileFragment -> {
                    bottomNavView.visibility = View.VISIBLE
                }
                else -> {
                    bottomNavView.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            sessionManager.unauthorizedEvent.collect {
                // Navigate to landing, clearing backstack
                navController.navigate(R.id.landingFragment, null, 
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
            }
        }
    }
}
