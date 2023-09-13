package com.crypto.calculator.cardReader.bluetooth


class FTBlueReadException : Exception {
    constructor() : super()
    constructor(msg: String?) : super(msg)
    constructor(throwable: Throwable?) : super(throwable)
    constructor(detailMessage: String?, throwable: Throwable?) : super(detailMessage, throwable)
}
