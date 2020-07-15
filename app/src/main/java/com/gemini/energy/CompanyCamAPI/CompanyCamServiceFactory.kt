package CompanyCamAPI

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object CompanyCamServiceFactory {

    private const val API_BASE_URL = "https://app.companycam.com/"
    private const val ACCESS_TOKEN = "e12b134258ccd3dff694581132e6192f998c54bddc1b5c5ea6f1363bd72c9b68"
    private const val SECRET_KEY = ""
    private const val USER_EMAIL = "akinslow2@geminiesolutions.com"

    // add the logging interceptor

//    https://app.companycam.com/oauth/authorize?client_id=e12b134258ccd3dff694581132e6192f998c54bddc1b5c5ea6f1363bd72c9b68&redirect_uri=https://www.hidevmobile.com/&response_type=code&scope=read+write+destroy


    fun makeService(): CompanyCamService {
        // log json responses
        val logging = HttpLoggingInterceptor()
        logging.apply { logging.level = HttpLoggingInterceptor.Level.BODY }

//        curl -X GET -H
//        "Content-type: application/json" -H
//        "Authorization: Bearer <ACCESS_TOKEN>" -H
//        "X-CompanyCam-User: shawn@psych.com"

        val httpClient = OkHttpClient.Builder().addInterceptor(logging).addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
//                .addHeader("X-CompanyCam-Secret", SECRET_KEY)
                    .addHeader("Authorization", "Bearer $ACCESS_TOKEN")
                    .addHeader("X-CompanyCam-User", USER_EMAIL)
                    .addHeader("Content-type", "application/json")
                    .build()
            chain.proceed(newRequest)

        }

        val builder = Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())

        return builder
                .client(httpClient.build())
                .build().create(CompanyCamService::class.java)
    }
}