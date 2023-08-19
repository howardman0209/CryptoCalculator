package com.crypto.calculator.model
enum class PaymentMethod(val id: Int, val officialName: String) {
    UNKNOWN(1, "N/A"),
    CASH(2, "Cash"),
    WECHAT(3, "WeChat Pay"),
    ALIPAY(4, "Alipay"),
    VISA(5, "Visa"),
    MASTER(6, "Mastercard"),
    AMEX(7, "American Express"),
    JCB(8, "JCB"),
    UNIONPAY(9, "UnionPay"),
    DINERS(10, "Diners"),
    DISCOVER(11, "Discover"),
    VERVE(12, "Verve"),
    FPS(13, "FPS"),
    OCTOPUS(14, "OCTOPUS"),
    UPI_QR(15, "UPI_QR"),
    EPS(16, "EPS"),
    ZELLE(17, "ZELLE"),
    MAESTRO(18, "Maestro"),
    EASYCARD(19, "EasyCard"),
    PLC(20, "Private Label Card"),
    EASYCARD_QR(21, "EasyCard_QR"),
    YUU(22, "YUU"),
    PAYME(23, "PayMe"),
    VISA_ELECTRON(24, "Visa Electron"),
    PAYNOW(25, "PayNow");

    companion object {
        private val lookup: MutableMap<Int, PaymentMethod> = HashMap()

        init {
            for (d in PaymentMethod.values()) {
                lookup[d.id] = d
            }
        }

        fun getById(id: Int): PaymentMethod {
            val e = lookup[id]
            return e ?: UNKNOWN
        }
    }
}
