package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PaymentWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var sesionId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_webview)

        sesionId = intent.getIntExtra("sesionId", 0)
        val paymentUrl = intent.getStringExtra("paymentUrl") ?: ""
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        webView = findViewById(R.id.webViewPago)
        val progress = findViewById<ProgressBar>(R.id.progressPago)
        val btnCerrar = findViewById<ImageButton>(R.id.btnCerrarPago)

        btnCerrar.setOnClickListener { finishWithRefresh() }

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    progress.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                    if (url != null && url.contains("/api/pagos/confirmar")) {
                        Toast.makeText(this@PaymentWebViewActivity, "Procesando pago...", Toast.LENGTH_SHORT).show()
                        finishWithRefresh()
                    }
                }
            }

            loadUrl(paymentUrl)
        }
    }

    private fun finishWithRefresh() {
        setResult(RESULT_OK, Intent().putExtra("sesionId", sesionId))
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else finishWithRefresh()
    }
}
