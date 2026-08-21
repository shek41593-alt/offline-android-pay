package com.lastmilebanking.app.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import com.lastmilebanking.app.features.authentication.AuthViewModel

class PersonalInfoFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_personal_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val etFullName = view.findViewById<TextInputEditText>(R.id.etFullName)
        val etDob = view.findViewById<TextInputEditText>(R.id.etDob)
        val btnContinue = view.findViewById<MaterialButton>(R.id.btnContinue)

        btnContinue.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val dob = etDob.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Full name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.setPersonalInfo(name, dob)
            findNavController().navigate(R.id.action_personal_to_address)
        }
    }
}
