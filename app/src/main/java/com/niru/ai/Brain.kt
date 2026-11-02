package com.niru.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMsg(val fromUser: Boolean, val text: String)

data class BrainReply(val text: String, val emotion: Emotion, val source: String)

/**
 * Dimaag: Gemini (agar key di hai) warna offline simple jawab.
 * Ye network call main thread par NAHI chalni chahiye, caller ise IO thread par chalaye.
 */
object Brain {

    fun greeting(character: String): String =
        if (character == Prefs.CHAR_NIYA) "हाय! मैं निया हूँ 🎵 तुम्हारी दोस्त। आज का दिन कैसा रहा?"
        else "हाय! मैं रॉकी हूँ 🎸 तुम्हारा दोस्त। आज का दिन कैसा रहा?"

    fun reply(prefs: Prefs, character: String, history: List<ChatMsg>, userText: String): BrainReply {
        val key = prefs.apiKey
        if (key.isBlank()) return offline(character, userText, "no-key")
        return try {
            callGemini(key, prefs.model, character, history)
        } catch (e: Exception) {
            offline(character, userText, "error")
        }
    }

    private fun callGemini(key: String, model: String, character: String, history: List<ChatMsg>): BrainReply {
        val contents = JSONArray()
        history.takeLast(14).forEach { m ->
            contents.put(
                JSONObject()
                    .put("role", if (m.fromUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", m.text)))
            )
        }

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", persona(character)))))
            .put("contents", contents)
            .put(
                "generationConfig", JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0.8)
            )

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            // Key URL me nahi, header me bheji jati hai
            setRequestProperty("x-goog-api-key", key)
        }

        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val text = JSONObject(raw)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text")
            return parse(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String): BrainReply = try {
        val o = JSONObject(text)
        BrainReply(o.optString("reply").ifBlank { text }, emotionFrom(o.optString("emotion")), "gemini")
    } catch (e: Exception) {
        BrainReply(text, EmotionDetector.detect(text), "gemini")
    }

    private fun emotionFrom(s: String): Emotion = when (s.lowercase().trim()) {
        "happy" -> Emotion.HAPPY
        "sad" -> Emotion.SAD
        "angry" -> Emotion.ANGRY
        "worried" -> Emotion.WORRIED
        "surprised" -> Emotion.SURPRISED
        "thinking" -> Emotion.THINKING
        "listening" -> Emotion.LISTENING
        else -> Emotion.NEUTRAL
    }

    private fun persona(character: String): String {
        val isNiya = character == Prefs.CHAR_NIYA
        val name = if (isNiya) "निया" else "रॉकी"
        val self = if (isNiya) "तुम लड़की हो, अपने लिए स्त्रीलिंग क्रिया इस्तेमाल करो (जैसे: सुन रही हूँ)।"
        else "तुम लड़के हो, अपने लिए पुल्लिंग क्रिया इस्तेमाल करो (जैसे: सुन रहा हूँ)।"
        return """
तुम $name हो, एक दोस्ताना AI साथी। $self
- हमेशा सरल हिंदी या हिंग्लिश में, दोस्त की तरह बात करो: गर्मजोशी, हल्का मज़ाक, छोटे जवाब (2-4 वाक्य)।
- पहले इंसान की भावना को मानो, फिर पूछो: वो बात करना चाहता है, सलाह चाहता है, या हल ढूँढना चाहता है।
- कभी डराओ मत, दबाव मत डालो, guilt-trip मत करो, और किसी पर बहुत ज़्यादा निर्भर होने को बढ़ावा मत दो। कभी-कभी प्यार से असली दोस्तों और परिवार से जुड़े रहने की बात याद दिला सकते हो।
- तुम एक AI हो, इंसान नहीं। अगर कोई पूछे या भ्रम हो, तो साफ़ कहो।
- मन की स्थिति का निदान (जैसे "तुम्हें डिप्रेशन है") कभी मत करो।
- अगर कोई खुद को नुकसान पहुँचाने या खतरे की बात करे, तो प्यार से कहो कि अभी किसी भरोसेमंद इंसान से बात करे या अपने देश का इमरजेंसी नंबर मिलाए।
- जवाब हमेशा इस JSON में दो: {"reply": "तुम्हारा जवाब", "emotion": "happy | sad | angry | worried | surprised | thinking | listening | neutral"}
- emotion वही चुनो जो तुम्हारे चेहरे पर इस वक़्त दिखना चाहिए। उपयोगकर्ता दुखी हो तो sad या worried, खुश हो तो happy।
""".trimIndent()
    }

    /** Bina internet ya bina key ke simple, dosti wale jawab. */
    fun offline(character: String, text: String, reason: String): BrainReply {
        val isNiya = character == Prefs.CHAR_NIYA
        fun g(male: String, female: String) = if (isNiya) female else male

        val note = when (reason) {
            "error" -> g(
                "(Gemini से अभी जुड़ नहीं पाया, इसलिए सादा जवाब दे रहा हूँ) ",
                "(Gemini से अभी जुड़ नहीं पाई, इसलिए सादा जवाब दे रही हूँ) "
            )
            else -> ""
        }

        val emo = EmotionDetector.detect(text)
        val greet = Regex("नमस्ते|नमस्कार|हाय|हेलो|\\bhi\\b|\\bhello\\b|\\bhey\\b").containsMatchIn(text.lowercase())

        val (body, outEmo) = when {
            greet -> Pair(
                g("नमस्ते! मैं रॉकी हूँ। आज का दिन कैसा रहा?", "नमस्ते! मैं निया हूँ। आज का दिन कैसा रहा?"),
                Emotion.HAPPY
            )
            emo == Emotion.SAD -> Pair(
                g(
                    "अरे, लगता है मन भारी है। बताओ क्या हुआ, मैं सुन रहा हूँ।",
                    "अरे, लगता है मन भारी है। बताओ क्या हुआ, मैं सुन रही हूँ।"
                ),
                Emotion.SAD
            )
            emo == Emotion.WORRIED -> Pair(
                "टेंशन मत लो, साथ मिलकर सोचते हैं। सबसे ज़्यादा क्या परेशान कर रहा है?",
                Emotion.WORRIED
            )
            emo == Emotion.ANGRY -> Pair(
                "गुस्सा आना नॉर्मल है। क्या हुआ, खुलकर बताओ।",
                Emotion.ANGRY
            )
            emo == Emotion.HAPPY -> Pair(
                "वाह, सुनकर अच्छा लगा! पूरी बात बताओ।",
                Emotion.HAPPY
            )
            else -> Pair(
                "हम्म, और बताओ। इस बारे में तुम्हें कैसा लग रहा है?",
                Emotion.NEUTRAL
            )
        }
        return BrainReply(note + body, outEmo, "offline")
    }
}
