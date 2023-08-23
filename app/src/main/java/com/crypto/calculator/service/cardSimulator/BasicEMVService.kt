package com.crypto.calculator.service.cardSimulator

import android.util.Log
import com.crypto.calculator.service.BasicApduService

abstract class BasicEMVService : BasicApduService() {
    var emvFlowDelegate: EMVFlowDelegate? = null

    interface EMVFlowDelegate {
        fun onPPSEReply(cAPDU: String): String
        fun onSelectAIDReply(cAPDU: String): String
        fun onExecuteGPOReply(cAPDU: String): String
        fun onReadRecordReply(cAPDU: String): String
        fun onGenerateACReply(cAPDU: String): String
        fun onGetChallengeReply(cAPDU: String): String
    }

    override fun responseConstructor(cAPDU: String?): String {
        if (cAPDU == null) return "6A82"

        return try {
            val rAPDU = when {
                cAPDU == "00A404000E325041592E5359532E444446303100" -> emvFlowDelegate?.onPPSEReply(cAPDU)
                cAPDU.startsWith("00A40400") -> emvFlowDelegate?.onSelectAIDReply(cAPDU)
                cAPDU.startsWith("80A80000") -> emvFlowDelegate?.onExecuteGPOReply(cAPDU)
                cAPDU.startsWith("00B2") -> emvFlowDelegate?.onReadRecordReply(cAPDU)
                cAPDU.startsWith("80AE") -> emvFlowDelegate?.onGenerateACReply(cAPDU)
                cAPDU == "0084000000" -> emvFlowDelegate?.onGetChallengeReply(cAPDU)
                else -> "6A82"
            }

            rAPDU ?: "6A82"
        } catch (e: Exception) {
            Log.e("BasicEMVCardSimulator", "Exception: $e")
            "6A82"
        }
    }

}