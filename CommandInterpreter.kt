package com.jarvis.assistant

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Resultado da interpretacao de um comando falado ou digitado.
 */
sealed class InterpretedCommand {
    /** Comando que deve ser respondido imediatamente, sem envolver o PC. */
    data class Local(val responseText: String) : InterpretedCommand()

    /** Comando que deve ser enviado ao servidor do PC (identificador simples). */
    data class RemoteCommand(val commandId: String) : InterpretedCommand()

    /** Acao especial dentro do proprio aplicativo (ex: abrir configuracoes). */
    data class AppAction(val action: AppActionType) : InterpretedCommand()

    /** Comando nao reconhecido. */
    object Unknown : InterpretedCommand()
}

enum class AppActionType {
    OPEN_SETTINGS,
    TEST_CONNECTION
}

/**
 * Interpreta o texto falado/digitado pelo usuario e decide se a resposta
 * pode ser dada localmente ou se precisa ser enviada ao servidor do PC.
 *
 * Por seguranca, o aplicativo NUNCA envia texto livre como comando de
 * shell: ele so reconhece frases desta lista e as converte para
 * identificadores fixos, ja validados tambem no servidor.
 */
object CommandInterpreter {

    fun interpret(rawText: String): InterpretedCommand {
        val text = normalize(rawText)

        return when {
            containsAny(text, "que horas", "qual é a hora", "qual e a hora", "mostrar hora", "mostrar a hora") ->
                InterpretedCommand.Local(currentTimeResponse())

            containsAny(text, "que dia é hoje", "que dia e hoje", "qual é a data", "qual e a data") ->
                InterpretedCommand.Local(currentDateResponse())

            containsAny(text, "abrir configurações", "abrir configuracoes", "abrir configuracao") ->
                InterpretedCommand.AppAction(AppActionType.OPEN_SETTINGS)

            containsAny(text, "testar conexão", "testar conexao", "testar conexão com o pc") ->
                InterpretedCommand.AppAction(AppActionType.TEST_CONNECTION)

            containsAny(text, "abrir youtube") ->
                InterpretedCommand.RemoteCommand("youtube")

            containsAny(text, "abrir google") ->
                InterpretedCommand.RemoteCommand("google")

            containsAny(text, "abrir calculadora") ->
                InterpretedCommand.RemoteCommand("calculator")

            containsAny(text, "abrir bloco de notas", "abrir bloco de nota") ->
                InterpretedCommand.RemoteCommand("notepad")

            containsAny(text, "abrir explorador de arquivos", "abrir explorador") ->
                InterpretedCommand.RemoteCommand("explorer")

            else -> InterpretedCommand.Unknown
        }
    }

    /** Resposta falada correspondente a cada identificador de comando remoto. */
    fun spokenConfirmation(commandId: String): String {
        return when (commandId) {
            "youtube" -> "Abrindo o YouTube."
            "google" -> "Abrindo o Google."
            "calculator" -> "Calculadora aberta."
            "notepad" -> "Bloco de notas aberto."
            "explorer" -> "Explorador de arquivos aberto."
            "time" -> "Certo."
            else -> "Comando executado."
        }
    }

    private fun currentTimeResponse(): String {
        val format = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
        val now = format.format(Calendar.getInstance().time)
        val (hora, minuto) = now.split(":")
        return "Agora são ${hora} horas e ${minuto} minutos."
    }

    private fun currentDateResponse(): String {
        val format = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        return "Hoje é ${format.format(Calendar.getInstance().time)}."
    }

    private fun normalize(text: String): String {
        return text.trim().lowercase(Locale("pt", "BR"))
            .replace("jarvis", "")
            .trim()
    }

    private fun containsAny(text: String, vararg options: String): Boolean {
        return options.any { text.contains(it) }
    }
}
