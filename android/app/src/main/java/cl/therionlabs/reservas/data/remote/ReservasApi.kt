package cl.therionlabs.reservas.data.remote

import cl.therionlabs.reservas.data.remote.dto.ConflictoDto
import cl.therionlabs.reservas.data.remote.dto.LoginRequestDto
import cl.therionlabs.reservas.data.remote.dto.OcupacionDto
import cl.therionlabs.reservas.data.remote.dto.ReservaDto
import cl.therionlabs.reservas.data.remote.dto.ReservaRequestDto
import cl.therionlabs.reservas.data.remote.dto.SalaDto
import cl.therionlabs.reservas.data.remote.dto.SalaRequestDto
import cl.therionlabs.reservas.data.remote.dto.UsuarioDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato Retrofit del backend (controladores en backend/src/main/java/.../web).
 * Los parámetros null no se envían, así los filtros opcionales quedan fuera de la URL.
 */
interface ReservasApi {

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequestDto): UsuarioDto

    @GET("api/salas")
    suspend fun salas(@Query("incluirInactivas") incluirInactivas: Boolean = true): List<SalaDto>

    @GET("api/salas/disponibles")
    suspend fun salasDisponibles(
        @Query("fecha") fecha: String,
        @Query("inicio") inicio: String,
        @Query("fin") fin: String,
        @Query("capacidadMin") capacidadMin: Int? = null,
        @Query("proyector") proyector: Boolean? = null,
        @Query("computadores") computadores: Boolean? = null,
        @Query("climatizacion") climatizacion: Boolean? = null,
        @Query("tipo") tipo: String? = null
    ): List<SalaDto>

    @POST("api/salas")
    suspend fun crearSala(@Body cuerpo: SalaRequestDto): SalaDto

    @PUT("api/salas/{id}")
    suspend fun actualizarSala(@Path("id") id: Long, @Body cuerpo: SalaRequestDto): SalaDto

    @DELETE("api/salas/{id}")
    suspend fun desactivarSala(@Path("id") id: Long): Response<Unit>

    @GET("api/reservas")
    suspend fun reservas(
        @Query("fecha") fecha: String? = null,
        @Query("usuarioId") usuarioId: Long? = null,
        @Query("desde") desde: String? = null
    ): List<ReservaDto>

    @POST("api/reservas")
    suspend fun crearReserva(@Body cuerpo: ReservaRequestDto): ReservaDto

    @PUT("api/reservas/{id}")
    suspend fun actualizarReserva(@Path("id") id: Long, @Body cuerpo: ReservaRequestDto): ReservaDto

    @DELETE("api/reservas/{id}")
    suspend fun cancelarReserva(@Path("id") id: Long): ReservaDto

    @GET("api/reservas/conflictos")
    suspend fun conflictos(): List<ConflictoDto>

    @GET("api/ocupacion")
    suspend fun ocupacion(@Query("desde") desde: String, @Query("hasta") hasta: String): List<OcupacionDto>
}
