package com.jarvis.assistant

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs
    private lateinit var networkClient: NetworkClient

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        networkClient = NetworkClient(prefs)

        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        binding.etIp.setText(prefs.pcIp)
        binding.etPort.setText(prefs.pcPort.toString())
        binding.etToken.setText(prefs.token)
        binding.switchVoice.isChecked = prefs.voiceEnabled
    }

    private fun setupListeners() {
        binding.btnTestConnection.setOnClickListener {
            saveSettingsFromFields()
            testConnection()
        }

        binding.btnSaveSettings.setOnClickListener {
            saveSettingsFromFields()
            Toast.makeText(this, "Configurações salvas.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveSettingsFromFields() {
        val ip = binding.etIp.text?.toString()?.trim().orEmpty()
        val portText = binding.etPort.text?.toString()?.trim().orEmpty()
        val token = binding.etToken.text?.toString()?.trim().orEmpty()

        if (ip.isNotBlank()) prefs.pcIp = ip
        val port = portText.toIntOrNull()
        if (port != null) prefs.pcPort = port
        if (token.isNotBlank()) prefs.token = token
        prefs.voiceEnabled = binding.switchVoice.isChecked
    }

    private fun testConnection() {
        binding.tvTestResult.text = getString(R.string.pc_checking)
        activityScope.launch {
            when (val result = networkClient.testConnection()) {
                is ServerResult.Success -> {
                    binding.tvTestResult.text = getString(R.string.pc_connected)
                }
                is ServerResult.NotFound -> {
                    binding.tvTestResult.text = result.reason
                }
                is ServerResult.Error -> {
                    binding.tvTestResult.text = result.reason
                }
            }
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
