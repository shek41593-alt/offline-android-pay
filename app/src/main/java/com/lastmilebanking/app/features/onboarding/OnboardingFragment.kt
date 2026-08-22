package com.lastmilebanking.app.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.lastmilebanking.app.R

class OnboardingFragment : Fragment() {

    private var currentPage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvDesc)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)

        toolbar.setNavigationOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updateUI(tvTitle, tvDesc, btnNext)
            } else {
                findNavController().popBackStack()
            }
        }

        btnNext.setOnClickListener {
            if (currentPage < 3) {
                currentPage++
                updateUI(tvTitle, tvDesc, btnNext)
            } else {
                val bundle = Bundle().apply { putBoolean("isLogin", false) }
                findNavController().navigate(R.id.action_onboarding_to_create_account, bundle)
            }
        }
        
        updateUI(tvTitle, tvDesc, btnNext)
    }

    private fun updateUI(tvTitle: TextView, tvDesc: TextView, btnNext: MaterialButton) {
        when (currentPage) {
            1 -> {
                tvTitle.text = "Offline-first banking"
                tvDesc.text = "Access payment functionality even when internet connectivity is limited."
                btnNext.text = "Next"
            }
            2 -> {
                tvTitle.text = "Multiple payment methods"
                tvDesc.text = "QR, SMS, and Bluetooth options are available for seamless payments."
                btnNext.text = "Next"
            }
            3 -> {
                tvTitle.text = "Secure synchronization"
                tvDesc.text = "Transactions synchronize with the backend when connectivity becomes available."
                btnNext.text = "CREATE ACCOUNT"
            }
        }
    }
}
