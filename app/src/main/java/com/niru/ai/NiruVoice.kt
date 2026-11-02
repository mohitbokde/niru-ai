package com.niru.ai

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Bolna (TTS) aur sunna (STT). Sab callbacks main thread par aate hain.
 * Mic sirf tab chalta hai jab startListening() call ho (user ke tap se).
 */
class NiruVoice(private val context: Context) : TextToSpeech.OnInitListener {

    var onResult: (String) -> Unit = {}
    var onState: (String) -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private var ttsReady = false
    private var pendingDone: (() -> Unit)? = null
    private var recognizer: SpeechRecognizer? = null
    private val tts: TextToSpeech = TextToSpeech(context, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            onState("फ़ोन का बोलने वाला इंजन चालू नहीं हुआ")
            return
        }
        val r = tts.setLanguage(Locale("hi", "IN"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale("en", "IN"))
            onState("Hindi आवाज़ इस फ़ोन में नहीं है, Google TTS से Hindi voice डाउनलोड करो")
        }
        tts.setSpeechRate(0.95f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                main.post { pendingDone?.invoke(); pendingDone = null }
            }
            override fun onError(utteranceId: String?) {
                main.post { pendingDone?.invoke(); pendingDone = null }
            }
        })
        ttsReady = true
    }

    fun speak(text: String, onDone: () -> Unit) {
        if (!ttsReady) {
            onDone()
            return
        }
        pendingDone = onDone
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stopSpeaking() {
        tts.stop()
        pendingDone?.invoke()
        pendingDone = null
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onState("इस फ़ोन में बोलकर लिखने की सुविधा नहीं है")
            return
        }
        stopListening()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                onState(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "सुन नहीं पाई, फिर से बोलो"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक की इजाज़त चाहिए"
                        else -> "बोलने में दिक्कत आई (code $error)"
                    }
                )
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) onResult(text)
                else onState("सुन नहीं पाई, फिर से बोलो")
            }
        })
        sr.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
        )
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    fun release() {
        stopListening()
        tts.stop()
        tts.shutdown()
    }
}
