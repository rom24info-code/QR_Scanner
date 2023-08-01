package com.example.myapplication

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzerML(
    private val urlCallback: (String, Int) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        var maxCount = 300
    }

    private var barcodList = mutableListOf<String>()

    private fun checkBarcodes(listBarcodes: MutableList<String>): Boolean {
        if (listBarcodes.isNotEmpty()) {
            val mapBarcodes = listBarcodes.groupBy { it }
            val groupGreatOne = mapBarcodes.values.find { it.count() > 1 && it[0] != "no barcode" }
            if (groupGreatOne!= null && groupGreatOne.count() >= 3)
                return true
        }

        return false
    }
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image

        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(inputImage).addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val byteArrayBarcode = barcodes.first()?.rawValue?.toByteArray()

                        if (byteArrayBarcode != null) {
                            barcodList.add(byteArrayBarcode.decodeToString())

                            if (checkBarcodes(barcodList)) {
                                urlCallback(byteArrayBarcode.decodeToString(), 0)
                                barcodList.clear()
                            }

                        } else {
                            urlCallback("", barcodList.count())
                        }
                    } else {
                        barcodList.add("no barcode")
                        urlCallback("", barcodList.count())
                    }
                    image.close()
                }.addOnFailureListener {
                    urlCallback("", barcodList.count())
                    image.close()
                }.addOnCanceledListener {
                    urlCallback("", barcodList.count())
                    image.close()
                }
        }
        if (barcodList.count() >= 50) {
            barcodList.clear()
        }
    }
}