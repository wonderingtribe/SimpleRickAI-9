package com.simplerick.ai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: Button = findViewById(R.id.btn_start_service)
        val stopButton: Button = findViewById(R.id.btn_stop_service)

        startButton.setOnClickListener {
            if (checkOverlayPermission()) {
                startFloatingService()
            } else {
                requestOverlayPermission()
            }
        }

        stopButton.setOnClickListener {
            stopFloatingService()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (checkOverlayPermission()) {
                Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show()
                startFloatingService()
            } else {
                Toast.makeText(this, "Overlay permission denied. Cannot start Simple Rick AI.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startFloatingService() {
        if (!FloatingAssistantService.isRunning) {
            startService(Intent(this, FloatingAssistantService::class.java))
            Toast.makeText(this, "Simple Rick AI is running!", Toast.LENGTH_SHORT).show()
            finish() 
        } else {
            Toast.makeText(this, "Simple Rick AI is already active.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopFloatingService() {
        if (FloatingAssistantService.isRunning) {
            stopService(Intent(this, FloatingAssistantService::class.java))
            Toast.makeText(this, "Simple Rick AI stopped.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Simple Rick AI is not running.", Toast.LENGTH_SHORT).show()
        }
    }
}
