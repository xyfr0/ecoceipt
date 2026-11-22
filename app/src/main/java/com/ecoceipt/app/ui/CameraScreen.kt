package com.ecoceipt.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import java.io.File

@Composable fun CameraScreen(busy: Boolean, openGallery: Boolean, scanned: (Uri) -> Unit, back: () -> Unit, error: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var taking by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scanned(it)
        }
    }
    LaunchedEffect(Unit) {
        if (openGallery) gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else if (!granted) requestPermission.launch(Manifest.permission.CAMERA)
    }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(granted, lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        var active = true
        val future = if (granted) ProcessCameraProvider.getInstance(context) else null
        future?.addListener({
            if (active) try {
                provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                capture = imageCapture
                cameraError = null
            } catch (e: Exception) { cameraError = "Kamera tidak tersedia. Pilih gambar dari galeri." }
        }, executor)
        onDispose { active = false; provider?.unbindAll(); capture = null }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) AndroidView({ previewView }, Modifier.fillMaxSize())
        if (!granted || cameraError != null) Column(Modifier.align(Alignment.Center).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label(cameraError ?: "Izinkan kamera untuk memotret nota, atau pilih gambar dari galeri.", color = Color.White)
            if (!granted) Action("Izinkan Kamera") { requestPermission.launch(Manifest.permission.CAMERA) }
            Action("Pilih dari Galeri", secondary = true) { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }
        TextButton(back, Modifier.align(Alignment.TopStart)) { Label("‹ Kembali", color = Color.White) }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(175.dp).background(Color.White.copy(alpha = 0.27f)).padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage("file:///android_asset/figma/eb40c.svg", "Pilih dari galeri", Modifier.size(40.dp).clickable(enabled = !busy && !taking) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                Label("Galeri", 11, Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage("file:///android_asset/figma/60c30.svg", "Ambil foto nota", Modifier.size(90.dp).clickable(enabled = capture != null && !busy && !taking) {
                    taking = true
                    val photo = File.createTempFile("receipt-", ".jpg", context.cacheDir)
                    capture?.takePicture(ImageCapture.OutputFileOptions.Builder(photo).build(), executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                taking = false; scanned(Uri.fromFile(photo))
                            }
                            override fun onError(exception: ImageCaptureException) {
                                taking = false; photo.delete(); error("Foto gagal diambil. Coba lagi atau pilih dari galeri.")
                            }
                        })
                })
                Label(if (taking) "Memotret…" else "Ambil Foto", 11, Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                var flash by remember { mutableStateOf(false) }
                AsyncImage("file:///android_asset/figma/d2a24.svg", "Lampu kilat", Modifier.size(70.dp).clickable(enabled = capture != null) {
                    flash = !flash; capture?.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                })
                Label(if (flash) "Flash aktif" else "Flash mati", 11, Color.White)
            }
        }
    }
}
