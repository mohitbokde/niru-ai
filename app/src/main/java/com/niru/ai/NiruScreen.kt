@file:OptIn(ExperimentalMaterial3Api::class)

package com.niru.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NiruScreen(prefs: Prefs) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var character by remember { mutableStateOf(prefs.character) }
    val messages = remember { mutableStateListOf(ChatMsg(false, Brain.greeting(prefs.character))) }

    var voiceOn by remember { mutableStateOf(false) }   // app khulne par hamesha OFF
    var paused by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var talking by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var emotion by remember { mutableStateOf(Emotion.NEUTRAL) }
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    val voice = remember { NiruVoice(context) }
    DisposableEffect(Unit) {
        onDispose { voice.release() }
    }

    // Notification ke Stop / Pause buttons yahan se sunte hain
    DisposableEffect(Unit) {
        VoiceService.onStopRequested = {
            voiceOn = false
            prefs.voiceMode = false
            paused = false
            talking = false
            listening = false
            voice.stopSpeaking()
            voice.stopListening()
            status = "वॉइस मोड बंद"
        }
        VoiceService.onPauseChanged = { p ->
            paused = p
            if (p) voice.stopSpeaking()
        }
        onDispose {
            VoiceService.onStopRequested = null
            VoiceService.onPauseChanged = null
        }
    }

    fun speak(text: String) {
        talking = true
        voice.speak(text) { talking = false }
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || thinking) return
        messages.add(ChatMsg(true, text))
        input = ""
        emotion = EmotionDetector.detect(text)
        thinking = true
        val history = messages.toList()
        scope.launch {
            val reply = withContext(Dispatchers.IO) {
                Brain.reply(prefs, character, history, text)
            }
            thinking = false
            emotion = reply.emotion
            messages.add(ChatMsg(false, reply.text))
            if (voiceOn && !VoiceService.paused) speak(reply.text)
        }
    }

    voice.onResult = { t ->
        listening = false
        send(t)
    }
    voice.onState = { m ->
        listening = false
        status = m
    }

    fun startMic() {
        listening = true
        status = "सुन रही हूँ..."
        voice.startListening()
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startMic() else status = "माइक की इजाज़त चाहिए, तभी बोल पाऊँगी"
    }

    fun onMicTap() {
        if (listening) {
            voice.stopListening()
            listening = false
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) startMic() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun setVoice(on: Boolean) {
        voiceOn = on
        prefs.voiceMode = on
        paused = false
        if (on) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            VoiceService.paused = false
            VoiceService.start(context)
            status = "वॉइस मोड चालू है। ऐप बैकग्राउंड में भी बोल सकती है।"
        } else {
            voice.stopSpeaking()
            voice.stopListening()
            talking = false
            listening = false
            VoiceService.stop(context)
            status = "वॉइस मोड बंद है। अब कुछ नहीं बोलेगी और न सुनेगी।"
        }
    }

    fun togglePause() {
        paused = !paused
        VoiceService.setPaused(context, paused)
        if (paused) voice.stopSpeaking()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // Upar ki line: naam, chat saaf, settings
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("NIRU AI", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                messages.clear()
                messages.add(ChatMsg(false, Brain.greeting(character)))
                emotion = Emotion.NEUTRAL
            }) { Text("चैट साफ़") }
            TextButton(onClick = { showSettings = true }) { Text("सेटिंग") }
        }

        // Character chooser
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = character == Prefs.CHAR_NIYA,
                onClick = {
                    character = Prefs.CHAR_NIYA
                    prefs.character = character
                    messages.add(ChatMsg(false, Brain.greeting(character)))
                },
                label = { Text("निया (लड़की)") }
            )
            FilterChip(
                selected = character == Prefs.CHAR_ROCKY,
                onClick = {
                    character = Prefs.CHAR_ROCKY
                    prefs.character = character
                    messages.add(ChatMsg(false, Brain.greeting(character)))
                },
                label = { Text("रॉकी (लड़का)") }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Character
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                NiruCharacter(
                    character = character,
                    emotion = if (thinking) Emotion.THINKING else emotion,
                    talking = talking
                )
                Text(
                    emotionLabel(if (thinking) Emotion.THINKING else emotion),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(6.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // VOICE MODE toggle: bada aur saaf
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("वॉइस मोड", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (voiceOn) "ON: NIRU बोलेगी (बैकग्राउंड में भी)" else "OFF: कुछ नहीं बोलेगी, न सुनेगी",
                        fontSize = 12.sp
                    )
                }
                Switch(checked = voiceOn, onCheckedChange = { setVoice(it) })
            }
            if (voiceOn) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { togglePause() }) {
                        Text(if (paused) "फिर से चालू" else "रोकें")
                    }
                    OutlinedButton(onClick = { voice.stopSpeaking(); talking = false }) {
                        Text("अभी चुप कराओ")
                    }
                }
            }
            if (status.isNotEmpty()) {
                Text(
                    status,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                )
            }
        }

        // Chat
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            items(messages) { m -> Bubble(m) }
        }

        // Input
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("कुछ कहो...") },
                maxLines = 3
            )
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = { onMicTap() }) {
                Text(if (listening) "रोकें" else "🎤")
            }
            Spacer(Modifier.width(6.dp))
            Button(onClick = { send(input) }, enabled = !thinking && input.isNotBlank()) {
                Text("भेजें")
            }
        }
    }

    if (showSettings) {
        var keyDraft by remember { mutableStateOf(prefs.apiKey) }
        var modelDraft by remember { mutableStateOf(prefs.model) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("सेटिंग") },
            text = {
                Column {
                    Text(
                        "Gemini API key: सिर्फ़ इसी फ़ोन में सेव होगी। Key न डालो तो NIRU offline सादे जवाब देगी।",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        singleLine = true,
                        label = { Text("API key") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelDraft,
                        onValueChange = { modelDraft = it },
                        singleLine = true,
                        label = { Text("Model") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.apiKey = keyDraft
                    prefs.model = modelDraft
                    showSettings = false
                    status = "सेटिंग सेव हो गई"
                }) { Text("सेव") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("रद्द") }
            }
        )
    }
}

@Composable
private fun Bubble(m: ChatMsg) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (m.fromUser) Color(0xFFD9CCFF) else Color.White,
            shadowElevation = 1.dp
        ) {
            Text(
                m.text,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 280.dp)
            )
        }
    }
}

private fun emotionLabel(e: Emotion): String = when (e) {
    Emotion.HAPPY -> "खुश 😊"
    Emotion.SAD -> "उदास 😢"
    Emotion.ANGRY -> "गुस्सा 😠"
    Emotion.WORRIED -> "चिंतित 😟"
    Emotion.SURPRISED -> "हैरान 😮"
    Emotion.THINKING -> "सोच में... 🤔"
    Emotion.LISTENING -> "ध्यान से सुन रहे हैं"
    Emotion.NEUTRAL -> "शांत"
}
