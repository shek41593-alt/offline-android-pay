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

class AddressFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val etAddressLine = view.findViewById<TextInputEditText>(R.id.etAddressLine)
        val etCity = view.findViewById<TextInputEditText>(R.id.etCity)
        val etState = view.findViewById<TextInputEditText>(R.id.etState)
        val etPinCode = view.findViewById<TextInputEditText>(R.id.etPinCode)
        val btnDone = view.findViewById<MaterialButton>(R.id.btnDone)

        btnDone.setOnClickListener {
            val line = etAddressLine.text.toString().trim()
            val city = etCity.text.toString().trim()
            val state = etState.text.toString().trim()
            val pin = etPinCode.text.toString().trim()

            if (line.isEmpty() || city.isEmpty() || state.isEmpty() || pin.length != 6) {
                Toast.makeText(requireContext(), "Please fill all fields and enter valid 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.setAddress(line, city, state, pin)
            findNavController().navigate(R.id.action_address_to_sync)
        }
    }
}
