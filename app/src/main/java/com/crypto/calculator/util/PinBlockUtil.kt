package com.crypto.calculator.util

import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.util.encryption.AES
import java.security.SecureRandom
import java.util.*
import kotlin.experimental.xor

/**
 * For pin block calculation
 * See ISO 9564 for more details
 */
object PinBlockUtil {

    /**
     * ISO 9564 when translate pin into pinblock it have 4 different formats
     */
    enum class PinBlockFormat(val code: Char) {
        Format0('0'),
        Format1('1'),
        Format2('2'),
        Format3('3'),
        Format4('4');

        companion object {
            fun getByValue(code: Char) = values().firstOrNull { it.code == code }
        }

    }

    /**
     * Helper function to define each format behaviour
     */
    private fun getPinBlockFormatBehaviour(pinBlockFormat: PinBlockFormat): Triple<Char?, Boolean, Boolean> {
        val defaultPaddingChar: Char?
        val needXorPan: Boolean
        var needExtendedPinBlock = false

        when (pinBlockFormat) {
            PinBlockFormat.Format0 -> {
                needXorPan = true
                defaultPaddingChar = 'F'
            }
            PinBlockFormat.Format1 -> {
                needXorPan = false
                defaultPaddingChar = null
            }
            PinBlockFormat.Format2 -> {
                needXorPan = false
                defaultPaddingChar = 'F'
            }
            PinBlockFormat.Format3 -> {
                needXorPan = true
                defaultPaddingChar = null

            }
            PinBlockFormat.Format4 -> {
                needXorPan = false
                defaultPaddingChar = 'A'
                needExtendedPinBlock = true
            }
        }

        return Triple(defaultPaddingChar, needXorPan, needExtendedPinBlock)
    }

    /**
     * Helper function to perform XOR for input PAN and PIN (Padded)
     */
    private fun performXorPinWithPan(pan: String?, paddedPin: String): String {
        require(pan != null && pan.length >= 13)

        val startIndex = pan.length - 1 - 12

        //Extract PAN field. Exclude check digit and only need last 12 digits
        val panString = "0000" + pan.substring(startIndex, startIndex + 12)

        val panStringArr = panString.hexToByteArray()

        val pinStringArr = paddedPin.hexToByteArray()

        for (i in panStringArr.indices) {
            panStringArr[i] = panStringArr[i].xor(pinStringArr[i])
        }

        return panStringArr.toHexString().uppercase(Locale.ENGLISH)
    }

    /**
     * Translate Pin block to Pin
     *
     * @param pinBlock The clear plain text pin block
     * @param pan optional Full PAN include check digit. Not all modes need this
     * @return Plain text PIN
     */
    fun pinBlockToPin(pinBlock: String, pan: String?): String {
        val format = PinBlockFormat.getByValue(pinBlock[0])
        requireNotNull(format)

        val behavior = getPinBlockFormatBehaviour(format)

        val defaultPaddingChar = behavior.first
        val needXorPan = behavior.second
        val needExtendedPinBlock = behavior.third

        var tmpPinBlock = pinBlock.uppercase(Locale.ENGLISH)

        if (needExtendedPinBlock) {
            require(pinBlock.length == 32)
            //First remove extend pin block
            tmpPinBlock = tmpPinBlock.substring(0, 16)
        } else {
            require(pinBlock.length == 16)
        }

        val pinStr = if (needXorPan) {
            performXorPinWithPan(pan, pinBlock)
        } else {
            tmpPinBlock
        }

        val pinLen = pinStr[1].toString().toInt(16)

        val padLen = 16 - 2 - pinLen

        //check padding if not null
        defaultPaddingChar?.let {
            if (!pinStr.endsWith(it.toString().repeat(padLen)))
                throw IllegalArgumentException("Wrong padding")
        }

        return pinStr.substring(2, 2 + pinLen)
    }

