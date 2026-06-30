package com.conectatarot.app

import android.app.AlertDialog
import android.content.Context
import android.widget.ScrollView
import android.widget.TextView

object PoliticaHelper {

    fun mostrar(context: Context) {
        val tv = TextView(context).apply {
            text = TEXTO_POLITICA
            textSize = 13f
            setTextColor(0xFFe8c3ff.toInt())
            setPadding(48, 32, 48, 32)
            setLineSpacing(4f, 1.1f)
        }
        val sv = ScrollView(context).apply { addView(tv) }

        AlertDialog.Builder(context)
            .setTitle("Términos de Uso y Política de Privacidad")
            .setView(sv)
            .setPositiveButton("Entendido", null)
            .show()
    }

    private val TEXTO_POLITICA = """
POLÍTICA DE PRIVACIDAD Y TÉRMINOS DE USO
ConectaTarot
Última actualización: junio 2025

━━━━━━━━━━━━━━━━━━━━━━━━━
POLÍTICA DE PRIVACIDAD
━━━━━━━━━━━━━━━━━━━━━━━━━

1. QUIÉNES SOMOS
ConectaTarot es una plataforma de intermediación entre tarotistas y clientes, desarrollada como proyecto universitario (DuocUC). Contacto: ric.gaete@duocuc.cl

2. DATOS QUE RECOPILAMOS

Para todos los usuarios:
· Nombre completo y correo electrónico
· Contraseña (almacenada de forma cifrada, nunca en texto plano)
· Historial de sesiones agendadas, pagos y reseñas
· Token de dispositivo para notificaciones push (Firebase)

Para tarotistas (adicional):
· Nombre profesional, descripción y precio de sesión
· RUT y nombre completo para verificación de identidad
· Fotografía del carnet de identidad (frente)
· Datos de cuenta bancaria: banco, tipo de cuenta, número y titular

3. PARA QUÉ USAMOS TUS DATOS
· Crear y gestionar tu cuenta
· Conectarte con tarotistas o clientes según tu rol
· Procesar y registrar pagos de sesiones
· Verificar la identidad de los tarotistas
· Enviarte notificaciones sobre el estado de tus sesiones
· Detectar y prevenir usos fraudulentos de la plataforma
· Mejorar la aplicación

4. CON QUIÉN COMPARTIMOS TUS DATOS
· Administradores de ConectaTarot: revisan perfiles y verificaciones
· Transbank: procesa los pagos de manera segura (nunca accedemos a datos de tarjeta de crédito/débito del cliente)
· Firebase (Google): autenticación de usuarios y envío de notificaciones push
· No vendemos, cedemos ni comercializamos tus datos personales con terceros

5. RETENCIÓN DE DATOS
Tus datos se conservan mientras tu cuenta esté activa. Al solicitar la eliminación de tu cuenta, tus datos personales serán eliminados en un plazo máximo de 30 días hábiles, conservando únicamente los registros contables que exige la legislación tributaria chilena.

6. TUS DERECHOS
De acuerdo con la Ley N° 19.628 sobre Protección de la Vida Privada y la Ley N° 21.096, tienes derecho a:
· Acceder a los datos personales que tenemos sobre ti
· Rectificar datos inexactos o incompletos
· Oponerte al tratamiento de tus datos
· Solicitar la eliminación de tu cuenta y datos

Para ejercer estos derechos, escríbenos a ric.gaete@duocuc.cl indicando tu nombre y correo registrado.

7. SEGURIDAD
· Todas las comunicaciones utilizan cifrado HTTPS/TLS
· Las contraseñas se almacenan con hash bcrypt
· Los datos sensibles de verificación solo son accesibles por el equipo administrador
· Realizamos buenas prácticas de seguridad en el desarrollo de la app

8. CAMBIOS A ESTA POLÍTICA
Te notificaremos dentro de la app si realizamos cambios relevantes a esta política de privacidad.

━━━━━━━━━━━━━━━━━━━━━━━━━
TÉRMINOS Y CONDICIONES DE USO
━━━━━━━━━━━━━━━━━━━━━━━━━

1. ACEPTACIÓN
Al crear una cuenta en ConectaTarot aceptas estos Términos de Uso en su totalidad. Si no estás de acuerdo con alguna parte, no debes usar la aplicación.

2. DESCRIPCIÓN DEL SERVICIO
ConectaTarot es una plataforma de intermediación que permite a los usuarios agendar sesiones de tarot con tarotistas registrados. No somos una agencia de empleo ni prestamos el servicio de tarot directamente.

3. REGISTRO Y CUENTA
· Debes proporcionar información veraz y actualizada al registrarte
· Eres responsable de mantener la confidencialidad de tu contraseña
· Una cuenta por persona; está prohibido crear cuentas múltiples o falsas
· La edad mínima para usar la plataforma es 18 años

4. OBLIGACIONES DE LOS TAROTISTAS
· Toda la información profesional publicada debe ser veraz
· Los tarotistas son responsables del contenido y calidad de sus sesiones
· ConectaTarot no garantiza ni avala las habilidades o resultados de ningún tarotista
· Los tarotistas deben completar el proceso de verificación de identidad para recibir pagos

5. PAGOS Y COMISIONES
· Los pagos se procesan a través de Transbank bajo sus propios términos de servicio
· ConectaTarot retiene una comisión por cada sesión procesada exitosamente
· El monto total se muestra claramente antes de confirmar el pago
· No se realizan reembolsos automáticos; las disputas se gestionan a través del sistema de reportes

6. CANCELACIONES
· Los clientes pueden cancelar sesiones pendientes o confirmadas desde "Mis Sesiones"
· Los tarotistas pueden rechazar solicitudes antes de confirmarlas
· Las cancelaciones reiteradas sin justificación pueden resultar en restricciones de la cuenta

7. CONDUCTA PROHIBIDA
Está prohibido:
· Publicar información falsa o engañosa
· Acosar, amenazar o discriminar a otros usuarios
· Usar la plataforma para actividades ilegales
· Intentar eludir el sistema de pagos acordando cobros fuera de la plataforma
· Vulnerar los sistemas de seguridad de la aplicación

8. PROPIEDAD INTELECTUAL
El nombre ConectaTarot, el logotipo, el diseño y el código de la aplicación son propiedad del equipo desarrollador. No se autoriza su reproducción sin permiso expreso.

9. LIMITACIÓN DE RESPONSABILIDAD
ConectaTarot actúa únicamente como intermediario. No somos responsables de:
· El contenido o resultados de las sesiones de tarot
· Interrupciones del servicio por causas externas (fallas de internet, servidores, etc.)
· Acciones de terceros (Transbank, Firebase)
· Decisiones tomadas por los usuarios basadas en las sesiones de tarot

10. LEY APLICABLE Y JURISDICCIÓN
Estos Términos se rigen por la legislación de la República de Chile. Cualquier disputa se someterá a la jurisdicción de los tribunales ordinarios de justicia de Santiago de Chile.

11. CONTACTO
Para consultas, reclamos o ejercicio de derechos: ric.gaete@duocuc.cl
    """.trimIndent()
}
