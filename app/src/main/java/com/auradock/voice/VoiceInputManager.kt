package com.auradock.voice

import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.auradock.ui.viewmodel.StandbyViewModel
import java.util.Locale

class VoiceInputManager(
    private val context: Context
) {
    private companion object {
        const val TAG = "VoiceInputManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startSpeechInput(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition unavailable")
            return
        }

        release()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            isListening = true
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    stopListening()
                    onError("Speech error: $error")
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    stopListening()
                    onResult(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        onPartialResult(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
            )
        }
    }

    fun startSpeechToTemplateGeneration(
        viewModel: StandbyViewModel,
        onHeardText: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModel.onVoiceListeningStarted()
        startSpeechInput(
            onResult = { spokenText ->
                if (spokenText.isBlank()) {
                    viewModel.onVoiceInputError()
                    onError("No speech detected")
                    return@startSpeechInput
                }
                Log.d(TAG, "Speech recognized: $spokenText")
                onHeardText(spokenText)
                viewModel.generateFromPrompt(spokenText, fromVoice = true)
            },
            onPartialResult = { partial ->
                viewModel.onVoicePartialTranscript(partial)
            },
            onError = onError
        )
    }

    fun stopListening() {
        if (isListening) {
            runCatching { speechRecognizer?.stopListening() }
            isListening = false
        }
        release()
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}
