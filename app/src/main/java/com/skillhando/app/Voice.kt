package com.skillhando.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import java.util.Locale

class Voice(private val context: Activity) {
    var listening by mutableStateOf(false)
        private set
    var transcript by mutableStateOf("")
    var status by mutableStateOf("")
        private set
    var spokenAvailable by mutableStateOf(false)
        private set
    val recognitionAvailable get() = SpeechRecognizer.isRecognitionAvailable(context)
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { result -> spokenAvailable = result == TextToSpeech.SUCCESS }
    }

    fun listen(locale: Locale) {
        if (!recognitionAvailable || listening) return
        transcript = ""
        status = ""
        try {
            val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onResults(results: Bundle?) {
                    listening = false
                    transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (transcript.isBlank()) status = tr("no_speech_was_recognised_try_again_or_type_a_command")
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onError(error: Int) {
                    listening = false
                    status = tr("speech_recognition_is_unavailable_for_this_request_try_again_or_type_a_command")
                }
            })
            listening = true
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag()))
        } catch (_: RuntimeException) {
            listening = false
            status = tr("speech_recognition_could_not_start")
        }
    }

    fun speak(words: String, locale: Locale) {
        val engine = tts
        if (!spokenAvailable || engine == null || engine.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE ||
            engine.speak(words.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, null, "guidance") != TextToSpeech.SUCCESS) {
            status = tr("this_device_has_no_available_voice_for_the_selected_language")
        } else {
            status = ""
        }
    }

    fun cancel() { recognizer?.cancel(); listening = false; tts?.stop() }
    fun close() { recognizer?.destroy(); recognizer = null; tts?.shutdown(); tts = null }
}

enum class VoiceIntent { PROGRESS, READ, PAUSE, RESUME, NEXT, GOALS, FINISH }

object VoiceCommands {
    private val phrases = mapOf(
        "en" to listOf("show my progress", "read today's challenge", "pause the timer", "resume practice", "what should i practise next", "read my learning goals", "mark this exercise complete"),
        "es" to listOf("muestra mi progreso", "lee el reto de hoy", "pausa el temporizador", "reanuda la práctica", "qué debo practicar ahora", "lee mis metas", "termina este ejercicio"),
        "fr" to listOf("montre mes progrès", "lis le défi du jour", "mets le minuteur en pause", "reprends la pratique", "que dois-je pratiquer ensuite", "lis mes objectifs", "termine cet exercice"),
        "de" to listOf("zeige meinen fortschritt", "lies die heutige übung vor", "pausiere den timer", "setze die übung fort", "was soll ich als nächstes üben", "lies meine lernziele vor", "schließe diese übung ab"),
        "it" to listOf("mostra i miei progressi", "leggi la sfida di oggi", "metti in pausa il timer", "riprendi la pratica", "cosa devo praticare ora", "leggi i miei obiettivi", "termina questo esercizio"),
        "pt" to listOf("mostra o meu progresso", "lê o desafio de hoje", "pausa o temporizador", "retoma a prática", "o que devo praticar a seguir", "lê os meus objetivos", "termina este exercício"),
        "nl" to listOf("toon mijn voortgang", "lees de uitdaging van vandaag", "pauzeer de timer", "hervat de oefening", "wat moet ik hierna oefenen", "lees mijn leerdoelen", "rond deze oefening af"),
        "pl" to listOf("pokaż moje postępy", "przeczytaj dzisiejsze wyzwanie", "wstrzymaj minutnik", "wznów ćwiczenie", "co mam teraz ćwiczyć", "przeczytaj moje cele", "zakończ to ćwiczenie"),
        "sv" to listOf("visa mina framsteg", "läs dagens utmaning", "pausa timern", "fortsätt öva", "vad ska jag öva på härnäst", "läs mina mål", "avsluta övningen"),
        "ja" to listOf("進捗を見せて", "今日の課題を読んで", "タイマーを一時停止して", "練習を再開して", "次は何を練習する", "学習目標を読んで", "この練習を完了して"),
        "hi" to listOf("मेरी प्रगति दिखाओ", "आज का अभ्यास पढ़ो", "टाइमर रोक दो", "अभ्यास जारी रखो", "अब मैं क्या अभ्यास करूँ", "मेरे लक्ष्य पढ़ो", "यह अभ्यास पूरा करो"),
        "ar" to listOf("اعرض تقدمي", "اقرأ تحدي اليوم", "أوقف المؤقت مؤقتا", "استأنف التدريب", "ماذا أتدرب بعد ذلك", "اقرأ أهدافي", "أكمل هذا التمرين"),
        "tr" to listOf("ilerlememi göster", "bugünün alıştırmasını oku", "zamanlayıcıyı duraklat", "alıştırmaya devam et", "sırada neyi çalışmalıyım", "öğrenme hedeflerimi oku", "bu alıştırmayı tamamla"),
        "id" to listOf("tampilkan kemajuan saya", "bacakan tantangan hari ini", "jeda pewaktu", "lanjutkan latihan", "apa yang perlu saya latih berikutnya", "bacakan tujuan belajar saya", "selesaikan latihan ini")
    )

    fun examples(locale: Locale) = phrases[locale.language] ?: phrases.getValue("en")

    fun parse(text: String, locale: Locale): VoiceIntent? {
        val normalized = text.lowercase(locale).trim().trim('¿', '؟', '?', '.', '!', '。', '？', '！')
            .replace('’', '\'').replace(Regex("\\s+"), " ")
        val index = examples(locale).indexOf(normalized)
        if (index >= 0) return VoiceIntent.entries[index]
        return if (locale.language == "en" && normalized == "what should i practice next") VoiceIntent.NEXT else null
    }
}
