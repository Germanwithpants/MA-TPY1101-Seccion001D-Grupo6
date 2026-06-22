package com.conectatarot.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class VideoCallActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val PERMISSIONS_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        val sesionId = intent.getIntExtra("sesionId", 0)
        val nombreSesion = "ConectaTarot-Sesion-$sesionId"

        webView = findViewById(R.id.webViewJitsi)
        val btnCerrar = findViewById<ImageButton>(R.id.btnCerrarLlamada)

        btnCerrar.setOnClickListener {
            webView.loadUrl("about:blank")
            finish()
        }

        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            launchJitsi(nombreSesion)
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSIONS_REQUEST)
        }
    }

    private fun launchJitsi(roomName: String) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                allowFileAccess = true
                userAgentString = settings.userAgentString + " ConectaTarotApp"
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    request.grant(request.resources)
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    findViewById<View>(R.id.progressVideoCall).visibility = View.GONE
                }
            }
            loadUrl("https://meet.jit.si/$roomName#config.startWithAudioMuted=false&config.startWithVideoMuted=false&config.prejoinPageEnabled=false&interfaceConfig.SHOW_JITSI_WATERMARK=false")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            launchJitsi("ConectaTarot-Sesion-${intent.getIntExtra("sesionId", 0)}")
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        webView.loadUrl("about:blank")
        super.onBackPressed()
    }
}
