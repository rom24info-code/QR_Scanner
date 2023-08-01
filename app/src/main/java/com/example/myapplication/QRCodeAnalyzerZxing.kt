package com.example.myapplication

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QRCodeAnalyzerZxing(
    private val urlCallback: (String, Int) -> Unit
) : ImageAnalysis.Analyzer  {

    private var count = 0

    companion object {
        var maxCount = 50
    }


    override fun analyze(image: ImageProxy) {
        count ++

        val bytes = image.planes.first().buffer.toByteArray()

        val source = PlanarYUVLuminanceSource(
            bytes,
            image.width,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false
        )


        val binaryBmp = BinaryBitmap(HybridBinarizer(source))
        val multiFormatReader = MultiFormatReader()
//        multiFormatReader.setHints (mapOf(
//                DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.EAN_13)
//            )
//        )

        try {
            val result = multiFormatReader.decode(binaryBmp)
            urlCallback(result.text, 0)
            count = 0
            return

        } catch (e: Exception) {
            println(e.localizedMessage)
        } finally {
            image.close()
        }

        if (count >= maxCount) {
            count = 0
        }

        urlCallback("", count)
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val byteArray = ByteArray(remaining())
    get(byteArray)
    return byteArray
}