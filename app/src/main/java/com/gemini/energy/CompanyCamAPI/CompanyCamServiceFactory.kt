package CompanyCamAPI

import CompanyCamAPI.Requests.Auth2RequestParameters
import CompanyCamAPI.Responses.Auth2Response
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.gemini.energy.App
import com.gemini.energy.CompanyCamAPI.Requests.RefreshTokenRequest
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


object CompanyCamServiceFactory {

    private const val apiBaseUrl = "https://app.companycam.com/"
    private const val rootUserEmail = "akinslow2@geminiesolutions.com"

    private const val clientId = "8418eeee9e98af39e69160ba9e4127d55dcbd0e3f88a381e060c0ed571ca3e73"
    private const val secretKey = "c1a14dd4c9099eed46c03f2b0394519412677a42a940d00605a1b65ceffe5722"
    private const val redirectUri = "https://www.geminiesolutions.com/companycamauth"

    private const val tokenPreferenceName = "companycamBearerToken"
    private const val bearerTokenKey = "bearer-token"
    private const val refreshTokenKey = "refresh_token"
    private const val accessExpiresAtKey = "access_expires_at"

    // returns succsess
    fun attemptTokenRefresh() {
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            Log.d("------", "error refreshing access token with company cam")
//            return false
        }

        val mainActivity = Job()
        val scope = CoroutineScope((mainActivity + Dispatchers.Main))
                .launch(errorHandler) {
                    val newToken = makeService().refreshAccessToken(RefreshTokenRequest(clientId, secretKey, "refresh token", redirectUri))
                    setToken(newToken)
//                    return true
                }
    }


    fun setToken(data: Uri) {
        val code = data.getQueryParameter("code") ?: return

        //1 Create a Coroutine scope using a job to be able to cancel when needed
        val mainActivityJob = Job()

        //2 Handle exceptions if any
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            Log.d("------", "error authorizing with company cam")
        }

        //3 the Coroutine runs using the Main (UI) dispatcher
        val coroutineScope = CoroutineScope(mainActivityJob + Dispatchers.Main)
        coroutineScope.launch(errorHandler) {

            val token = makeService().getAuthToken(Auth2RequestParameters(clientId, secretKey, code, redirectUri))
            Log.d("------", "token $token")
            setToken(token)
        }
    }

    private fun setToken(token: Auth2Response) {
        val access = token.access_token
        val refresh = token.refresh_token
        val expiresAt = token.created_at + token.expires_in
        val expireDate = Date(expiresAt.toLong() * 1000)

        App.instance.getSharedPreferences(tokenPreferenceName, Context.MODE_PRIVATE)
                .edit()
                .putString(bearerTokenKey, token.access_token)
                .putString(refreshTokenKey, token.refresh_token)
                .putInt(accessExpiresAtKey, expiresAt)
                .apply()
    }

    fun tokenExpiration(): Date? {
        val prefs = App.instance.getSharedPreferences(tokenPreferenceName, Context.MODE_PRIVATE)
        val timestamp = prefs.getInt(accessExpiresAtKey, 0)
        val refreshToken = prefs.getString(refreshTokenKey, "")
        val bearer = prefs.getString(bearerTokenKey, "")
        if (timestamp == 0) return null
        return Date(timestamp.toLong() * 1000)
    }

    fun bearerToken(): String? {
        val prefs: SharedPreferences = App.instance.getSharedPreferences(tokenPreferenceName, Context.MODE_PRIVATE)
        return prefs.getString(bearerTokenKey, null)
    }

//    fun clearAuthToken() {
//        val prefs: SharedPreferences = App.instance.getSharedPreferences(tokenPreferenceName, Context.MODE_PRIVATE)
//        val editor: SharedPreferences.Editor = prefs.edit()
//        editor.remove(bearerTokenKey)
//        editor.apply()
//    }

    fun logout() {
        App.instance.getSharedPreferences(tokenPreferenceName, Context.MODE_PRIVATE)
                .edit()
                .remove(bearerTokenKey)
                .remove(refreshTokenKey)
                .remove(accessExpiresAtKey)
                .apply()
    }


    fun authorizeIntent(): Intent? {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://app.companycam.com/oauth/authorize?client_id=8418eeee9e98af39e69160ba9e4127d55dcbd0e3f88a381e060c0ed571ca3e73&redirect_uri=https%3A%2F%2Fwww.geminiesolutions.com%2Fcompanycamauth&response_type=code&scope=read+write+destroy")

        return intent
    }


    fun makeService(): CompanyCamService {
        // log json responses
        val logging = HttpLoggingInterceptor()
        logging.apply { logging.level = HttpLoggingInterceptor.Level.BODY }

        val httpClient = OkHttpClient.Builder().addInterceptor(logging).addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                    .addHeader("Content-type", "application/json")
                    .addHeader("X-CompanyCam-Secret", secretKey)

            val token = bearerToken()
            if (token != null) {
                newRequest.addHeader("Authorization", "Bearer $token")
                        // TODO: when multiple users, query and save user's email
                        .addHeader("X-CompanyCam-User", rootUserEmail)
                        .addHeader("Connection", "keep-alive")
            }

            chain.proceed(newRequest.build())
        }

        val builder = Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())

        return builder
                .client(httpClient.build())
                .build().create(CompanyCamService::class.java)
    }
}