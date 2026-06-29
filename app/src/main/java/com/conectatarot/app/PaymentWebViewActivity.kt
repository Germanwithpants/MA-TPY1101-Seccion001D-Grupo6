package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class PaymentWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_webview)

        val sesionId = intent.getIntExtra("sesionId", 0)
        val paymentUrl = intent.getStringExtra("paymentUrl") ?: ""
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        webView = findViewById(R.id.webViewPago)
        val progress = findViewById<ProgressBar>(R.id.progressPago)
        val btnCerrar = findViewById<ImageButton>(R.id.btnCerrarPago)

        btnCerrar.setOnClickListener { finish() }

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false // let all URLs load in the WebView so the backend can process the payment
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    progress.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                    // Only trigger after our backend processes the return URL, not on the initial Transbank page
                    if (url != null && url.contains("/api/pagos/confirmar")) {
                        checkPaymentAndClose(token, sesionId)
                    }
                }
            }

            loadUrl(paymentUrl)
        }
    }

    private fun checkPaymentAndClose(token: String, sesionId: Int) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.estadoPago(sesionId)
                val estado = resp.body()?.estadoPago ?: "DESCONOCIDO"
                val msg = when (estado) {
                    "PAGADO" -> "✅ Pago realizado con éxito"
                    "FALLIDO" -> "❌ El pago falló. Inténtalo nuevamente."
                    else -> "Pago procesado. Verifica el estado de tu sesión."
                }
                Toast.makeText(this@PaymentWebViewActivity, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@PaymentWebViewActivity, "Verifica el estado de tu sesión", Toast.LENGTH_SHORT).show()
            }
            setResult(RESULT_OK, Intent().putExtra("sesionId", sesionId))
            finish()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
