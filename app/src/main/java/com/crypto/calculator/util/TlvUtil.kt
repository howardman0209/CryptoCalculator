package com.crypto.calculator.util

import android.util.Log
import com.crypto.calculator.extension.hexToBinary
import com.crypto.calculator.extension.toDataClass
import com.crypto.calculator.extension.toHexString
import com.google.gson.JsonObject
import java.lang.StringBuilder

object TlvUtil {
    /**
     * Decode TLV into structured map
     * Eg. input tlv = 6F6C840E325041592E5359532E4444463031A558BF0C55800101A503800177611B4F07A00000015230105008446973636F7665728701019F2A02000661164F07A00000032410105008446973636F76657287010261164F07A00000032410105008446973636F7665728701029000
     * return {"6F":{"84":"325041592E5359532E4444463031","A5":{"BF0C":{"80":"01","A5":{"80":"77"},"61":[{"4F":"A0000001523010","50":"446973636F766572","87":"01","9F2A":"0006"},{"4F":"A0000003241010","50":"446973636F766572","87":"02"},{"4F":"A0000003241010","50":"446973636F766572","87":"02"}]}}}}
     */
    fun decodeTLV(tlv: String): Map<String, Any?> {
        Log.d("TLV_DECODER, Start", "tlv: $tlv")
        try {
            val output: MutableMap<String, Any?> = mutableMapOf()
            var cursor = 0
            while (cursor < tlv.length) {
                val tag = nextTag(tlv.substring(cursor))
                Log.d("TLV_DECODER", "Current tag: $tag")
                val lengthByte = when (tlv.substring(cursor + tag.length, cursor + tag.length + 2)) {
                    "81" -> {
                        cursor += 2
                        2
                    }

                    "82" -> {
                        cursor += 2
                        4
                    }

                    else -> 2
                }
                val dataLength = tlv.substring(cursor + tag.length, cursor + tag.length + lengthByte).toInt(16) * 2
                val data = tlv.substring(cursor + tag.length + lengthByte, cursor + tag.length + lengthByte + dataLength)
                Log.d("TLV_DECODER", "Tag: $tag, Length: $dataLength, Value: $data")

                if (isTemplateTag(tag)) {
                    if (output[tag] != null) {
                        when (output[tag]) {
                            is MutableList<*> -> (output[tag] as MutableList<Any?>).add(decodeTLV(data))
                            else -> output[tag] = mutableListOf(output[tag]).apply { add(decodeTLV(data)) }
                        }
                    } else {
                        output[tag] = decodeTLV(data)
                    }
                } else {
                    output[tag] = data
                    Log.d("TLV_DECODER", "output - ${tag}: ${output[tag]}")
                }
                cursor += (tag.length + lengthByte + dataLength)
                Log.d("TLV_DECODER", "Remaining Tlv: ${tlv.substring(cursor)}")

                if (tlv.substring(cursor) == APDU_RESPONSE_CODE_OK) {
                    Log.d("TLV_DECODER, End", "Decoded TLV: $output")
                    return output.toSortedMap()
                }
            }
            Log.d("TLV_DECODER, End", "Decoded TLV: $output")
            return output.toSortedMap()
        } catch (e: Exception) {
            Log.d("TLV_DECODER", "Exception: $e")
            throw Exception("TLV decode error: Invalid TLV")
        }
    }

    /**
     * Construct tlv from input structured map
     * Eg. input map = {"6F":{"84":"325041592E5359532E4444463031","A5":{"BF0C":{"80":"01","A5":{"80":"77"},"61":[{"4F":"A0000001523010","50":"446973636F766572","87":"01","9F2A":"0006"},{"4F":"A0000003241010","50":"446973636F766572","87":"02"},{"4F":"A0000003241010","50":"446973636F766572","87":"02"}]}}}}
     * return 6F6A840E325041592E5359532E4444463031A558BF0C55800101A503800177611B4F07A00000015230105008446973636F7665728701019F2A02000661164F07A00000032410105008446973636F76657287010261164F07A00000032410105008446973636F766572870102
     */
    fun encodeTLV(jsonString: String): String {
        val jsonObj = try {
            jsonString.toDataClass<JsonObject>()
        } catch (ex: Exception) {
            Log.d("TLV_ENCODER", "Exception: $ex")
            throw ex
        }
        return encodeTLV(jsonObj)
    }

