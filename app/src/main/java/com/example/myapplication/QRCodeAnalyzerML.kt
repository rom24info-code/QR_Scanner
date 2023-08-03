package com.example.myapplication

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.Socket




class QRCodeAnalyzerML(
    private val errorFun: (String) -> Unit,
    private val successFun: (String) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        var maxCount = 50
    }

    private var barcodeList = mutableListOf<ByteArray>()

    private fun checkBarcodes(listBarcodes: MutableList<ByteArray>): Boolean {
        if (listBarcodes.isNotEmpty()) {
            val mapBarcodes = listBarcodes.groupBy { it.decodeToString() }
            val groupGreatOne = mapBarcodes.values.find { it.count() > 1 && it.first().size != 0}
            if (groupGreatOne!= null && groupGreatOne.count() >= 3)
                return true
        }
        return false
    }

    private fun getDecodedBarcode(listBarcodes: List<ByteArray>): ByteArray {
        if (listBarcodes.isNotEmpty()) {
            val mapBarcodes = listBarcodes.groupBy { it.decodeToString() }
            val groupGreatOne = mapBarcodes.values.find { it.count() > 1 && it.first().size != 0}
            if (groupGreatOne!= null && groupGreatOne.count() >= 3)
                return groupGreatOne.first()
        }
        return byteArrayOf()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image

        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val byteArrayBarcode = barcodes.first()?.rawValue?.toByteArray()

                        if (byteArrayBarcode != null) {
                            barcodeList.add(byteArrayBarcode)

                            if (checkBarcodes(barcodeList)) {
                                GlobalScope.launch(Dispatchers.IO) {
                                    barcodeSuccess(
                                        barcodeList,
                                        successFun
                                    )
                                }
                            }
                        }
                    } else {
                        barcodeList.add(byteArrayOf())
                    }
                }.addOnFailureListener {
                    val a = ""
                }.addOnCompleteListener {
                    image.close()
                }
        }

        if (barcodeList.count() >= maxCount) {
            errorFun("Штрихкод не распознан!!!")
            barcodeList.clear()
        }
    }

    private suspend fun barcodeSuccess(
        barcodeList: MutableList<ByteArray>,
        successFun: (String) -> Unit
    ) {
        successFun(getDecodedBarcode(barcodeList).decodeToString())
        barcodeList.clear()

        try {
            val mSocket = Socket("192.168.11.106", 8080)
            mSocket.getOutputStream().write(getDecodedBarcode(barcodeList))
            mSocket.close()

        } catch (e: Exception) {

        }

    }
}

