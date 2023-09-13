package com.crypto.calculator.cardReader.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class BluetoothSocketClass(socket: BluetoothSocket?) {
    var socket: BluetoothSocket? = null
    protected var mBtSocketInputStream: InputStream? = null
    var outputStream: OutputStream? = null

    init {
        this.socket = socket
        if (this.socket != null) {
            try {
                mBtSocketInputStream = this.socket!!.inputStream
            } catch (var4: IOException) {
                Log.e("BluetoothSocketClass", "IOException ex mBtSocketInputStream")
            }
            try {
                outputStream = this.socket!!.outputStream
            } catch (var3: IOException) {
                Log.e("BluetoothSocketClass", "IOException ex m_btSocketOutputStream")
            }
        }
    }

    val isSocketConnected: Boolean
        get() {
            var flag = false
            if (socket != null) {
                flag = socket!!.isConnected
            }
            return flag
        }
    var inputStream: InputStream?
        get() {
            if (mBtSocketInputStream == null) {
                Log.e("BluetoothSocketClass", "mBtSocketInputStream is NULL")
            }
            return mBtSocketInputStream
        }
        set(iStream) {
            mBtSocketInputStream = iStream
        }

    fun safeFreeBTSocket() {
        if (mBtSocketInputStream != null) {
            try {
                mBtSocketInputStream!!.close()
            } catch (var4: IOException) {
            }
            mBtSocketInputStream = null
        }
        if (outputStream != null) {
            try {
                outputStream!!.close()
            } catch (var3: IOException) {
            }
            outputStream = null
        }
        if (socket != null) {
            try {
                socket!!.close()
                Log.i("BluetoothSocketClass", "safeFreeBTSocket socket closed")
            } catch (var2: IOException) {
            }
            socket = null
        }
    }

    fun onDestroy() {
        safeFreeBTSocket()
    }

    companion object {
        private const val m_className = "BluetoothSocketClass"
    }
}
