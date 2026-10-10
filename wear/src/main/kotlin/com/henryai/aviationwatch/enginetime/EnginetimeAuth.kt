package com.henryai.aviationwatch.enginetime

import android.content.Context
import android.util.Base64
import com.henryai.aviationwatch.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Google sign-in for Enginetime Drive sync using the OAuth 2.0 Device Authorization Grant
 * (RFC 8628), one of the two flows Wear OS recommends: the watch shows a short code that
 * is entered once at google.com/device on the phone. The refresh token is kept in
 * app-private storage; access tokens are refreshed automatically.
 */
object EnginetimeAuth {
    private const val DEVICE_CODE_URL = "https://oauth2.googleapis.com/device/code"
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    private const val SCOPE = "openid email https://www.googleapis.com/auth/drive.appdata"
    private const val PREFS = "enginetime_auth"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EMAIL = "email"
    private const val EXPIRY_MARGIN_MS = 60_000L

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var accessTokenExpiry = 0L

    /** False when the APK was built without an Enginetime OAuth client (see docs/ENGINETIME.md). */
    val isConfigured: Boolean
        get() = BuildConfig.ENGINETIME_CLIENT_ID.isNotBlank() && BuildConfig.ENGINETIME_CLIENT_SECRET.isNotBlank()

    data class DeviceCode(
        val deviceCode: String,
        val userCode: String,
        val verificationUrl: String,
        val expiresInSeconds: Int,
        val intervalSeconds: Int,
    )

    sealed interface PollResult {
        data object Pending : PollResult
        data object SlowDown : PollResult
        data class SignedIn(val email: String?) : PollResult
        data object Denied : PollResult
        data object Expired : PollResult
        data class Failed(val message: String) : PollResult
    }

    /** Thrown when the stored sign-in was revoked; the user has to sign in again. */
    class SignInRequiredException : IOException("Enginetime sign-in required")

    fun signedInEmail(context: Context): String? =
        prefs(context).takeIf { it.contains(KEY_REFRESH) }?.let { it.getString(KEY_EMAIL, null) ?: "Google account" }

    fun signOut(context: Context) {
        prefs(context).edit().clear().commit()
        cachedAccessToken = null
        accessTokenExpiry = 0
    }

    fun invalidateAccessToken() {
        cachedAccessToken = null
        accessTokenExpiry = 0
    }

    suspend fun requestDeviceCode(): DeviceCode = withContext(Dispatchers.IO) {
        val response = Http.postForm(
            DEVICE_CODE_URL,
            mapOf("client_id" to BuildConfig.ENGINETIME_CLIENT_ID, "scope" to SCOPE),
        )
        val json = parse(response)
        if (!response.isSuccess) throw IOException(errorMessage(json, response))
        DeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUrl = json.optString("verification_url", json.optString("verification_uri", "google.com/device")),
            expiresInSeconds = json.optInt("expires_in", 1800),
            intervalSeconds = json.optInt("interval", 5),
        )
    }

    suspend fun poll(context: Context, code: DeviceCode): PollResult = withContext(Dispatchers.IO) {
        val response = Http.postForm(
            TOKEN_URL,
            mapOf(
                "client_id" to BuildConfig.ENGINETIME_CLIENT_ID,
                "client_secret" to BuildConfig.ENGINETIME_CLIENT_SECRET,
                "device_code" to code.deviceCode,
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
            ),
        )
        val json = parse(response)
        if (response.isSuccess) {
            val refresh = json.optString("refresh_token")
            if (refresh.isEmpty()) return@withContext PollResult.Failed("No refresh token returned")
            val email = emailFromIdToken(json.optString("id_token"))
            prefs(context).edit().putString(KEY_REFRESH, refresh).putString(KEY_EMAIL, email).commit()
            storeAccessToken(json)
            return@withContext PollResult.SignedIn(email)
        }
        when (json.optString("error")) {
            "authorization_pending" -> PollResult.Pending
            "slow_down" -> PollResult.SlowDown
            "access_denied" -> PollResult.Denied
            "expired_token" -> PollResult.Expired
            else -> PollResult.Failed(errorMessage(json, response))
        }
    }

    /** A valid access token, refreshed if needed. Null when not signed in. */
    suspend fun accessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val cached = cachedAccessToken
        if (cached != null && System.currentTimeMillis() < accessTokenExpiry - EXPIRY_MARGIN_MS) {
            return@withContext cached
        }
        val refresh = prefs(context).getString(KEY_REFRESH, null) ?: return@withContext null
        val response = Http.postForm(
            TOKEN_URL,
            mapOf(
                "client_id" to BuildConfig.ENGINETIME_CLIENT_ID,
                "client_secret" to BuildConfig.ENGINETIME_CLIENT_SECRET,
                "refresh_token" to refresh,
                "grant_type" to "refresh_token",
            ),
        )
        val json = parse(response)
        if (!response.isSuccess) {
            if (json.optString("error") == "invalid_grant") {
                signOut(context)
                throw SignInRequiredException()
            }
            throw IOException(errorMessage(json, response))
        }
        storeAccessToken(json)
        cachedAccessToken
    }

    private fun storeAccessToken(json: JSONObject) {
        cachedAccessToken = json.optString("access_token").takeIf { it.isNotEmpty() }
        accessTokenExpiry = System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun parse(response: Http.Response): JSONObject =
        try {
            JSONObject(response.body)
        } catch (e: JSONException) {
            JSONObject()
        }

    private fun errorMessage(json: JSONObject, response: Http.Response): String =
        json.optString("error_description").ifEmpty { json.optString("error") }.ifEmpty { "HTTP ${response.code}" }

    private fun emailFromIdToken(idToken: String): String? = try {
        val payload = idToken.split(".").getOrNull(1) ?: return null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        JSONObject(String(decoded, Charsets.UTF_8)).optString("email").ifEmpty { null }
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: JSONException) {
        null
    }
}
