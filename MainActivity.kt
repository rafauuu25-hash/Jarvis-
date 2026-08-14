package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var networkClient: NetworkClient

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var isListening = false

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private val requestMicPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            Toast.makeText(this, getString(R.string.permission_mic_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        networkClient = NetworkClient(prefs)

        textToSpeech = TextToSpeech(this, this)
        setupSpeechRecognizer()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPcConnection()
    }

    // ---------------------------------------------------------------
    // Configuracao inicial
    // ---------------------------------------------------------------

    private fun setupListeners() {
        binding.btnMic.setOnClickListener { onMicButtonClicked() }

        binding.btnSend.setOnClickListener { sendTypedCommand() }

        binding.etCommand.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnter) {
                sendTypedCommand()
                true
            } else {
                false
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    setListeningState(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    setListeningState(false)
                }

                override fun onError(error: Int) {
                    setListeningState(false)
                }

                override fun onResults(results: Bundle?) {
                    setListeningState(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        handleUserInput(spokenText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("pt", "BR"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    // ---------------------------------------------------------------
    // Microfone / reconhecimento de voz
    // ---------------------------------------------------------------

    private fun onMicButtonClicked() {
        if (isListening) {
            // Ja esta ouvindo: nao faz nada (evita escuta continua indesejada).
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            Toast.makeText(this, "Reconhecimento de voz indisponível neste aparelho.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.startListening(intent)
    }

    private fun setListeningState(listening: Boolean) {
        isListening = listening
        binding.btnMic.setBackgroundResource(
            if (listening) R.drawable.bg_mic_listening else R.drawable.bg_mic_idle
        )
        binding.tvListeningIndicator.text = getString(
            if (listening) R.string.status_listening else R.string.status_idle
        )
    }

    // ---------------------------------------------------------------
    // Envio manual de comando
    // ---------------------------------------------------------------

    private fun sendTypedCommand() {
        val text = binding.etCommand.text?.toString().orEmpty()
        if (text.isBlank()) return
        binding.etCommand.setText("")
        handleUserInput(text)
    }

    // ---------------------------------------------------------------
    // Interpretacao e execucao de comandos
    // ---------------------------------------------------------------

    private fun handleUserInput(text: String) {
        binding.tvUserSpeech.text = text
        binding.tvListeningIndicator.text = getString(R.string.status_processing)

        when (val result = CommandInterpreter.interpret(text)) {
            is InterpretedCommand.Local -> {
                showAndSpeakResponse(result.responseText)
            }
            is InterpretedCommand.AppAction -> {
                handleAppAction(result.action)
            }
            is InterpretedCommand.RemoteCommand -> {
                sendRemoteCommand(result.commandId)
            }
            InterpretedCommand.Unknown -> {
                showAndSpeakResponse(getString(R.string.error_unknown_command))
            }
        }

        binding.tvListeningIndicator.text = getString(R.string.status_idle)
    }

    private fun handleAppAction(action: AppActionType) {
        when (action) {
            AppActionType.OPEN_SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            AppActionType.TEST_CONNECTION -> {
                checkPcConnection(showFeedback = true)
            }
        }
    }

    private fun sendRemoteCommand(commandId: String) {
        activityScope.launch {
            when (val result = networkClient.sendCommand(commandId)) {
                is ServerResult.Success -> {
                    val spoken = CommandInterpreter.spokenConfirmation(commandId)
                    val messageFromServer = result.message.ifBlank { spoken }
                    showAndSpeakResponse(messageFromServer)
                    updateConnectionStatus(connected = true)
                }
                is ServerResult.NotFound -> {
                    showAndSpeakResponse(getString(R.string.error_no_pc))
                    updateConnectionStatus(connected = false)
                }
                is ServerResult.Error -> {
                    showAndSpeakResponse(result.reason.ifBlank { getString(R.string.error_server) })
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Conexao com o PC
    // ---------------------------------------------------------------

    private fun checkPcConnection(showFeedback: Boolean = false) {
        binding.tvConnectionStatus.text = getString(R.string.pc_checking)
        activityScope.launch {
            when (val result = networkClient.testConnection()) {
                is ServerResult.Success -> {
                    updateConnectionStatus(connected = true)
                    if (showFeedback) showAndSpeakResponse(getString(R.string.pc_connected))
                }
                is ServerResult.NotFound -> {
                    updateConnectionStatus(connected = false)
                    if (showFeedback) showAndSpeakResponse(result.reason)
                }
                is ServerResult.Error -> {
                    updateConnectionStatus(connected = false)
                    if (showFeedback) showAndSpeakResponse(result.reason)
                }
            }
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        binding.tvConnectionStatus.text = getString(
            if (connected) R.string.pc_connected else R.string.pc_disconnected
        )
        binding.dotConnection.setBackgroundResource(
            if (connected) R.drawable.dot_connected else R.drawable.dot_disconnected
        )
    }

    // ---------------------------------------------------------------
    // Saida de texto / voz
    // ---------------------------------------------------------------

    private fun showAndSpeakResponse(message: String) {
        binding.tvJarvisResponse.text = message
        if (prefs.voiceEnabled && ttsReady) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        activityScope.cancel()
        super.onDestroy()
    }
}
