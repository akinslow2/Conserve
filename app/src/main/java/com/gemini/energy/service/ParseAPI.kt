package com.gemini.energy.service

import com.gemini.energy.domain.entity.Audit
import com.gemini.energy.domain.entity.Feature
import com.google.gson.JsonObject
import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class ParseAPI {

    data class ParseListResponse<T>(val result: List<T>)

    interface ParseAPIService {
        @GET("classes/PlugLoad")
        fun fetchPlugload(@Query("where") where: String): Single<JsonObject>

        @GET("classes/LaborCost")
        fun fetchLaborCost(@Query("where") where: String): Single<JsonObject>

        @GET("classes/HVAC")
        fun fetchHVAC(@Query("where") where: String): Single<JsonObject>

        @GET("classes/Motors")
        fun fetchMotors(@Query("where") where: String): Single<JsonObject>

        @GET("classes/$SYNC_AUDIT")
        fun fetchAudits(@Query("where") where: String): Single<ParseListResponse<Audit>>

        @GET("classes/$SYNC_FEATURE")
        fun fetchFeatures(@Query("where") where: String, @Query("limit") limit: Int = 10000): Single<ParseListResponse<Feature>>
    }

    companion object {
        private val serverUrl = System.getenv("PARSE_SERVER_URL")
        private val applicationId = System.getenv("PARSE_APP_ID")
        private val masterKey = System.getenv("PARSE_MASTER_KEY")

        private const val SYNC_AUDIT = "rAudit"
        private const val SYNC_FEATURE = "rFeature"

        private val okHttpClient = OkHttpClient()
                .newBuilder()
                .addInterceptor {
                    val original = it.request()
                    val request = original.newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("X-Parse-Application-Id", applicationId)
                            .addHeader("X-Parse-REST-API-Key", masterKey)
                            .build()
                    it.proceed(request)
                }

        fun create(): ParseAPIService {
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(serverUrl)
                    .client(okHttpClient.build())
                    .build()

            return retrofit.create(ParseAPIService::class.java)
        }
    }
}

