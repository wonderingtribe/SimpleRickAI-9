package com.simplerick

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PermissionScreen(
                    onStartService = { startFloatingService() }
                )
            }
        }
    }

    private fun startFloatingService() {
        // Request overlay permission if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)) {

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val intent = Intent(this, FloatingAssistantService::class.java)
        startService(intent)
    }
}

@Composable
fun PermissionScreen(onStartService: () -> Unit) {
    var granted by remember { mutableStateOf(false) }

    granted = Settings.canDrawOverlays(LocalContext.current)

    Surface {
        Button(
            onClick = onStartService,
            enabled = true
        ) {
            Text(text = "Start Floating Assistant")
        }
    }
}

@Preview
@Composable
fun PreviewPermission() {
    PermissionScreen({})
}
