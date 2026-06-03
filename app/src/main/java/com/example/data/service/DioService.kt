package com.example.data.service

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AltParcaModel
import com.example.data.model.UrunModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface OrderApiService {
    @GET("get-order-items/{id}")
    suspend fun getOrderItems(@Path("id") orderId: String): List<UrunModel>

    @GET("get-sub-items/{product_id}")
    suspend fun getSubItems(@Path("product_id") productId: String): List<AltParcaModel>

    @POST("upload-photos")
    suspend fun uploadPhotos(@Body photos: List<AltParcaModel>): Map<String, Any>
}

class DioService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shipment_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_BASE_URL = "api_base_url"
        const val KEY_DEMO_MODE = "demo_mode"
        const val DEFAULT_BASE_URL = "https://api-saha-montaj.com/"
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty() && !cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        prefs.edit().putString(KEY_BASE_URL, cleanUrl).apply()
    }

    fun isDemoMode(): Boolean {
        // Default to true for easy previewing right out of the box
        return prefs.getBoolean(KEY_DEMO_MODE, true)
    }

    fun setDemoMode(isDemo: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, isDemo).apply()
    }

    fun getApiService(): OrderApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val baseUrl = getBaseUrl()
        val targetUrl = if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            baseUrl
        } else {
            DEFAULT_BASE_URL
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(targetUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(OrderApiService::class.java)
    }
}
