package cl.therionlabs.reservas.data.remote

import cl.therionlabs.reservas.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente único del backend. `by lazy` construye el Retrofit la primera vez que se usa
 * y lo reutiliza para siempre. La URL sale de gradle.properties (reservas.apiUrl).
 */
object ApiClient {

    val api: ReservasApi by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReservasApi::class.java)
    }
}
