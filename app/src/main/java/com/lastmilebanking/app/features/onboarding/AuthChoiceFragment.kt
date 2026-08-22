package com.lastmilebanking.app.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.lastmilebanking.app.R

class AuthChoiceFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_auth_choice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        view.findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isLogin", true) }
            findNavController().navigate(R.id.action_authChoice_to_login, bundle)
        }

        view.findViewById<MaterialButton>(R.id.btnCreateAccount).setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isLogin", false) }
            findNavController().navigate(R.id.action_authChoice_to_login, bundle)
        }
    }
}
