package com.example.myapplication

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket


class QRCodeAnalyzerML(
    private val imageAnalysis: ImageAnalysis,
    private val errorFun: (String) -> Unit,
    private val successFun: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var countScan = 1
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
//                                GlobalScope.launch(Dispatchers.IO) {
                                    barcodeSuccess(
                                        barcodeList
                                    )
//                                }
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
            imageAnalysis.clearAnalyzer()
        }
    }


    private fun barcodeSuccess(
        barcodeList: MutableList<ByteArray>,
    ) {
        imageAnalysis.clearAnalyzer()
        val barcode = getDecodedBarcode(barcodeList).decodeToString()
        barcodeList.clear()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val mSocket = Socket("192.168.0.25", 8080)

                val bWriter = BufferedWriter(OutputStreamWriter(mSocket.getOutputStream()))
                bWriter.write(barcode + "\n")
                bWriter.flush()

                val bReader = BufferedReader(InputStreamReader(mSocket.getInputStream()))
                val rezult = bReader.readLine()
                if (rezult == null || rezult.uppercase() != "OK")
                    throw IllegalArgumentException("Сбой виртуальной клавиатуры!!!")

                mSocket.close()

                successFun(barcode)

            } catch (e: Exception) {
                errorFun(e.stackTraceToString())
            }
        }

    }
}

