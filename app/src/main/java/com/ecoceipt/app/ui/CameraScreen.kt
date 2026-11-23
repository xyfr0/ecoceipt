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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.CircularProgressIndicator
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
import java.io.File

@Composable fun CameraScreen(busy: Boolean, openGallery: Boolean, scanned: (Uri) -> Unit, back: () -> Unit, error: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var taking by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
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
                val camera = provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
                flash = false
                capture = imageCapture
                cameraError = null
            } catch (e: Exception) { cameraError = "Kamera tidak tersedia. Pilih gambar dari galeri." }
        }, executor)
        onDispose { active = false; provider?.unbindAll(); capture = null }
    }
    Column(Modifier.fillMaxSize().background(Ink)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (granted) AndroidView({ previewView }, Modifier.fillMaxSize())
            if (!granted || cameraError != null) Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Label(cameraError ?: "Izinkan kamera untuk memotret nota, atau pilih gambar dari galeri.", color = Color.White)
                if (!granted) Action("Izinkan Kamera") { requestPermission.launch(Manifest.permission.CAMERA) }
                Action("Pilih dari Galeri", secondary = true) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            TextButton(back, Modifier.align(Alignment.TopStart).padding(16.dp)
                .background(Ink.copy(alpha = 0.85f), RoundedCornerShape(24.dp)), enabled = !taking && !busy,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                Label("‹  Kembali", 14, Color.White, true)
            }
            if (granted && cameraError == null) Box(Modifier.align(Alignment.BottomCenter)
                .padding(16.dp).background(Ink.copy(alpha = 0.85f), Round).padding(12.dp)) {
                Label("Pastikan seluruh nota terlihat dan teks terbaca", 12, Color.White)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, enabled = !busy && !taking, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Label("Galeri", 14, if (!busy && !taking) Color.White else Muted, true)
            }
            Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val ready = capture != null && !busy && !taking
                Box(Modifier.size(80.dp).clip(CircleShape)
                    .border(3.dp, if (ready) Color.White else Muted, CircleShape)
                    .clickable(enabled = ready, role = Role.Button, onClickLabel = "Ambil foto nota") {
                        val imageCapture = capture ?: return@clickable
                        taking = true
                        val photo = runCatching { File.createTempFile("receipt-", ".jpg", context.cacheDir) }
                            .getOrElse { taking = false; error("Foto gagal disiapkan. Periksa ruang penyimpanan."); return@clickable }
                        imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(photo).build(), executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    taking = false; scanned(Uri.fromFile(photo))
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    taking = false; photo.delete(); error("Foto gagal diambil. Coba lagi atau pilih dari galeri.")
                                }
                            })
                    }.padding(7.dp).background(if (ready) Color.White else LightGray, CircleShape),
                    contentAlignment = Alignment.Center) {
                    if (taking) CircularProgressIndicator(Modifier.size(28.dp), color = DarkTeal, strokeWidth = 3.dp)
                }
                Label(if (taking) "Memotret…" else "Ambil Foto", 12, Color.White, true)
            }
            TextButton(onClick = {
                flash = !flash
                capture?.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            }, enabled = capture != null && hasFlash && !busy && !taking,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Label(if (!hasFlash) "Tanpa flash" else if (flash) "Flash aktif" else "Flash mati", 12,
                    if (!hasFlash || busy || taking) Muted else if (flash) Mint else Color.White, true)
            }
        }
    }
}