    /**
     * Translate PIN into PIN block
     *
     * @param pinBlockFormat The pin block format
     * @param pin Clear plain pin
     * @param pan optional Full PAN include check digit. Not all modes need this
     * @return Clear plain pin block
     */
    fun pinToPinBlock(pinBlockFormat: PinBlockFormat, pin: String, pan: String?): String {
        //Length check
        require(pin.length in 4..12)

        val behavior = getPinBlockFormatBehaviour(pinBlockFormat)

        val defaultPaddingChar = behavior.first
        val needXorPan = behavior.second
        val needExtendedPinBlock = behavior.third

        val pinStringWithPad = if (defaultPaddingChar != null) {
            //use Default padding char
            (pinBlockFormat.code + pin.length.toString(16) + pin).padEnd(16, defaultPaddingChar)
        } else {
            //use Random padding
            val byteArray = ByteArray(8)
            SecureRandom().nextBytes(byteArray)
            (pinBlockFormat.code + pin.length.toString(16) + pin + byteArray.toHexString()).substring(0, 16)
        }

        val tmpStr = if (needXorPan) {
            performXorPinWithPan(pan, pinStringWithPad)
        } else {
            pinStringWithPad
        }

        return if (needExtendedPinBlock) {
            //use Random to extend pin block for mode4
            val byteArray = ByteArray(8)
            SecureRandom().nextBytes(byteArray)
            tmpStr + byteArray.toHexString()
        } else {
            tmpStr
        }
    }

    /**
     * Decode Encrypted Pin block (EPB) into PIN using AES (format 4)
     * Refer to 9564-1:2017
     *
     * @param pinKey AES PIN key it use
     * @param encryptedPinBlock Encrypted PIN Block
     * @param pan full PAN include checking digit
     *
     * @return plain text PIN
     */
    fun encryptedPinBlockToPinAES(pinKey: String, encryptedPinBlock: String, pan: String): String {
        //1. Gen PAN block from Pan
        require(pan.length in 12..19)

        val padLen = pan.length - 12

        val panBlock = (padLen.toString() + pan).padEnd(32, '0')

        //2. Gen Block B
        val blockA = AES.decrypt(encryptedPinBlock, pinKey, AES.Padding.NoPadding)

        //3. Perform Xor to derived Block A
        require(blockA != null)
        val blockAArr = blockA.hexToByteArray()

        val panBlockArr = panBlock.hexToByteArray()

        for (i in panBlockArr.indices) {
            panBlockArr[i] = panBlockArr[i].xor(blockAArr[i])
        }

        //4. Derive clear pin block
        val clearPinBlock = AES.decrypt(panBlockArr, pinKey.hexToByteArray(), AES.Padding.NoPadding)?.toHexString()

        require(clearPinBlock != null)

        //5. Convert Pin block to PIN
        return pinBlockToPin(clearPinBlock, pan)
    }

    /**
     * Encode PIN into Encrypted Pin block (EPB) using AES (format 4)
     * Refer to 9564-1:2017
     *
     * @param pinKey AES PIN key it use
     * @param pin Clear plain text PIN
     * @param pan full PAN include checking digit
     *
     * @return Encrypted Pin block EPB
     */
    fun pinToEncryptedPinBlockAES(pinKey: String, pin: String, pan: String): String {
        //1. Gen PAN block from Pan
        require(pan.length in 12..19)

        val padLen = pan.length - 12

        val panBlock = (padLen.toString() + pan).padEnd(32, '0')

        //2. Convert clear pin block from pin
        val pinBlock = pinToPinBlock(PinBlockFormat.Format4, pin, null)

        //3. Derive Intermediate Block A
        val blockA = AES.encrypt(pinBlock, pinKey, AES.Padding.NoPadding)

        //4. Derive Intermediate Block B by Perform XOR
        require(blockA != null)
        val blockAArr = blockA.hexToByteArray()

        val panBlockArr = panBlock.hexToByteArray()

        for (i in panBlockArr.indices) {
            panBlockArr[i] = panBlockArr[i].xor(blockAArr[i])
        }

        //5. Generate final Enciphered pin block
        val epbArray = AES.encrypt(panBlockArr, pinKey.hexToByteArray(), AES.Padding.NoPadding)

        return epbArray!!.toHexString()
    }
}