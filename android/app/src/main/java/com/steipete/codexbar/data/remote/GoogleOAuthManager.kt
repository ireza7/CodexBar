package com.steipete.codexbar.data.remote

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles the Google OAuth2 loopback authorization flow for Antigravity.
 * Spawns a lightweight local HTTP server on 127.0.0.1:8085, launches Chrome Custom Tabs,
 * captures the authorization code callback, and exchanges it for access/refresh tokens.
 */
object GoogleOAuthManager {

    private const val CLIENT_ID = "1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com"
    private const val CLIENT_SECRET = "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"
    private const val PORT = 8085
    private const val REDIRECT_URI = "http://localhost:$PORT/oauth2callback"
    private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    private const val SCOPES = "https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/userinfo.email"

    private var serverSocket: ServerSocket? = null
    private val isListening = AtomicBoolean(false)

    /**
     * Starts the local loopback receiver, opens Chrome Custom Tabs, and suspends
     * until the user completes the login or an error occurs.
     * Returns the full JSON token response containing access_token and refresh_token.
     */
    suspend fun loginWithBrowser(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            stopServer()

            val server = ServerSocket(PORT)
            serverSocket = server
            isListening.set(true)

            val authUrl = Uri.parse(AUTH_ENDPOINT).buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("prompt", "select_account")
                .appendQueryParameter("access_type", "offline")
                .build()

            withContext(Dispatchers.Main) {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(context, authUrl)
            }

            // Wait for redirect request on loopback socket
            var authCode: String? = null
            server.soTimeout = 120_000 // 2 minutes timeout

            while (isListening.get()) {
                val socket = server.accept()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val firstLine = reader.readLine().orEmpty()

                if (firstLine.startsWith("GET")) {
                    val path = firstLine.split(" ").getOrNull(1).orEmpty()
                    val uri = Uri.parse("http://localhost$path")
                    val code = uri.getQueryParameter("code")
                    val error = uri.getQueryParameter("error")

                    val responseHtml = if (code != null) {
                        authCode = code
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1">
                            <title>Antigravity Login Successful</title>
                            <style>
                                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0F1117; color: #FFFFFF; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 90vh; text-align: center; padding: 20px; }
                                .card { background: #1A1D24; padding: 30px; border-radius: 16px; border: 1px solid #2D3139; max-width: 400px; }
                                h1 { color: #34A853; font-size: 22px; margin-bottom: 10px; }
                                p { color: #9AA0A6; font-size: 14px; line-height: 1.5; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <h1>✓ Authentication Successful</h1>
                                <p>You can now return to <b>Antigravity Quota Widget</b>. Your quota is now connected!</p>
                            </div>
                        </body>
                        </html>
                        """.trimIndent()
                    } else {
                        """
                        <!DOCTYPE html>
                        <html>
                        <head><title>Login Failed</title></head>
                        <body style="background:#0F1117; color:#fff; font-family:sans-serif; text-align:center; padding-top:50px;">
                            <h2 style="color:#EA4335;">Login Error</h2>
                            <p>${error ?: "No authorization code received"}</p>
                        </body>
                        </html>
                        """.trimIndent()
                    }

                    val os = socket.getOutputStream()
                    val header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${responseHtml.toByteArray().size}\r\n" +
                            "Connection: close\r\n\r\n"
                    os.write(header.toByteArray())
                    os.write(responseHtml.toByteArray())
                    os.flush()
                    socket.close()

                    if (authCode != null || error != null) {
                        break
                    }
                } else {
                    socket.close()
                }
            }

            stopServer()

            if (authCode == null) {
                return@withContext Result.failure(Exception("Authentication was cancelled or timed out."))
            }

            // Exchange authorization code for tokens
            exchangeCodeForToken(authCode)
        } catch (e: Exception) {
            stopServer()
            Result.failure(e)
        }
    }

    private fun exchangeCodeForToken(code: String): Result<String> {
        val client = ResilientHttpClientFactory.createClient()
        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", REDIRECT_URI)
            .build()

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.contains("access_token")) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to exchange code: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes an expired access token using the refresh token.
     */
    fun refreshAccessToken(refreshToken: String): Result<String> {
        val client = ResilientHttpClientFactory.createClient()
        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build()

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.contains("access_token")) {
                    val json = JSONObject(body)
                    val newAccessToken = json.getString("access_token")
                    Result.success(newAccessToken)
                } else {
                    Result.failure(Exception("Failed to refresh token: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun stopServer() {
        isListening.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
