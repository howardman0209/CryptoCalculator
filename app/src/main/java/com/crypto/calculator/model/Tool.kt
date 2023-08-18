package com.crypto.calculator.model

import com.crypto.calculator.R

enum class Tool(val id: Int, val resourceId: Int) {
    UNKNOWN(0, R.string.label_unknown),
    DES(1, R.string.label_tool_des),
    RSA(2, R.string.label_tool_rsa),
    AES(3, R.string.label_tool_aes),
    MAC(4, R.string.label_tool_mac),
    HASH(5, R.string.label_tool_hash),
    BITWISE(6, R.string.label_tool_bitwise),
    CONVERTER(7, R.string.label_tool_converter),
    TLV_PARSER(8, R.string.label_tool_tlv_parser),
    CARD_SIMULATOR(9, R.string.label_tool_card_simulator),
    EMV_KERNEL(10, R.string.label_tool_emv_kernel),
    ;

    companion object {
        fun getById(id: Int): Tool {
            return when (id) {
                1 -> DES
                2 -> RSA
                3 -> AES
                4 -> MAC
                5 -> HASH
                6 -> BITWISE
                7 -> CONVERTER
                8 -> TLV_PARSER
                9 -> CARD_SIMULATOR
                10 -> EMV_KERNEL
                else -> UNKNOWN
            }
        }
    }
}