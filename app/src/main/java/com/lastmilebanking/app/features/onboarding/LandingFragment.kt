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

        view.findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            findNavController().navigate(R.id.action_landing_to_onboarding)
        }

        view.findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isLogin", true) }
            findNavController().navigate(R.id.action_landing_to_login, bundle)
        }
    }
}
