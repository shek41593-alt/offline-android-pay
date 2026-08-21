package com.lastmilebanking.app.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.lastmilebanking.app.R

class LandingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            // Using a bundle to specify the intent flow if needed, but for now just navigate
            val bundle = Bundle().apply { putBoolean("isLogin", true) }
            findNavController().navigate(R.id.action_landing_to_login, bundle)
        }

        view.findViewById<MaterialButton>(R.id.btnCreateAccount).setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isLogin", false) }
            findNavController().navigate(R.id.action_landing_to_login, bundle)
        }
    }
}
