package com.conectatarot.app.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>

    // Usuarios
    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body request: RegistroRequest): Response<RegistroResponse>

    @PUT("api/usuarios/{id}")
    suspend fun editarPerfil(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: EditarPerfilRequest
    ): Response<Any>

    @PUT("api/usuarios/{id}/fcm-token")
    suspend fun saveFcmToken(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: FcmTokenRequest
    ): Response<Any>

    @PUT("api/usuarios/{id}/password")
    suspend fun cambiarPassword(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CambiarPasswordRequest
    ): Response<Any>

    // Tarotistas
    @GET("api/tarotistas")
    suspend fun getTarotistas(
        @Header("Authorization") token: String,
        @Query("especialidad") especialidad: String? = null
    ): Response<TarotistasResponse>

    @POST("api/tarotistas")
    suspend fun registrarTarotista(@Body request: RegistroTarotistaRequest): Response<RegistroResponse>

    @GET("api/tarotistas/usuario/{usuarioId}")
    suspend fun getTarotistaByUsuario(
        @Header("Authorization") token: String,
        @Path("usuarioId") usuarioId: Int
    ): Response<TarotistaResponse>

    @PUT("api/tarotistas/{id}/perfil")
    suspend fun editarPerfilTarotista(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: EditarPerfilTarotistaRequest
    ): Response<Any>

    @POST("api/tarotistas/completar-perfil")
    suspend fun completarPerfilTarotista(
        @Header("Authorization") token: String,
        @Body request: CompletarPerfilRequest
    ): Response<Any>

    // Especialidades
    @GET("api/especialidades")
    suspend fun getEspecialidades(): Response<EspecialidadesResponse>

    @GET("api/tarotistas/{id}/especialidades")
    suspend fun getEspecialidadesTarotista(
        @Path("id") id: Int
    ): Response<EspecialidadesTarotistaResponse>

    // Disponibilidad
    @GET("api/tarotistas/{id}/disponibilidad")
    suspend fun getDisponibilidad(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<DisponibilidadResponse>

    @POST("api/tarotistas/{id}/disponibilidad")
    suspend fun addDisponibilidad(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: DisponibilidadRequest
    ): Response<Any>

    @DELETE("api/tarotistas/{tarotistaId}/disponibilidad/{disponibilidadId}")
    suspend fun deleteDisponibilidad(
        @Header("Authorization") token: String,
        @Path("tarotistaId") tarotistaId: Int,
        @Path("disponibilidadId") disponibilidadId: Int
    ): Response<Any>

    // Sesiones
    @POST("api/sesiones")
    suspend fun agendarSesion(
        @Header("Authorization") token: String,
        @Body request: SesionRequest
    ): Response<SesionResponse>

    @GET("api/sesiones/mis-sesiones")
    suspend fun getMisSesiones(@Header("Authorization") token: String): Response<SesionClienteResponse>

    @PUT("api/sesiones/{id}/cancelar")
    suspend fun cancelarSesion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>

    @GET("api/sesiones/tarotista")
    suspend fun getSesionesTarotista(@Header("Authorization") token: String): Response<SesionTarotistaResponse>

    @PUT("api/sesiones/{id}/confirmar")
    suspend fun confirmarSesion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>

    @PUT("api/sesiones/{id}/rechazar")
    suspend fun rechazarSesion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>

    // Reseñas
    @POST("api/resenas")
    suspend fun crearResena(
        @Header("Authorization") token: String,
        @Body request: ResenaRequest
    ): Response<Any>

    @GET("api/resenas/tarotista/{id}")
    suspend fun getResenasTarotista(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ResenasResponse>

    @GET("api/resenas/sesion/{sesionId}/existe")
    suspend fun existeResena(
        @Header("Authorization") token: String,
        @Path("sesionId") sesionId: Int
    ): Response<ExisteResenaResponse>

    @POST("api/resenas/cliente")
    suspend fun crearResenaCliente(
        @Header("Authorization") token: String,
        @Body request: ResenaClienteRequest
    ): Response<Any>

    // Pagos
    @POST("api/pagos/iniciar/{sesionId}")
    suspend fun iniciarPago(
        @Header("Authorization") token: String,
        @Path("sesionId") sesionId: Int
    ): Response<IniciarPagoResponse>

    @GET("api/pagos/estado/{sesionId}")
    suspend fun estadoPago(@Path("sesionId") sesionId: Int): Response<EstadoPagoResponse>

    // Admin
    @GET("api/admin/estadisticas")
    suspend fun getAdminEstadisticas(@Header("Authorization") token: String): Response<AdminEstadisticasResponse>

    @GET("api/admin/usuarios")
    suspend fun getAdminUsuarios(@Header("Authorization") token: String): Response<AdminUsuariosResponse>

    @PUT("api/admin/usuarios/{id}/bloquear")
    suspend fun bloquearUsuario(@Header("Authorization") token: String, @Path("id") id: Int): Response<Any>

    @PUT("api/admin/usuarios/{id}/desbloquear")
    suspend fun desbloquearUsuario(@Header("Authorization") token: String, @Path("id") id: Int): Response<Any>

    @GET("api/admin/tarotistas/pendientes")
    suspend fun getTarotistasPendientes(@Header("Authorization") token: String): Response<AdminTarotistasResponse>

    @PUT("api/admin/tarotistas/{id}/aprobar")
    suspend fun aprobarTarotista(@Header("Authorization") token: String, @Path("id") id: Int): Response<Any>

    @PUT("api/admin/tarotistas/{id}/rechazar")
    suspend fun rechazarTarotista(@Header("Authorization") token: String, @Path("id") id: Int): Response<Any>

    @GET("api/admin/pagos")
    suspend fun getAdminPagos(@Header("Authorization") token: String): Response<AdminPagosResponse>

    @GET("api/admin/comisiones")
    suspend fun getAdminComisiones(@Header("Authorization") token: String): Response<AdminComisionesResponse>

    @GET("api/admin/logs")
    suspend fun getAdminLogs(@Header("Authorization") token: String): Response<AdminLogsResponse>

    // Verificación
    @POST("api/verificacion/{tarotistaId}/solicitar")
    suspend fun solicitarVerificacion(
        @Header("Authorization") token: String,
        @Path("tarotistaId") tarotistaId: Int,
        @Body request: VerificacionRequest
    ): Response<Any>

    @GET("api/verificacion/{tarotistaId}/estado")
    suspend fun getEstadoVerificacion(
        @Header("Authorization") token: String,
        @Path("tarotistaId") tarotistaId: Int
    ): Response<VerificacionStatusResponse>

    // Disputas
    @POST("api/disputas")
    suspend fun reportarDisputa(@Header("Authorization") token: String, @Body body: DisputaRequest): Response<Any>

    @GET("api/admin/disputas")
    suspend fun getAdminDisputas(@Header("Authorization") token: String): Response<DisputasResponse>

    @PUT("api/admin/disputas/{id}/resolver")
    suspend fun resolverDisputa(@Header("Authorization") token: String, @Path("id") id: Long, @Body body: Map<String, String>): Response<Any>

    @PUT("api/admin/disputas/{id}/en-revision")
    suspend fun marcarEnRevision(@Header("Authorization") token: String, @Path("id") id: Long): Response<Any>
}

// ── Data Classes ──────────────────────────────────────────────────────────────

data class LoginRequest(val email: String, val password: String)
data class GoogleLoginRequest(val idToken: String)
data class FcmTokenRequest(val fcmToken: String)

data class LoginResponse(
    val idUsuario: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean,
    val token: String,
    val esNuevo: Boolean = false
)

data class RegistroRequest(val nombre: String, val email: String, val password: String)
data class RegistroResponse(val success: Boolean, val message: String)

data class RegistroTarotistaRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val nombreProfesional: String,
    val descripcion: String,
    val precioBase: Double
)

