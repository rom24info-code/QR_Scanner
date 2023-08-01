package com.example.myapplication

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzerML(
    private val errorFun: (String) -> Unit,
    private val successFun: (String) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        var maxCount = 50
    }

    private var barcodList = mutableListOf<String>()

    private fun checkBarcodes(listBarcodes: MutableList<String>): Boolean {
        if (listBarcodes.isNotEmpty()) {
            val mapBarcodes = listBarcodes.groupBy { it }
            val groupGreatOne = mapBarcodes.values.find { it.count() > 1 && it.first() != ""}
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
                            successFun(byteArrayBarcode.decodeToString())
                            barcodList.clear()
                        }
                    }
                } else {
                    barcodList.add("")
                }
            }.addOnCompleteListener {
                image.close()
            }
        }

        if (barcodList.count() >= maxCount) {
            errorFun("Штрихкод не распознан!!!")
            barcodList.clear()
        }
    }
}