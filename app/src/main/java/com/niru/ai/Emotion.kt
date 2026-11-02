package com.niru.ai

enum class Emotion { NEUTRAL, HAPPY, SAD, WORRIED, ANGRY, SURPRISED, THINKING, LISTENING }

/**
 * Shabdon ko dekhkar andaaza lagata hai ki insaan kaisa mehsoos kar raha hai.
 * Ye camera se chehra padhna nahi hai, sirf likhe hue shabdon par based hai, isliye galat bhi ho sakta hai.
 */
object EmotionDetector {
    private val table: List<Pair<Emotion, List<String>>> = listOf(
        Emotion.SAD to listOf(
            "दुखी", "उदास", "रो रह", "रोना", "अकेला", "अकेली", "बुरा लग", "टूट गया", "टूट गई",
            "मन भारी", "sad", "cry", "lonely", "depress", "miss you", "hurt"
        ),
        Emotion.WORRIED to listOf(
            "डर", "टेंशन", "tension", "परेशान", "चिंता", "घबरा", "नर्वस", "worried", "scared", "anxious", "afraid"
        ),
        Emotion.ANGRY to listOf(
            "गुस्सा", "पागल", "नफरत", "चिढ़", "angry", "hate", "annoy", "frustrat"
        ),
        Emotion.HAPPY to listOf(
            "खुश", "मज़ा", "मजा", "बढ़िया", "शानदार", "मस्त", "जीत", "पास हो", "happy", "great", "awesome", "yay"
        ),
        Emotion.SURPRISED to listOf(
            "सच में", "वाह", "अरे यार", "omg", "wow", "unbelievable"
        )
    )

    fun detect(text: String): Emotion {
        val t = text.lowercase()
        var best = Emotion.NEUTRAL
        var bestScore = 0
        for ((emo, words) in table) {
            val score = words.count { t.contains(it) }
            if (score > bestScore) {
                best = emo
                bestScore = score
            }
        }
        return best
    }
}
