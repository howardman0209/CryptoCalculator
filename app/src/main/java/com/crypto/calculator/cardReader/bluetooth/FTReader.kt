package com.crypto.calculator.cardReader.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.util.Log
import com.crypto.calculator.model.EmvConfig
import com.feitian.reader.devicecontrol.Card
import com.feitian.readerdk.Tool.DK
import java.util.UUID


class FTReader(context: Context, device: BluetoothDevice) : BluetoothReader(context, device) {
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var isPowerOn = false
    private val innerCard: Card
    private var mHandler: Handler? = null

    init {
        val inputStream = getBluetoothSocketClass(uuid).inputStream
        val outputStream = getBluetoothSocketClass(uuid).outputStream
        innerCard = Card(inputStream, outputStream)
    }

    @Throws(FTBlueReadException::class)
    fun powerOn(): Int {
        val ret = innerCard.PowerOn()
        if (ret != DK.RETURN_SUCCESS) {
            throw FTBlueReadException("Power On Failed")
        }
        isPowerOn = true
        return DK.RETURN_SUCCESS
    }

    @Throws(FTBlueReadException::class)
    fun powerOff(): Int {
        val ret = innerCard.PowerOff()
        if (ret != DK.RETURN_SUCCESS) {
            throw FTBlueReadException("Power Off Failed")
        }
        isPowerOn = false
        return DK.RETURN_SUCCESS
    }

    @get:Throws(FTBlueReadException::class)
    val protocol: Int
        get() {
            if (!isPowerOn) {
                throw FTBlueReadException("Power Off already")
            }
            return innerCard.protocol
        }

    @get:Throws(FTBlueReadException::class)
    val atr: ByteArray
        get() {
            if (!isPowerOn) {
                throw FTBlueReadException("Power Off already")
            }
            return innerCard.atr
        }

    fun getVersion(recvBuf: ByteArray?, recvBufLen: IntArray?): Int {
        return innerCard.getVersion(recvBuf, recvBufLen)
    }

    val dkVersion: String = innerCard.GetDkVersion()

    @get:Throws(FTBlueReadException::class)
    val cardStatus: Int = innerCard.getcardStatus()

    fun getSerialNum(serial: ByteArray?, serialLen: IntArray?): Int {
        return innerCard.FtGetSerialNum(serial, serialLen)
    }

    fun readFlash(buf: ByteArray?, offset: Int, len: Int): Int {
        return innerCard.FtReadFlash(buf, offset, len)
    }

    fun writeFlash(buf: ByteArray?, offset: Int, len: Int): Int {
        return innerCard.FtWriteFlash(buf, offset, len)
    }

    //To minitor card slot status
    //new api for monitor the card status
    //#1 register card status monitoring 
    @Throws(FTBlueReadException::class)
    fun registerCardStatusMonitoring(handler: Handler?) {
        mHandler = handler
        if (innerCard.registerCardStatusMonitoring(handler) != DK.RETURN_SUCCESS) {
            throw FTBlueReadException("not support cardStatusMonitoring")
        }
    }

    @Throws(FTBlueReadException::class)
    fun transApdu(
        txLength: Int, txBuffer: ByteArray?,
        rxLength: IntArray?, rxBuffer: ByteArray?
    ): Int {
        if (!isPowerOn) {
            throw FTBlueReadException("Power Off already")
        }

        return when (innerCard.transApdu(txLength, txBuffer, rxLength, rxBuffer)) {
            DK.RETURN_SUCCESS -> DK.RETURN_SUCCESS
            DK.BUFFER_NOT_ENOUGH -> throw FTBlueReadException("receive buffer not enough")
            DK.CARD_ABSENT -> {
                mHandler!!.obtainMessage(
                    DK.CARD_STATUS,
                    DK.CARD_ABSENT, -1
                ).sendToTarget()
                throw FTBlueReadException("card is absent")
            }

            else -> throw FTBlueReadException("trans apdu error")
        }
    }

    private fun readerClose() {
        innerCard.cardClose()
    }

    override fun init() {
        powerOn()
    }

    override fun release() {}

    override fun disconnect() {
        readerClose()
    }

    override fun connect() {
        innerCard.open()
    }

    override fun initSetting() {
        Log.d("FTReader", "initSetting")
    }

    override fun startEMV(authorizedAmount: String?, cashbackAmount: String?, emvConfig: EmvConfig) {
        Log.d("FTReader", "startEMV")
    }

    override fun cancelCheckCard() {
        Log.d("FTReader", "cancelCheckCard")
    }

    override fun sendOnlineReply(replyTLV: String?) {
        Log.d("FTReader", "sendOnlineReply")
    }

    override fun pollCardRemove() {
        Log.d("FTReader", "pollCardRemove")
    }
}
