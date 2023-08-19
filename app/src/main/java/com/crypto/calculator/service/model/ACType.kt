package com.crypto.calculator.service.model

object ApplicationCryptogram{
    enum class Type {
        ARQC,
        AAC,
        TC
    }

    fun getCryptogramInformationData(type: Type): String {
        return when (type) {
            Type.TC -> "40"
            Type.AAC -> "00"
            Type.ARQC -> "80"
        }
    }
}
