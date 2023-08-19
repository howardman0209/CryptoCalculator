package com.crypto.calculator.model


enum class EntryMode(val id: Int) {
    UNKNOWN(1),
    MANUAL(2),
    MAGSTRIPE(3),
    FALLBACK(4),
    CONTACTLESS(5),
    CHIP(6),
    QR_SCAN(7),
    QR_PRESENT(8),
    CASH(9),
    CONTACTLESS_MAGSTRIPE(10),
    QR_STATIC(11),
    OCTOPUS(12),
    ECOMMERCE(13);

    companion object {
        private val lookup: MutableMap<Int, EntryMode> = HashMap()

        init {
            for (d in EntryMode.values()) {
                lookup[d.id] = d
            }
        }

        fun getById(id: Int?): EntryMode {
            if (id != null) {
                val mode = lookup[id]
                if (mode != null) {
                    return mode
                }
            }
            return UNKNOWN
        }
    }
}

