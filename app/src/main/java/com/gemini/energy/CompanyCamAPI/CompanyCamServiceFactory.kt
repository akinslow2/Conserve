package CompanyCamAPI

import CompanyCamAPI.Requests.Auth2RequestParameters
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.gemini.energy.App
import com.gemini.energy.branch
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI


object CompanyCamServiceFactory {

    private const val API_BASE_URL = "https://app.companycam.com/"
    private const val ACCESS_TOKEN = "e12b134258ccd3dff694581132e6192f998c54bddc1b5c5ea6f1363bd72c9b68"
    private const val ROOT_USER_EMAIL = "akinslow2@geminiesolutions.com"

    private const val CLIENT_ID = "8418eeee9e98af39e69160ba9e4127d55dcbd0e3f88a381e060c0ed571ca3e73"
    private const val SECRET_KEY = "c1a14dd4c9099eed46c03f2b0394519412677a42a940d00605a1b65ceffe5722"
    private const val REDIRECT_URI = "https://www.geminiesolutions.com/companycamauth"

    private const val TOKEN_STORAGE = "companycamBearerToken"

    fun setToken(data: Uri) {
        val code = data.getQueryParameter("code") ?: return

        //1 Create a Coroutine scope using a job to be able to cancel when needed
        val mainActivityJob = Job()

        //2 Handle exceptions if any
        val errorHandler = CoroutineExceptionHandler { _, exception ->
//            callback(false, exception)
        }

        //3 the Coroutine runs using the Main (UI) dispatcher
        val coroutineScope = CoroutineScope(mainActivityJob + Dispatchers.Main)
        coroutineScope.launch(errorHandler) {

            val token = makeService().getAuthToken(Auth2RequestParameters(CLIENT_ID, SECRET_KEY, code, REDIRECT_URI))
            setToken(token.access_token)
        }
    }

    fun setToken(authToken: String) {
        val prefs: SharedPreferences = App.instance.getSharedPreferences(TOKEN_STORAGE, Context.MODE_PRIVATE)
        prefs.edit().putString("bearer-token", authToken).apply()
    }

    fun bearerToken(): String? {
        val prefs: SharedPreferences = App.instance.getSharedPreferences(TOKEN_STORAGE, Context.MODE_PRIVATE)
        return prefs.getString("bearer-token", null)
    }

    fun clearAuthToken() {
        val prefs: SharedPreferences = App.instance.getSharedPreferences(TOKEN_STORAGE, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.remove("bearer-token")
        editor.apply()
    }


    fun authorizeIntent(): Intent? {
        val authorizeUrl = "https://app.companycam.com/oauth/authorize?".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("client_id", CLIENT_ID)
                ?.addQueryParameter("redirect_uri", REDIRECT_URI)
                ?.addQueryParameter("response_type", "code")
                // TODO: get correct scope for read/write token
//                ?.addQueryParameter("scope", "read+write+destroy")
                ?.build()
                ?: return null

        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(authorizeUrl.toUrl().toString())

        return i
    }


    fun makeService(): CompanyCamService {
        // log json responses
        val logging = HttpLoggingInterceptor()
        logging.apply { logging.level = HttpLoggingInterceptor.Level.BODY }

        val httpClient = OkHttpClient.Builder().addInterceptor(logging).addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                    .addHeader("Content-type", "application/json")

            val token = bearerToken()
            if (token != null) {
                newRequest.addHeader("Authorization", "Bearer $token")
                        // TODO: when multiple users, query and save user's email
                    .addHeader("X-CompanyCam-User", ROOT_USER_EMAIL)
            }

            chain.proceed(newRequest.build())
        }

        val builder = Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())

        return builder
                .client(httpClient.build())
                .build().create(CompanyCamService::class.java)
    }
}