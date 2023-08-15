package com.crypto.calculator.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.Editable
import android.util.Base64
import android.util.Patterns
import com.crypto.calculator.model.BitwiseOperation
import com.crypto.calculator.util.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.math.BigDecimal
import java.math.BigInteger
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.ceil

/**
 * Convert to ISOAmountString
 * @param exponent exponent
 * example: "3.52".toISOAmountString(2) -> "352"
 */
fun String.toISOAmountString(exponent: Int): String = this.toBigDecimal().movePointRight(exponent).toBigInteger().toString()

/**
 * Convert from ISOAmountString to BigDecimal
 * @param exponent exponent
 * example: "352".toBigDecimalFromISOAmountString(2) -> 3.52
 */
fun String.toBigDecimalFromISOAmountString(exponent: Int): BigDecimal = this.toBigDecimal().movePointLeft(exponent)
fun String.simplifiedCurrencySymbol(): String {
    val pattern: Pattern = Pattern.compile("[A-Z]")
    val matcher: Matcher = pattern.matcher(this)
    return matcher.replaceAll("")
}

fun String.toCurrencyDecimal(): Int {
    return Currency.getInstance(this).defaultFractionDigits
}

fun String.toMaskedAccount(): String {
    return this.takeLast(4).padStart(this.length, '*')
}

fun String.toTimestampOrNull(): Long? {
    val dateFormat = SimpleDateFormat(DATE_TIME_PATTERN_UTC, Locale.ENGLISH)
    return try {
        dateFormat.parse(this)?.time?.div(1000)
    } catch (e: Exception) {
        null
    }
}

fun String.toLocalTime(pattern: String? = DATE_TIME_DISPLAY_PATTERN_SHORT): String? {
    return try {
        val dateFormat = SimpleDateFormat(DATE_TIME_DISPLAY_PATTERN_FULL, Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone(timeZone_HK)
        val date = dateFormat.parse(this)
        SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            timeZone = Calendar.getInstance().timeZone
        }.format(date!!)

    } catch (ex: ParseException) {
        null
    }
}

@Suppress("unused")
fun String.base64StringToBitmap(): Bitmap? {
    val pureBase64String = this.split(",").last() //remove "data:image/jpg;base64," in the front
    val decode: ByteArray = Base64.decode(pureBase64String, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decode, 0, decode.size)
}

/**
 * Email pattern checking
 */
fun String.isEmailValid(): Boolean = Patterns.EMAIL_ADDRESS.matcher(this).matches()

/**
 * @return error res if username not valid; or null if valid
 */
fun String?.usernameError(): String? = if (!isNullOrBlank())
    null
else
    "Invalid username"

fun String.toDateFormat(inputFormat: String, displayFormat: String): String {
    val format = SimpleDateFormat(inputFormat, Locale.ENGLISH)
    val sdfDisplayFormat = SimpleDateFormat(displayFormat, Locale.ENGLISH)
    return try {
        val date = format.parse(this)
        if (date == null)
            this
        else
            sdfDisplayFormat.format(date)
    } catch (e: ParseException) {
        this
    }
}

fun String.utcToLocalTime(pattern: String? = DATE_TIME_DISPLAY_PATTERN_SHORT): String? {
    return try {
        val utcFormat = SimpleDateFormat(DATE_TIME_PATTERN_UTC_DEFAULT, Locale.ENGLISH)
        utcFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = utcFormat.parse(this)
        SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            timeZone = Calendar.getInstance().timeZone
        }.format(date!!)

    } catch (ex: ParseException) {
        null
    }
}

fun String.toServerBase64String(): String {
    return "data:image/jpeg;base64,$this"
}

/**
 * Truncate string if too long and ended with ellipsis (…)
 */
fun String.truncateAndEllipsis(maxLength: Int = 15): String {
    return trim().run {
        if (length <= maxLength) this else "${take(maxLength)}…"
    }
}

@Suppress("unused")
fun Char.isAlphaNumeric(): Boolean {
    val regex = Regex("[a-zA-Z0-9]+?")
    return regex.matches(this.toString())
}

fun String.insert(insert: String, index: Int): String {
    val start = substring(0, index)
    val end = substring(index)
    return start + insert + end
}

fun String.hexToByteArray(): ByteArray {
    //Add leading zero in case of odd len
    val str = if (this.length and 1 == 1) {
        "0$this"
    } else
        this
    return str.trim().chunked(2).map { it.toInt(16).toByte() }.toByteArray()

}

fun String.rotate(n: Int) = drop(n % length) + take(n % length)

fun String.decodeHex(): String {
    require(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
        .toString(Charsets.ISO_8859_1)  // Or whichever encoding your input uses
}

fun String.asciiToHex(): String {
    val output = StringBuilder("")

    for (c: Char in this)
        output.append(c.toInt().toString(16))

    return output.toString()
}

/**
 * Convert ISO8601 Date time string into A pair of Date String (MMdd) and Time String (HHmmss)
 */
fun String.toISO8601To8583DateTime(): Pair<String, String> {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
    val txnTime = fmt.parse(this)
    val timeOut = SimpleDateFormat("HHmmss", Locale.ENGLISH)
    val dateOut = SimpleDateFormat("MMdd", Locale.ENGLISH)

    return Pair(
        dateOut.format(txnTime),
        timeOut.format(txnTime)
    )
}

fun String.convertToArray(): List<String> {
    return this.substring(1, this.lastIndex).split(", ")
}

fun String.qrStringToBitmap(): Bitmap? {
    val result: BitMatrix = try {
        MultiFormatWriter().encode(this, BarcodeFormat.QR_CODE, 300, 300, null)
    } catch (iae: IllegalArgumentException) {
        // Unsupported format
        return null
    }
    val w = result.width
    val h = result.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (result[x, y]) Color.BLACK else Color.WHITE
        }
    }
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, 300, 0, 0, w, h)
    return bitmap
}

fun String.hexToBinary(): String {
    val binary = this.toLong(16).toString(2).uppercase()
    return binary.padStart(this.length * 4, '0')
}

fun String.hexBitwise(hex: String, operation: BitwiseOperation): String {
    val i1 = BigInteger(this, 16)
    val i2 = BigInteger(hex, 16)
    val i3 = BigInteger("FF".padEnd(this.length, 'F'), 16)
    val res = when (operation) {
        BitwiseOperation.XOR -> i1.xor(i2)
        BitwiseOperation.AND -> i1.and(i2)
        BitwiseOperation.OR -> i1.or(i2)
        BitwiseOperation.NOT -> i1.xor(i3)
    }
    return res.toString(16).uppercase().padStart(hex.length, '0')
}

fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

fun String.applyISO9797Padding(method: Int): String {
    return when (method) {
        1 -> {
            val padLen = ceil(this.length.div(16.0)).toInt().times(16).let {
                if (it > 16) it else 16
            }
            this.padEnd(padLen, '0')
        }

        2 -> {
            val padLen = ceil("${this}80".length.div(16.0)).toInt().times(16).let {
                if (it > 16) it else 16
            }
            "${this}80".padEnd(padLen, '0')
        }

        else -> this
    }
}