    fun encodeTLV(json: JsonObject): String {
        Log.d("TLV_ENCODER, Start", "json: $json")
        try {
            val sb = StringBuilder()
            json.entrySet().forEach { (tag, value) ->
                when {
                    value.isJsonArray -> {
                        Log.d("TLV_ENCODER", "List value: $value")
                        value.asJsonArray.forEach {
                            val tlv = encodeTLV(it.asJsonObject)
                            val length = when ((tlv.length / 2)) {
                                in 0 until 126 -> (tlv.length / 2).toHexString()
                                in 127 until 256 -> "81" + (tlv.length / 2).toHexString()
                                else -> "82" + (tlv.length / 2).toHexString()
                            }
                            sb.append("$tag$length$tlv")
                        }
                    }


                    value.isJsonObject -> {
                        Log.d("TLV_ENCODER", "else value: $value")
                        val tlv = encodeTLV(value.asJsonObject)
                        val length = when ((tlv.length / 2)) {
                            in 0 until 126 -> (tlv.length / 2).toHexString()
                            in 127 until 256 -> "81" + (tlv.length / 2).toHexString()
                            else -> "82" + (tlv.length / 2).toHexString()
                        }
                        sb.append("$tag$length$tlv")
                    }

                    else -> {
                        val stringValue = value.asString.replace("\"", "")
                        Log.d("TLV_ENCODER", "String value: ${value.asString}")
                        val length = when ((stringValue.length / 2)) {
                            in 0 until 126 -> (stringValue.length / 2).toHexString()
                            in 127 until 256 -> "81" + (stringValue.length / 2).toHexString()
                            else -> "82" + (stringValue.length / 2).toHexString()
                        }
                        Log.d("TLV_ENCODER", "length: $length")
                        sb.append("$tag$length$stringValue")
                    }
                }
            }

            val tlv = sb.toString()
            Log.d("TLV_ENCODER, End", "tlv: $tlv")
            return tlv
        } catch (e: Exception) {
            Log.d("TLV_ENCODER", "Exception: $e")
            throw Exception("TLV encode error: Invalid Tag")
        }
    }

    fun encodeTLV(map: Map<String, Any?>): String {
        Log.d("TLV_ENCODER, Start", "map: $map")
        try {
            val sb = StringBuilder()
            map.forEach { (tag, value) ->
                when (value) {
                    is ArrayList<*> -> {
                        Log.d("TLV_ENCODER", "List value: $value")
                        value.forEach {
                            val tlv = encodeTLV(it as Map<String, Any?>)
                            val length = when ((tlv.length / 2)) {
                                in 0 until 126 -> (tlv.length / 2).toHexString()
                                in 127 until 256 -> "81" + (tlv.length / 2).toHexString()
                                else -> "82" + (tlv.length / 2).toHexString()
                            }
                            sb.append("$tag$length$tlv")
                        }
                    }

                    is String -> {
                        Log.d("TLV_ENCODER", "String value: $value")
                        val length = when ((value.length / 2)) {
                            in 0 until 126 -> (value.length / 2).toHexString()
                            in 127 until 256 -> "81" + (value.length / 2).toHexString()
                            else -> "82" + (value.length / 2).toHexString()
                        }
                        Log.d("TLV_ENCODER", "length: $length")
                        sb.append("$tag$length$value")
                    }

                    else -> {
                        Log.d("TLV_ENCODER", "else value: $value")
                        val tlv = encodeTLV(value as Map<String, Any?>)
                        val length = when ((tlv.length / 2)) {
                            in 0 until 126 -> (tlv.length / 2).toHexString()
                            in 127 until 256 -> "81" + (tlv.length / 2).toHexString()
                            else -> "82" + (tlv.length / 2).toHexString()
                        }
                        sb.append("$tag$length$tlv")
                    }
                }
            }

            val tlv = sb.toString()
            Log.d("TLV_ENCODER, End", "tlv: $tlv")
            return tlv
        } catch (e: Exception) {
            Log.d("TLV_ENCODER", "Exception: $e")
            throw Exception("TLV encode error: Invalid Tag")
        }
    }


