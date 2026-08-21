package com.lastmilebanking.app.features.payments

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lastmilebanking.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class BluetoothPayFragment : Fragment() {

    private val viewModel: BluetoothPayViewModel by viewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private lateinit var devicesAdapter: DeviceAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var paymentDialog: AlertDialog? = null
    
    private var pendingAmount: Double = 0.0

    // Standard BLE UART Service UUIDs
    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_RX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.all { it.value }) {
            checkBluetoothAndScan()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        devicesAdapter = DeviceAdapter(discoveredDevices.toList()) { device ->
            connectToDevice(device)
        }
        
        val rvDevices = view.findViewById<RecyclerView>(R.id.rvDevices)
        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = devicesAdapter

        val btnScan = view.findViewById<MaterialButton>(R.id.btnScan)
        val btnEnable = view.findViewById<MaterialButton>(R.id.btnEnable)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)

        btnScan.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Enter valid amount first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pendingAmount = amount
            
            if (hasPermissions()) {
                checkBluetoothAndScan()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
        }

        btnEnable.setOnClickListener {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BluetoothPayState.Verify -> {
                            // After we connect securely and write, or before?
                            // Wait, we need to connect first, then write.
                            // The state is just managed asynchronously below.
                        }
                        is BluetoothPayState.Success -> {
                            showSuccessDialog(state.transactionId)
                            viewModel.resetState()
                            disconnectGatt()
                        }
                        is BluetoothPayState.Error -> {
                            showErrorDialog(state.message)
                            viewModel.resetState()
                            disconnectGatt()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun checkBluetoothAndScan() {
        val btnEnable = view?.findViewById<MaterialButton>(R.id.btnEnable)
        val btnScan = view?.findViewById<MaterialButton>(R.id.btnScan)
        val statusText = view?.findViewById<TextView>(R.id.tvBluetoothStatus)

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            statusText?.text = "Bluetooth is required for nearby payment."
            btnEnable?.visibility = View.VISIBLE
            btnScan?.visibility = View.GONE
        } else {
            btnEnable?.visibility = View.GONE
            btnScan?.visibility = View.VISIBLE
            startScan()
        }
    }

    private fun startScan() {
        discoveredDevices.clear()
        updateDeviceList()
        view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Scanning for nearby merchants..."

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "BLE Scanner not available on this device."
            return
        }

        scanner.startScan(scanCallback)
        
        view?.postDelayed({
            scanner.stopScan(scanCallback)
            if (discoveredDevices.isEmpty()) {
                view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "No devices found."
            } else {
                view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Select a merchant to connect"
            }
        }, 10000)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name != null) {
                if (discoveredDevices.add(result.device)) {
                    updateDeviceList()
                }
            }
        }
    }

    private fun updateDeviceList() {
        devicesAdapter.updateData(discoveredDevices.toList())
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Connecting to ${device.name ?: device.address}..."

        // We prepare state up to "Verify" internally, wait to get payload
        // But the prompt wants confirm AT THIS UI moment, before connecting?
        // Let's ask user to confirm FIRST, then connect & transmit?
        // Or connect first, then confirm? "After receiver/device is identified: show a confirmation screen... CONFIRM PAYMENT... Do NOT send the transaction merely because Bluetooth connected."
        
        showConfirmationDialog(device, pendingAmount)
    }

    private fun showConfirmationDialog(device: BluetoothDevice, amount: Double) {
        val receiverName = device.name ?: device.address
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Payment")
            .setMessage("Receiver: $receiverName\nAmount: $$amount\nDevice: ${device.address}")
            .setCancelable(false)
            .setPositiveButton("Confirm Payment", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Payment cancelled."
            }
        
        paymentDialog = builder.create()
        paymentDialog?.setOnShowListener {
            val positiveButton = paymentDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.setOnClickListener {
                positiveButton.isEnabled = false
                positiveButton.text = "Connecting & Paying..."
                executeBluetoothPaymentSequence(device, amount)
            }
        }
        paymentDialog?.show()
    }

    private fun executeBluetoothPaymentSequence(device: BluetoothDevice, amount: Double) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            // First run verify payment logic in ViewModel to get payload
            viewModel.verifyPayment(device.address, amount)
            
            // We wait slightly to allow viewModel to generate Payload.
            // A perfect solution collects the exact state, but here we can just safely
            // fetch it or wait for the collect. To ensure real BLE interaction:
            bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                requireActivity().runOnUiThread {
                    view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Connected. Sending data..."
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                requireActivity().runOnUiThread {
                    // Clean up and error out if disconnected during payment
                    view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Connection lost."
                    viewModel.abortPayment("Connection failed/lost.")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CHAR_RX_UUID)
                    if (characteristic != null) {
                        // Extract payload from viewModel's state if we are in Verify
                        val currentState = viewModel.uiState.value
                        if (currentState is BluetoothPayState.Verify) {
                            characteristic.value = currentState.payload.toByteArray()
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            gatt.writeCharacteristic(characteristic)
                        } else {
                            viewModel.abortPayment("Internal error: payload not ready.")
                        }
                    } else {
                        viewModel.abortPayment("Characteristic not found on device.")
                    }
                } else {
                    viewModel.abortPayment("Payment Service not supported by this receiver.")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requireActivity().runOnUiThread {
                    val currentState = viewModel.uiState.value
                    if (currentState is BluetoothPayState.Verify) {
                        viewModel.completePayment(currentState.receiverId, currentState.amount)
                    }
                }
            } else {
                requireActivity().runOnUiThread {
                    viewModel.abortPayment("Failed to transmit payment data.")
                }
            }
        }
    }

    private fun disconnectGatt() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun showSuccessDialog(transactionId: String) {
        paymentDialog?.dismiss()
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Successful")
            .setMessage("Transaction completed.\nTransaction ID: $transactionId")
            .setCancelable(false)
            .setPositiveButton("DONE") { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        paymentDialog?.dismiss()
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Payment Failed")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("TRY AGAIN") { _, _ ->
                view?.findViewById<TextView>(R.id.tvBluetoothStatus)?.text = "Tap SCAN again"
            }
            .setNegativeButton("BACK") { _, _ ->
                findNavController().popBackStack()
            }
        builder.show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Bluetooth and Location permissions are required to scan for nearby devices. Please enable them in app settings if permanently denied.")
            .setPositiveButton("RETRY") { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        disconnectGatt()
    }
}

// Simple adapter for devices
class DeviceAdapter(private var devices: List<BluetoothDevice>, private val onClick: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    @SuppressLint("MissingPermission")
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
        val tvAddress: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device.name ?: "Unknown Device"
        holder.tvAddress.text = device.address
        holder.itemView.setOnClickListener { onClick(device) }
    }

    override fun getItemCount() = devices.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newDevices: List<BluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}
