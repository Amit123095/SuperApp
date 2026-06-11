package com.amit.application.AppUtils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class AppBluetoothManager(context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // Standard SPP (Serial Port Profile) UUID used by 99% of Bluetooth receipt/thermal printers
    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Returns a simple list of paired devices filtering or displaying potential printers
    @SuppressLint("MissingPermission") // Checked dynamically at UI layer on Android 12+
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    // Connects to a selected device and prints raw strings/ESC-POS byte packets
    @SuppressLint("MissingPermission")
    suspend fun printText(deviceAddress: String, rawText: String): Boolean = withContext(Dispatchers.IO) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return@withContext false
        var outputStream: OutputStream? = null

        try {
            // Secure RFCOMM socket connection
            val socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            outputStream = socket.outputStream

            // Format trailing feeds to let paper push past tear-bar cleanly
            val formattedMessage = rawText + "\n\n\n"
            outputStream.write(formattedMessage.toByteArray(charset("GBK"))) // Standard thermal layout encoding
            outputStream.flush()

            // Short rest interval before tear down closure
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}