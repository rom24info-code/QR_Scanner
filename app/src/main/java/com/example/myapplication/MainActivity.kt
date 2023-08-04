package com.example.myapplication

import android.media.CamcorderProfile
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme


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



@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun CameraView() {

    val camProfile by lazy {
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_LOW)) {
            CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
        } else {
            null
        }
    }



    if (camProfile == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Сamera not initialized!!!",
                modifier = Modifier
                    .align(Alignment.Center),
                color = Color.Red,
                fontSize = 30.sp
            )
        }
        return
    }

    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var textQR by remember { mutableStateOf("") }
    var colorText by remember { mutableStateOf(Color.Green) }
    var clickable by remember { mutableStateOf(true) }

    val mPlayerGood by remember {
        mutableStateOf(MediaPlayer.create(context, R.raw.scangood))
    }
    val mPlayerErr by remember {
        mutableStateOf(MediaPlayer.create(context, R.raw.scanerror))
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setTargetResolution(
                Size(
                    camProfile!!.videoFrameHeight,
                camProfile!!.videoFrameWidth)
            )
            .build()
    }



    val qrCodeAnalyzer = remember {
        QRCodeAnalyzerML(
            imageAnalysis = imageAnalysis,
            errorFun = {

            colorText = Color.Red
            textQR = it
            clickable = true

            mPlayerErr.start()
        }) {
            colorText = Color.Green
            textQR = it
            clickable = true

            mPlayerGood.start()
        }
    }

    val previewView = remember { PreviewView(context) }


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
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(top = 30.dp, start = 10.dp, end = 10.dp)
                .height(100.dp),
            color = colorText,
            textAlign = TextAlign.Center
        )

        if (!clickable) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp),
                color = Color.Magenta,
                strokeWidth = 10.dp
            )
        }

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