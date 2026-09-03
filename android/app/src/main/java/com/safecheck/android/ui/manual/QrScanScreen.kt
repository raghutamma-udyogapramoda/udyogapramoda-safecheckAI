package com.safecheck.android.ui.manual

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.safecheck.android.data.extract.QrAnalyzer
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.theme.NeutralMuted
import java.util.concurrent.Executors

/**
 * QR scanning screen (R-3.3). Decodes on-device; the decoded payload is handed back via
 * [onDecoded] and never auto-opened. If camera permission is denied, a manual paste
 * fallback keeps the journey usable (AC-3.3.3).
 */
@Composable
fun QrScanScreen(
    onDecoded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text("Scan QR", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "The QR is decoded on your device. SafeCheck never opens the link — it only checks it.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )
        Spacer(Modifier.height(16.dp))

        if (hasPermission) {
            CameraPreview(
                onDecoded = onDecoded,
                modifier = Modifier.fillMaxWidth().height(320.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Point your camera at the QR code.", style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
        } else {
            StatusBanner(
                "Camera permission is needed to scan a QR. You can grant it, or paste the QR content below.",
                kind = BannerKind.WARNING,
            )
            Spacer(Modifier.height(12.dp))
            SafeCheckButton(
                text = "Grant camera permission",
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
            Spacer(Modifier.height(20.dp))
            ManualQrFallback(onDecoded)
        }
    }
}

@Composable
private fun ManualQrFallback(onDecoded: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Text("Or paste QR content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text("Decoded QR text / URL") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    SafeCheckButton(
        text = "Check this",
        onClick = { onDecoded(value) },
        enabled = value.isNotBlank(),
    )
}

@Composable
private fun CameraPreview(
    onDecoded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, QrAnalyzer(onDecoded)) }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                    // If binding fails, the manual fallback remains available upstream.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