    /**
     * read next valid tag from input tlv (follow the ASN.1 rule)
     * Eg. input tlv = DFA200825F2ADFA2009F029F6E90
     * return DFA200
     */
    private fun nextTag(tlv: String): String {
        val nextTag: String
        var cursor = 0
        var currentByte = tlv.substring(0, 2).hexToBinary()
        if (currentByte.endsWith("11111")) {
            cursor += 2
            currentByte = tlv.substring(cursor, cursor + 2).hexToBinary()
            while (currentByte.startsWith("1")) {
                cursor += 2
                currentByte = tlv.substring(cursor, cursor + 2).hexToBinary()
            }
            Log.d("nextTag", "Tag: ${tlv.substring(0, cursor + 2)}")
            nextTag = tlv.substring(0, cursor + 2)
        } else {
            Log.d("nextTag", "Tag: ${tlv.substring(0, 2)}")
            nextTag = tlv.substring(0, 2)
        }

        return nextTag
    }


    /**
     * check tag is whether template tag or not (follow the ASN.1 rule)
     * Eg. input tag = 70, A5 return true
     * Eg. input tag = 80, 9F02 return false
     */
    fun isTemplateTag(tag: String): Boolean {
        return tag.substring(0, 2).hexToBinary()[2] == '1'
    }

    /**
     * Separate tags concatenate in a string into list of tag (follow the ASN.1 rule)
     * Eg. input tlv = DFA200825F2ADFA2009F029F6E90
     * return [DFA200, 82, 5F2A, DFA200, 9F02, 9F6E, 90]
     */
    fun readTagList(tags: String): List<String> {
        val tagList = mutableListOf<String>()
        var cursor = 0
        while (cursor < tags.length) {
            var tmpCursor = cursor
            var currentByte = tags.substring(cursor, cursor + 2).hexToBinary()
            if (currentByte.endsWith("11111")) {
                tmpCursor += 2
                currentByte = tags.substring(tmpCursor, tmpCursor + 2).hexToBinary()
                while (currentByte.startsWith("1")) {
                    tmpCursor += 2
                    currentByte = tags.substring(tmpCursor, tmpCursor + 2).hexToBinary()
                }
                Log.d("readTagList", "Tag: ${tags.substring(cursor, tmpCursor + 2)}")
                tagList.add(tags.substring(cursor, tmpCursor + 2))
                cursor = tmpCursor + 2
            } else {
                Log.d("readTagList", "Tag: ${tags.substring(cursor, cursor + 2)}")
                tagList.add(tags.substring(cursor, cursor + 2))
                cursor += 2
            }
        }
        return tagList
    }

    /**
     * Separate DOL (tag-length-tag-length...) into a map of <tag, length>
     * Eg. input dol = 9F02069F03069F3704
     * return {"9F02":"06", "9F03":"06", "9F37":"04"}
     */
    fun readDOL(dol: String): Map<String, String> {
        val dolMap = mutableMapOf<String, String>()
        var cursor = 0
        while (cursor < dol.length) {
            var tmpCursor = cursor
            var currentByte = dol.substring(cursor, cursor + 2).hexToBinary()
            if (currentByte.endsWith("11111")) {
                tmpCursor += 2
                currentByte = dol.substring(tmpCursor, tmpCursor + 2).hexToBinary()
                while (currentByte.startsWith("1")) {
                    tmpCursor += 2
                    currentByte = dol.substring(tmpCursor, tmpCursor + 2).hexToBinary()
                }
                dolMap[dol.substring(cursor, tmpCursor + 2)] = dol.substring(tmpCursor + 2, tmpCursor + 4)
                cursor = tmpCursor + 4
            } else {
                Log.d("readTagList", "Tag: ${dol.substring(cursor, cursor + 2)}")
                dolMap[dol.substring(cursor, cursor + 2)] = dol.substring(cursor + 2, cursor + 4)
                cursor += 4
            }
        }
        return dolMap
    }
}