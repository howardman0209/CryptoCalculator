package com.crypto.calculator.ui.viewModel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.extension.adjustDESParity
import com.crypto.calculator.extension.applyISO9797Padding
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.BaseViewModel
import com.crypto.calculator.util.Encryption
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import javax.crypto.Cipher

class CoreViewModel : BaseViewModel() {
    val gsonBeautifier: Gson = GsonBuilder().setPrettyPrinting().create()
    val title: ObservableField<String> = ObservableField()
    val logMessage: MutableLiveData<String> = MutableLiveData()
    val currentTool: MutableLiveData<Tool> = MutableLiveData(Tool.TLV_PARSER)

    val inputData1: ObservableField<String> = ObservableField()
    val inputData1Max: ObservableField<Int?> = ObservableField()
    val inputData1Label: ObservableField<String> = ObservableField()

    val inputData2: ObservableField<String> = ObservableField()
    val inputData2Max: ObservableField<Int?> = ObservableField()
    val inputData2Label: ObservableField<String> = ObservableField()

    fun printLog(message: String) {
        logMessage.postValue(message)
    }

    fun desEncrypt(data: String, key: String, mode: String, padding: String): String {
        val plaintext = when (padding) {
            PaddingMethod.ISO9797_1_M1.name -> data.applyISO9797Padding(1)
            PaddingMethod.ISO9797_1_M2.name -> data.applyISO9797Padding(2)
            else -> data
        }
        return Encryption.doDES(key, plaintext, mode, Cipher.ENCRYPT_MODE).uppercase()
    }

    fun desDecrypt(data: String, key: String, mode: String, padding: String): String {
        val decrypted = Encryption.doDES(key, data, mode, Cipher.DECRYPT_MODE).uppercase()
        return when (padding) {
            PaddingMethod.ISO9797_1_M2.name -> decrypted.substringBeforeLast("80")
            else -> decrypted
        }
    }

    fun macCompute(data: String, key: String, padding: String): String {
        val plaintext = when (padding) {
            PaddingMethod.ISO9797_1_M1.name -> data.applyISO9797Padding(1)
            PaddingMethod.ISO9797_1_M2.name -> data.applyISO9797Padding(2)
            else -> data
        }
        return Encryption.calculateMAC(key, plaintext).uppercase()
    }

    fun fixDESKeyParity(key: String): String {
        return key.hexToByteArray().adjustDESParity().toHexString().uppercase()
    }
}