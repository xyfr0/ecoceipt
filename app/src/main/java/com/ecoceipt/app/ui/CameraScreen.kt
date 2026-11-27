package com.ecoceipt.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.io.File
import com.ecoceipt.app.R

@Composable fun CameraScreen(busy: Boolean, openGallery: Boolean, scanned: (List<Uri>) -> Unit, back: () -> Unit, error: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var taking by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }
    val queuedUris = remember { mutableStateListOf<Uri>() }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            if (batchMode) queuedUris.addAll(uris) else scanned(uris)
        }
    }
    LaunchedEffect(Unit) {
        if (openGallery) gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else if (!granted) {
            permissionRequested = true
            requestPermission.launch(Manifest.permission.CAMERA)
        }
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
            if (granted && cameraError == null) ScanGuide(false, Modifier.align(Alignment.Center))
            if (!granted || cameraError != null) Column(
                Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScanGuide(true)
                Spacer(Modifier.height(26.dp))
                Text(cameraError ?: "Izin akses kamera dibutuhkan untuk memindai nota",
                    color = Color.White, fontSize = 19.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(36.dp))
                if (!granted) {
                    Action("Beri Izin Akses") {
                        val activity = context as? Activity
                        if (permissionRequested && activity != null &&
                            !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")))
                        } else {
                            permissionRequested = true
                            requestPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Action("Pilih dari Galeri", secondary = true) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            TextButton(back, Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp)
                .background(Ink.copy(alpha = 0.85f), RoundedCornerShape(24.dp)), enabled = !taking && !busy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                Icon(painterResource(R.drawable.ic_back), null, Modifier.size(24.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Label("Kembali", 15, Color.White)
            }
            TextButton({ batchMode = !batchMode }, Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp)
                .background(if (batchMode) Teal else Ink.copy(alpha = 0.85f), RoundedCornerShape(24.dp)),
                enabled = !taking && !busy, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                Label(if (batchMode) "Banyak Nota ✓" else "Banyak Nota", 14, Color.White)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_camera_batch), null, Modifier.size(22.dp), tint = Color.White)
            }
            if (queuedUris.isNotEmpty()) {
                TextButton({
                    val batch = queuedUris.toList()
                    queuedUris.clear()
                    scanned(batch)
                }, Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .background(Teal, RoundedCornerShape(24.dp)), enabled = !taking && !busy,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Label("Proses ${queuedUris.size} Nota ›", 13, Color.White, true)
                }
            }
            if (granted && cameraError == null && queuedUris.isEmpty()) Box(Modifier.align(Alignment.BottomCenter)
                .padding(16.dp).background(Ink.copy(alpha = 0.85f), Round).padding(12.dp)) {
                Label(if (batchMode) "Ambil beberapa foto nota lalu tekan Proses" else "Pastikan seluruh nota terlihat dan teks terbaca", 12, Color.White)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, enabled = granted && !busy && !taking, modifier = Modifier.weight(1f).heightIn(min = 100.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_camera_gallery), null, Modifier.size(36.dp),
                        tint = if (granted && !busy && !taking) Color.White else Muted)
                    Label("Galeri", 12, if (granted && !busy && !taking) Color.White else Muted, true)
                }
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
                                    taking = false
                                    val uri = Uri.fromFile(photo)
                                    if (batchMode) queuedUris.add(uri) else scanned(listOf(uri))
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
                modifier = Modifier.weight(1f).heightIn(min = 100.dp)) {
                val flashColor = if (!hasFlash || busy || taking) Muted else Color.White
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(if (flash) R.drawable.ic_camera_flash_on else R.drawable.ic_camera_flash_off),
                        null, Modifier.size(36.dp), tint = flashColor)
                    Label(if (!hasFlash) "Tanpa flash" else if (flash) "Flash aktif" else "Flash mati", 12,
                        flashColor, true)
                }
            }
        }
    }
}

@Composable private fun ScanGuide(unavailable: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(240.dp).background(if (unavailable) Color(0xFF202728) else Color.Transparent, Round),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val length = 32.dp.toPx()
            val inset = 4.dp.toPx()
            val right = size.width - inset
            val bottom = size.height - inset
            val corners = listOf(
                Offset(inset, inset + length) to Offset(inset, inset),
                Offset(inset, inset) to Offset(inset + length, inset),
                Offset(right - length, inset) to Offset(right, inset),
                Offset(right, inset) to Offset(right, inset + length),
                Offset(inset, bottom - length) to Offset(inset, bottom),
                Offset(inset, bottom) to Offset(inset + length, bottom),
                Offset(right - length, bottom) to Offset(right, bottom),
                Offset(right, bottom - length) to Offset(right, bottom)
            )
            corners.forEach { (start, end) ->
                drawLine(Color(0x5533C9B7), start, end, 11.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Color(0xFFE7FFFA), start, end, 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        if (unavailable) Icon(painterResource(R.drawable.ic_camera_unavailable),
            contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.Unspecified)
    }
}
