package com.example.myapplication

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.lang.Boolean
import kotlin.Any
import kotlin.ByteArray
import kotlin.IntArray
import kotlin.Result
import kotlin.also


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraView()
                }
            }
        }
    }
}


@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun CameraView() {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var textQR by remember { mutableStateOf("") }

    var clickable by remember { mutableStateOf(true) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder().build()
    }

    val qrCodeAnalyzer = remember {
        QRCodeAnalyzer { it, count ->
            println(count)
            val maxCount = 10
            var hText = ""
            if (count >= maxCount) {
                hText = "Barcode not recognized!"
            } else {
                hText = it
            }

            println(hText)
            textQR = hText

            if (count >= maxCount || it.isNotEmpty()) {
                imageAnalysis.clearAnalyzer()
                clickable = true
            }
        }
    }

    val previewView = remember { PreviewView(context) }



//    LaunchedEffect(lensFacing) {
//        val cameraProvider = cameraProviderFuture.get()
//        cameraProvider.unbindAll()
//
//        cameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            cameraSelector,
//            preview,
//            imageCapture
//        )
//
//        preview.setSurfaceProvider(previewView.surfaceProvider)
//    }



    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                preview.setSurfaceProvider(previewView.surfaceProvider)

                ProcessCameraProvider.getInstance(context).get().bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                previewView
            }
        )

        Text(
            textQR, modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
                .height(100.dp),
            color = Color.White
        )

        IconButton(
            enabled = clickable,
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = {
                textQR = ""
                clickable = false
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), qrCodeAnalyzer)
            },
            content = {
                Icon(
                    imageVector = Icons.Sharp.Check,
                    contentDescription = "Take picture",
                    tint = Color.White,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(1.dp)
                )
            }
        )
    }
}