data class EditarPerfilRequest(val nombre: String, val email: String)

data class EditarPerfilTarotistaRequest(
    val nombreProfesional: String,
    val descripcion: String,
    val precioBase: Double
)

data class CompletarPerfilRequest(
    val usuarioId: Int,
    val nombreProfesional: String,
    val descripcion: String,
    val precioBase: Double,
    val email: String
)

data class Tarotista(
    val id: Int,
    val nombreProfesional: String,
    val descripcion: String?,
    val precioBase: Double?,
    val estado: String?,
    val especialidades: List<String>?,
    val promedio: Double? = null,
    val totalResenas: Int? = null,
    val verificado: Boolean? = false
)

data class TarotistasResponse(val success: Boolean, val message: String, val data: List<Tarotista>?)
data class TarotistaResponse(val success: Boolean, val message: String, val data: Tarotista?)

data class Especialidad(val id: Int, val nombre: String)
data class EspecialidadesResponse(val success: Boolean, val data: List<Especialidad>?)
data class EspecialidadesTarotistaResponse(val success: Boolean, val data: List<Especialidad>?)

data class DisponibilidadItem(
    val id: Int,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String,
    val activa: Boolean?
)
data class DisponibilidadResponse(val success: Boolean, val data: List<DisponibilidadItem>?)
data class DisponibilidadRequest(val diaSemana: String, val horaInicio: String, val horaFin: String)

