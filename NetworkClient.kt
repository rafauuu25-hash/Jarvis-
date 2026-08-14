package com.jarvis.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Resultado de uma chamada ao servidor JARVIS no PC.
 */
sealed class ServerResult {
    data class Success(val message: String) : ServerResult()
    data class NotFound(val reason: String) : ServerResult()
    data class Error(val reason: String) : ServerResult()
}

/**
 * Cliente HTTP responsavel por conversar com o servidor JARVIS rodando
 * no computador Windows, dentro da mesma rede Wi-Fi local.
 */
class NetworkClient(private val prefs: Prefs) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl(): String = "http://${prefs.pcIp}:${prefs.pcPort}"

    /**
     * Testa a conexao com o servidor chamando o endpoint GET /status.
     */
    suspend fun testConnection(): ServerResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl()}/status")
                .addHeader("X-Jarvis-Token", prefs.token)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ServerResult.Success("PC conectado.")
                } else if (response.code == 401 || response.code == 403) {
                    ServerResult.Error("Token de autenticação inválido.")
                } else {
                    ServerResult.Error("O servidor respondeu com erro (${response.code}).")
                }
            }
        } catch (e: IOException) {
            ServerResult.NotFound("Não consegui encontrar o PC na rede Wi-Fi.")
        } catch (e: Exception) {
            ServerResult.Error("Ocorreu um erro ao testar a conexão.")
        }
    }

    /**
     * Envia um comando identificado (ex: "youtube", "calculator") para o
     * servidor JARVIS executar no PC.
     */
    suspend fun sendCommand(commandId: String): ServerResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("command", commandId)
            }
            val body = json.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("${baseUrl()}/command")
                .addHeader("X-Jarvis-Token", prefs.token)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()

                when {
                    response.code == 401 || response.code == 403 -> {
                        ServerResult.Error("Token de autenticação inválido.")
                    }
                    response.isSuccessful -> {
                        val message = extractMessage(bodyText)
                        ServerResult.Success(message)
                    }
                    else -> {
                        ServerResult.Error("Ocorreu um erro ao executar o comando.")
                    }
                }
            }
        } catch (e: IOException) {
            ServerResult.NotFound("Não consegui conectar ao computador.")
        } catch (e: Exception) {
            ServerResult.Error("Ocorreu um erro ao executar o comando.")
        }
    }

    /**
     * Tenta extrair um campo "message" de uma resposta JSON. Se a resposta
     * nao for JSON valido, retorna o texto puro recebido.
     */
    private fun extractMessage(bodyText: String): String {
        return try {
            val obj = JSONObject(bodyText)
            obj.optString("message", bodyText)
        } catch (e: Exception) {
            bodyText.ifBlank { "Comando executado." }
        }
    }
}
