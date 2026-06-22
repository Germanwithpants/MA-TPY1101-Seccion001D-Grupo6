package com.conectatarot.app.network

import android.content.Context
import android.content.Intent
import com.conectatarot.app.ConectaTarotApp
import com.conectatarot.app.MainActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://ingenious-smile-production-f4df.up.railway.app/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val ctx = ConectaTarotApp.instance
            ctx.getSharedPreferences("conectatarot", Context.MODE_PRIVATE)
                .edit().clear().apply()
            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("session_expired", true)
            }
            ctx.startActivity(intent)
        }
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
