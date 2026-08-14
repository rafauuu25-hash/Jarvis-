package com.jarvis.assistant

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerencia as preferencias do aplicativo (IP do PC, porta, token, etc.)
 * salvas localmente no dispositivo via SharedPreferences.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pcIp: String
        get() = sp.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = sp.edit().putString(KEY_IP, value).apply()

    var pcPort: Int
        get() = sp.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = sp.edit().putInt(KEY_PORT, value).apply()

    var token: String
        get() = sp.getString(KEY_TOKEN, DEFAULT_TOKEN) ?: DEFAULT_TOKEN
        set(value) = sp.edit().putString(KEY_TOKEN, value).apply()

    var voiceEnabled: Boolean
        get() = sp.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "jarvis_prefs"
        private const val KEY_IP = "pc_ip"
        private const val KEY_PORT = "pc_port"
        private const val KEY_TOKEN = "jarvis_token"
        private const val KEY_VOICE_ENABLED = "voice_enabled"

        // Valores padrao (o usuario deve ajustar na tela de configuracoes
        // conforme o IP real do seu computador na rede local).
        const val DEFAULT_IP = "192.168.1.20"
        const val DEFAULT_PORT = 8765
        const val DEFAULT_TOKEN = "jarvis_local_token"
    }
}
