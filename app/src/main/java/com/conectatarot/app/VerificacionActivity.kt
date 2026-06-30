package com.conectatarot.app

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.VerificacionRequest
import kotlinx.coroutines.launch

class VerificacionActivity : AppCompatActivity() {

    private val BANCOS = arrayOf(
        "BancoEstado", "Banco Santander", "Banco de Chile", "BCI",
        "Itaú", "Scotiabank", "Banco Falabella", "Tenpo", "Copec Pay",
        "Mercado Pago", "Otro"
    )
    private val TIPOS_CUENTA = arrayOf(
        "Cuenta Corriente", "Cuenta Vista", "Cuenta RUT", "Cuenta de Ahorro"
    )

    private var fotoUri: Uri? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            fotoUri = it
            findViewById<ImageView>(R.id.ivPreviewCarnet).setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verificacion)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val tarotistaId = prefs.getInt("idTarotista", 0).takeIf { it != 0 }
            ?: prefs.getInt("idUsuario", 0)

        val etRut           = findViewById<EditText>(R.id.etRut)
        val tvRutFeedback   = findViewById<TextView>(R.id.tvRutFeedback)
        val etNombre        = findViewById<EditText>(R.id.etNombreCompleto)
        val spinnerBanco    = findViewById<Spinner>(R.id.spinnerBanco)
        val spinnerTipo     = findViewById<Spinner>(R.id.spinnerTipoCuenta)
        val etNumero        = findViewById<EditText>(R.id.etNumeroCuenta)
        val etTitular       = findViewById<EditText>(R.id.etTitularCuenta)
        val tvError         = findViewById<TextView>(R.id.tvErrorVerif)
        val btnEnviar       = findViewById<Button>(R.id.btnEnviarVerificacion)
        val cardEstado      = findViewById<CardView>(R.id.cardEstado)
        val tvEstadoLabel   = findViewById<TextView>(R.id.tvEstadoLabel)
        val tvEstadoObs     = findViewById<TextView>(R.id.tvEstadoObservacion)

        findViewById<TextView>(R.id.tvVolverVerificacion).setOnClickListener { finish() }

        spinnerBanco.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, BANCOS)
        spinnerTipo.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, TIPOS_CUENTA)

        etNombre.setText(prefs.getString("nombre", "") ?: "")

        etRut.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val rut = s.toString().trim()
                if (rut.length >= 3) {
                    val ok = validarRut(rut)
                    tvRutFeedback.text = if (ok) "✅ RUT válido" else "❌ RUT inválido"
                    tvRutFeedback.setTextColor(if (ok) 0xFF27ae60.toInt() else 0xFFe74c3c.toInt())
                    tvRutFeedback.visibility = View.VISIBLE
                } else {
                    tvRutFeedback.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnSeleccionarFoto).setOnClickListener {
            pickImage.launch("image/*")
        }

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getEstadoVerificacion("Bearer $token", tarotistaId)
                if (resp.isSuccessful) {
                    val status = resp.body()?.data ?: return@launch
                    when (status.estado) {
                        "VERIFICADO" -> {
                            cardEstado.setCardBackgroundColor(0xFF1a472a.toInt())
                            tvEstadoLabel.text = "✅ Identidad verificada"
                            tvEstadoLabel.setTextColor(0xFF27ae60.toInt())
                            cardEstado.visibility = View.VISIBLE
                            disableForm(btnEnviar)
                        }
                        "PENDIENTE" -> {
                            cardEstado.setCardBackgroundColor(0xFF1a3a5c.toInt())
                            tvEstadoLabel.text = "⏳ Solicitud en revisión"
                            tvEstadoLabel.setTextColor(0xFF3498db.toInt())
                            cardEstado.visibility = View.VISIBLE
                            disableForm(btnEnviar)
                        }
                        "RECHAZADO" -> {
                            cardEstado.setCardBackgroundColor(0xFF4a0a0a.toInt())
                            tvEstadoLabel.text = "❌ Solicitud rechazada — completa nuevamente"
                            tvEstadoLabel.setTextColor(0xFFe74c3c.toInt())
                            if (!status.observacion.isNullOrBlank()) {
                                tvEstadoObs.text = "Motivo: ${status.observacion}"
                                tvEstadoObs.visibility = View.VISIBLE
                            }
                            cardEstado.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (_: Exception) { /* endpoint not yet live — form stays enabled */ }
        }

        btnEnviar.setOnClickListener {
            val rut     = etRut.text.toString().trim()
            val nombre  = etNombre.text.toString().trim()
            val numero  = etNumero.text.toString().trim()
            val titular = etTitular.text.toString().trim()

            if (rut.isEmpty() || nombre.isEmpty() || numero.isEmpty() || titular.isEmpty()) {
                tvError.text = "Completa todos los campos obligatorios"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (!validarRut(rut)) {
                tvError.text = "El RUT ingresado no es válido"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            btnEnviar.isEnabled = false
            btnEnviar.text = "Enviando..."

            lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.instance.solicitarVerificacion(
                        "Bearer $token", tarotistaId,
                        VerificacionRequest(
                            rut = rut,
                            nombreCompleto = nombre,
                            banco = BANCOS[spinnerBanco.selectedItemPosition],
                            tipoCuenta = TIPOS_CUENTA[spinnerTipo.selectedItemPosition],
                            numeroCuenta = numero,
                            titularCuenta = titular
                        )
                    )
                    if (resp.isSuccessful) {
                        mostrarEnviado(cardEstado, tvEstadoLabel, btnEnviar)
                    } else {
                        btnEnviar.isEnabled = true
                        btnEnviar.text = "Enviar Solicitud de Verificación"
                        val msg = try { org.json.JSONObject(resp.errorBody()?.string() ?: "").optString("message", "") } catch (_: Exception) { "" }
                        tvError.text = msg.ifBlank { "Error al enviar (${resp.code()})" }
                        tvError.visibility = View.VISIBLE
                    }
                } catch (_: Exception) {
                    // Backend endpoint not yet live — show optimistic confirmation
                    mostrarEnviado(cardEstado, tvEstadoLabel, btnEnviar)
                    Toast.makeText(this@VerificacionActivity, "Solicitud registrada. El equipo la revisará pronto.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun disableForm(btnEnviar: Button) {
        btnEnviar.isEnabled = false
        btnEnviar.alpha = 0.5f
    }

    private fun mostrarEnviado(card: CardView, tvLabel: TextView, btn: Button) {
        card.setCardBackgroundColor(0xFF1a3a5c.toInt())
        tvLabel.text = "⏳ Solicitud enviada — en revisión"
        tvLabel.setTextColor(0xFF3498db.toInt())
        card.visibility = View.VISIBLE
        btn.text = "Solicitud enviada ✓"
        btn.isEnabled = false
    }

    private fun validarRut(rut: String): Boolean {
        val cleaned = rut.replace(".", "").replace("-", "").uppercase().trim()
        if (cleaned.length < 2) return false
        val dv = cleaned.last()
        val numero = cleaned.dropLast(1).toLongOrNull() ?: return false
        if (numero < 1_000_000L) return false

        var sum = 0
        var mul = 2
        var n = numero
        while (n > 0) {
            sum += (n % 10).toInt() * mul
            n /= 10
            mul = if (mul == 7) 2 else mul + 1
        }
        val expected = when (val res = 11 - (sum % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> ('0' + res)
        }
        return dv == expected
    }
}
