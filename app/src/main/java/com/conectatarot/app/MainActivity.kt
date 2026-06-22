package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.FcmTokenRequest
import com.conectatarot.app.network.GoogleLoginRequest
import com.conectatarot.app.network.LoginRequest
import com.conectatarot.app.network.LoginResponse
import com.conectatarot.app.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.getBooleanExtra("session_expired", false)) {
            Toast.makeText(this, "Tu sesión expiró. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
        }

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("751618758049-va742gbpc4q1g85418c2kfaubf48s562.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvError = findViewById<TextView>(R.id.tvError)
        val tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)
        val tvIrRegistroTarotista = findViewById<TextView>(R.id.tvIrRegistroTarotista)
        val btnGoogleLogin = findViewById<Button>(R.id.btnGoogleLogin)

        tvIrRegistro.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }
        tvIrRegistroTarotista.setOnClickListener { startActivity(Intent(this, RegistroTarotistaActivity::class.java)) }

        btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Completa todos los campos"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Iniciando..."
            tvError.text = ""

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.login(LoginRequest(email, password))
                    if (response.isSuccessful) {
                        onLoginSuccess(response.body()!!)
                    } else {
                        val msg = if (response.code() == 403) "Cuenta desactivada"
                                  else "Credenciales incorrectas"
                        tvError.text = msg
                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar sesión"
                    }
                } catch (e: Exception) {
                    tvError.text = "Error de conexión"
                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"
                }
            }
        }
    }

    private fun onLoginSuccess(body: LoginResponse) {
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        prefs.edit()
            .putString("token", body.token)
            .putString("nombre", body.nombre)
            .putString("rol", body.rol)
            .putString("email", body.email)
            .putInt("idUsuario", body.idUsuario)
            .apply()

        // Register FCM token with backend (best-effort)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            lifecycleScope.launch {
                try {
                    RetrofitClient.instance.saveFcmToken(
                        "Bearer ${body.token}", body.idUsuario, FcmTokenRequest(fcmToken)
                    )
                } catch (e: Exception) { /* non-critical */ }
            }
        }

        val dest = when {
            body.esNuevo -> SeleccionRolActivity::class.java
            body.rol == "TAROTISTA" -> TarotistaHomeActivity::class.java
            else -> ClienteActivity::class.java
        }
        startActivity(Intent(this, dest))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In falló: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.instance.googleLogin(GoogleLoginRequest(idToken))
                        if (response.isSuccessful) {
                            onLoginSuccess(response.body()!!)
                        } else {
                            Toast.makeText(this@MainActivity, "Error al sincronizar cuenta", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Error de autenticación Firebase", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
