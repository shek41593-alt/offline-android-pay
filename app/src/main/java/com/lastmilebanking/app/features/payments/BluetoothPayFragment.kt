package com.lastmilebanking.app.features.payments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.lastmilebanking.app.R

class BluetoothPayFragment : Fragment() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        val statusText = view.findViewById<TextView>(R.id.tvBluetoothStatus)

        view.findViewById<MaterialButton>(R.id.btnStartBluetooth).setOnClickListener {
            if (!hasBluetoothPermissions()) {
                Toast.makeText(requireContext(), "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                statusText.text = "Bluetooth is OFF. Please enable it."
            } else {
                statusText.text = "Scanning for nearby merchant devices..."
                // TODO: Integrate BLE GATT scanning here in full implementation phase
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }
}