data class SesionRequest(
    val usuarioId: Int,
    val tarotistaId: Int,
    val especialidadId: Int,
    val fecha: String,
    val duracionMinutos: Int
)

data class SesionResponse(val success: Boolean, val message: String, val data: Any?)

data class SesionItem(
    val id: Int,
    val nombreCliente: String?,
    val nombreTarotista: String,
    val especialidad: String,
    val fecha: String,
    val duracionMinutos: Int,
    val precioTotal: Double,
    val estado: String,
    val estadoPago: String? = "PENDIENTE",
    val tarotistaId: Int? = null,
    val fechaCreacion: String? = null
)
data class CambiarPasswordRequest(val passwordActual: String, val passwordNueva: String)
data class ResenaClienteRequest(val sesionId: Int, val tarotistaId: Int, val calificacion: Int, val comentario: String)

data class PagedData(
    val content: List<SesionItem>?,
    val totalElements: Int,
    val totalPages: Int
)

data class SesionClienteResponse(val success: Boolean, val message: String, val data: List<SesionItem>?)
data class SesionTarotistaResponse(val success: Boolean, val message: String, val data: PagedData?)

data class ResenaRequest(
    val sesionId: Int,
    val tarotistaId: Int,
    val usuarioId: Int,
    val calificacion: Int,
    val comentario: String,
    val tags: String = ""
)

data class ResenaItem(
    val id: Int,
    val calificacion: Int,
    val comentario: String? = "",
    val tags: String? = "",
    val fecha: String? = ""
)
data class ResenasResponse(val success: Boolean, val data: List<ResenaItem>?, val promedio: Double, val total: Int = 0)
data class ExisteResenaResponse(val existe: Boolean)

data class IniciarPagoResponse(val success: Boolean, val url: String?, val token: String?)
data class EstadoPagoResponse(val success: Boolean, val estadoPago: String?)

// Admin data classes
data class AdminRol(val idRol: Int?, val nombreRol: String?)
data class AdminUsuario(val idUsuario: Int, val nombre: String, val email: String, val rol: AdminRol?, val activo: Boolean?)
data class AdminUsuariosResponse(val success: Boolean, val data: List<AdminUsuario>?)

data class AdminTarotistaPendiente(val id: Int, val nombreProfesional: String, val descripcion: String?, val precioBase: Double?, val estado: String?)
data class AdminTarotistasResponse(val success: Boolean, val data: List<AdminTarotistaPendiente>?)

data class AdminPagoItem(val sesionId: Int, val monto: Double, val fecha: String, val estadoPago: String, val tarotista: String, val cliente: String)
data class AdminPagosResponse(val success: Boolean, val data: List<AdminPagoItem>?)

data class AdminComisionItem(val sesionId: Int, val fecha: String, val monto: Double, val comision: Double, val tarotista: String)
data class AdminComisionesData(val totalComisiones: Double, val cantidad: Int, val detalle: List<AdminComisionItem>)
data class AdminComisionesResponse(val success: Boolean, val data: AdminComisionesData?)

data class AdminEstadisticasData(val totalUsuarios: Long, val usuariosActivos: Long, val totalSesiones: Long, val tarotistaActivos: Long, val ingresoTotal: Double)
data class AdminEstadisticasResponse(val success: Boolean, val data: AdminEstadisticasData?)

data class AdminLogItem(val id: Long, val accion: String, val entidad: String?, val entidadId: String?, val adminEmail: String?, val detalle: String?, val timestamp: String)
data class AdminLogsResponse(val success: Boolean, val data: List<AdminLogItem>?)

data class VerificacionRequest(
    val rut: String,
    val nombreCompleto: String,
    val banco: String,
    val tipoCuenta: String,
    val numeroCuenta: String,
    val titularCuenta: String
)

data class VerificacionStatus(
    val estado: String,
    val fechaSolicitud: String? = null,
    val observacion: String? = null
)

data class VerificacionStatusResponse(val success: Boolean, val data: VerificacionStatus?)

data class DisputaRequest(val sesionId: Int, val tipo: String, val descripcion: String)
data class DisputaItem(
    val id: Long, val sesionId: Int, val nombreCliente: String, val nombreTarotista: String,
    val tipo: String, val descripcion: String?, val estado: String,
    val reportadoPor: String, val fechaCreacion: String
)
data class DisputasResponse(val success: Boolean, val data: List<DisputaItem>?)
