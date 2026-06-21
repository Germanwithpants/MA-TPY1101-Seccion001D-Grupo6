package com.conectatarot.app.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Path


interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/tarotistas")
    suspend fun getTarotistas(@Header("Authorization") token: String): Response<TarotistasResponse>

    @POST("api/sesiones")
    suspend fun agendarSesion(
        @Header("Authorization") token: String,
        @Body request: SesionRequest
    ): Response<SesionResponse>

    @GET("api/sesiones/mis-sesiones")
    suspend fun getMisSesiones(
        @Header("Authorization") token: String
    ): Response<SesionClienteResponse>

    @PUT("api/sesiones/{id}/cancelar")
    suspend fun cancelarSesion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>

    @GET("api/sesiones/tarotista")
    suspend fun getSesionesTarotista(
        @Header("Authorization") token: String
    ): Response<SesionTarotistaResponse>

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

    @POST("api/usuarios")
    suspend fun registrarUsuario(
        @Body request: RegistroRequest
    ): Response<RegistroResponse>

    @POST("api/tarotistas")
    suspend fun registrarTarotista(
        @Body request: RegistroTarotistaRequest
    ): Response<RegistroResponse>

    @GET("api/especialidades")
    suspend fun getEspecialidades(): Response<EspecialidadesResponse>

    @PUT("api/usuarios/{id}")
    suspend fun editarPerfil(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: EditarPerfilRequest
    ): Response<Any>

    @PUT("api/tarotistas/{id}/perfil")
    suspend fun editarPerfilTarotista(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: EditarPerfilTarotistaRequest
    ): Response<Any>

    @POST("api/resenas")
    suspend fun crearResena(@Body request: ResenaRequest): Response<Any>

    @GET("api/resenas/tarotista/{id}")
    suspend fun getResenasTarotista(@Path("id") id: Int): Response<ResenasResponse>

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>

    @POST("api/tarotistas/completar-perfil")
    suspend fun completarPerfilTarotista(
        @Header("Authorization") token: String,
        @Body request: CompletarPerfilRequest
    ): Response<Any>

    @POST("api/pagos/iniciar/{sesionId}")
    suspend fun iniciarPago(@Path("sesionId") sesionId: Int): Response<IniciarPagoResponse>

    @GET("api/pagos/estado/{sesionId}")
    suspend fun estadoPago(@Path("sesionId") sesionId: Int): Response<EstadoPagoResponse>
}

data class PagedData(
    val content: List<SesionItem>?,
    val totalElements: Int,
    val totalPages: Int
)
data class LoginRequest(
    val email: String,
    val password: String
)
data class Tarotista(
    val id: Int,
    val nombreProfesional: String,
    val descripcion: String?,
    val precioBase: Double?,
    val estado: String?,
    val especialidades: List<String>?
)

data class TarotistasResponse(
    val success: Boolean,
    val message: String,
    val data: List<Tarotista>?
)
data class SesionRequest(
    val usuarioId: Int,
    val tarotistaId: Int,
    val especialidadId: Int,
    val fecha: String,
    val duracionMinutos: Int
)

data class SesionResponse(
    val success: Boolean,
    val message: String,
    val data: Any?
)

data class SesionClienteResponse(
    val success: Boolean,
    val message: String,
    val data: List<SesionItem>?
)

data class SesionItem(
    val id: Int,
    val nombreCliente: String?,
    val nombreTarotista: String,
    val especialidad: String,
    val fecha: String,
    val duracionMinutos: Int,
    val precioTotal: Double,
    val estado: String,
    val estadoPago: String? = "PENDIENTE"
)

data class SesionTarotistaResponse(
    val success: Boolean,
    val message: String,
    val data: PagedData?
)

data class RegistroRequest(
    val nombre: String,
    val email: String,
    val password: String
)

data class RegistroResponse(
    val success: Boolean,
    val message: String
)

data class RegistroTarotistaRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val nombreProfesional: String,
    val descripcion: String,
    val precioBase: Double
)

data class EspecialidadesResponse(
    val success: Boolean,
    val data: List<Especialidad>?
)

data class Especialidad(
    val id: Int,
    val nombre: String
)

data class EditarPerfilRequest(
    val nombre: String,
    val email: String
)

data class EditarPerfilTarotistaRequest(
    val nombreProfesional: String,
    val descripcion: String,
    val precioBase: Double
)

data class ResenaRequest(
    val sesionId: Int,
    val tarotistaId: Int,
    val usuarioId: Int,
    val calificacion: Int,
    val comentario: String
)

data class ResenaItem(
    val id: Int,
    val calificacion: Int,
    val comentario: String?
)

data class ResenasResponse(
    val success: Boolean,
    val data: List<ResenaItem>?,
    val promedio: Double
)

data class GoogleLoginRequest(
    val idToken: String
)

data class LoginResponse(
    val idUsuario: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean,
    val token: String,
    val esNuevo: Boolean = false
)

data class CompletarPerfilRequest(
    val usuarioId: Int,
    val nombreProfesional: String,
    val descripcion: String,
    val precioBase: Double,
    val email: String
)

data class IniciarPagoResponse(
    val success: Boolean,
    val url: String?,
    val token: String?
)

data class EstadoPagoResponse(
    val success: Boolean,
    val estadoPago: String?
)
