package com.crypto.calculator.cardReader.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.crypto.calculator.cardReader.BasicCardReader
import java.util.UUID

abstract class BluetoothReader(context: Context, val device: BluetoothDevice) : BasicCardReader(context) {
    @SuppressLint("MissingPermission")
    private fun getBluetoothSocket(uuid: UUID): BluetoothSocket? {
        return device.createRfcommSocketToServiceRecord(uuid)
    }

    fun getBluetoothSocketClass(uuid: UUID): BluetoothSocketClass {
        return BluetoothSocketClass(getBluetoothSocket(uuid))
    }
}