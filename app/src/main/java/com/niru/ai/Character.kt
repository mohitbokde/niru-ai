package com.niru.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Ek chehre ki "target" settings. Har emotion ke liye alag. Animation se ye smooth badalti hain. */
private class Face(
    val curve: Float,      // muskan: + khush, - udaas
    val eyeOpen: Float,    // aankh kitni khuli
    val brow: Float,       // bhaun: + gussa, - udaasi/chinta
    val mouthO: Float,     // 1 = O jaisa munh (hairani)
    val gazeX: Float,      // aankh kis taraf dekh rahi
    val gazeY: Float,
    val blush: Float,      // gaal ki laali
    val tears: Float,      // aansu
    val tilt: Float        // sir ka jhukav (degree)
)

private fun faceFor(e: Emotion): Face = when (e) {
    Emotion.NEUTRAL   -> Face(0.25f, 1.00f,  0.00f, 0f,  0f,    0f,    0f, 0f,  0f)
    Emotion.HAPPY     -> Face(1.00f, 0.65f,  0.10f, 0f,  0f,    0f,    1f, 0f,  4f)
    Emotion.SAD       -> Face(-0.80f, 0.85f, -0.60f, 0f, 0f,    0.15f, 0f, 1f, -6f)
    Emotion.WORRIED   -> Face(-0.30f, 1.05f, -0.50f, 0f, -0.2f, 0f,   0f, 0f, -3f)
    Emotion.ANGRY     -> Face(-0.60f, 0.90f,  0.90f, 0f,  0f,    0f,   0.3f, 0f, 2f)
    Emotion.SURPRISED -> Face(0.00f, 1.30f,  0.60f, 1f,  0f,    -0.1f, 0f, 0f,  0f)
    Emotion.THINKING  -> Face(-0.10f, 0.90f,  0.30f, 0f, 0.6f, -0.6f, 0f, 0f,  6f)
    Emotion.LISTENING -> Face(0.30f, 1.00f,  0.20f, 0f,  0f,    0f,    0f, 0f,  0f)
}

private class Pal(val outfit: Color, val hair: Color, val iris: Color)

private val NIYA_PAL = Pal(Color(0xFF8E6CFF), Color(0xFF2B1B3D), Color(0xFF6A4CC2))
private val ROCKY_PAL = Pal(Color(0xFF2F6FB0), Color(0xFF3A2A20), Color(0xFF2E7D5B))

private val SKIN = Color(0xFFFFDDC1)
private val SKIN_SHADE = Color(0xFFF2C0A0)
private val MOUTH = Color(0xFF8B2E3C)

/**
 * 2D vector character. Idle breathing, blinking, bolte samay mooh chalna,
 * aur emotion ke hisaab se chehre ka badlav.
 */
@Composable
fun NiruCharacter(
    character: String,
    emotion: Emotion,
    talking: Boolean,
    modifier: Modifier = Modifier
) {
    val isNiya = character == Prefs.CHAR_NIYA
    val pal = if (isNiya) NIYA_PAL else ROCKY_PAL
    val f = faceFor(emotion)

    val curve by animateFloatAsState(f.curve, tween(400), label = "curve")
    val eyeOpen by animateFloatAsState(f.eyeOpen, tween(400), label = "eye")
    val brow by animateFloatAsState(f.brow, tween(400), label = "brow")
    val mouthO by animateFloatAsState(f.mouthO, tween(300), label = "mouthO")
    val gazeX by animateFloatAsState(f.gazeX, tween(500), label = "gx")
    val gazeY by animateFloatAsState(f.gazeY, tween(500), label = "gy")
    val blush by animateFloatAsState(f.blush, tween(400), label = "blush")
    val tears by animateFloatAsState(f.tears, tween(400), label = "tears")
    val tilt by animateFloatAsState(f.tilt, tween(500), label = "tilt")

    val inf = rememberInfiniteTransition(label = "niru")
    val breath by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val talkAmp by inf.animateFloat(
        0.1f, 1f,
        infiniteRepeatable(tween(140), RepeatMode.Reverse),
        label = "talk"
    )

    // Random gap par palak jhapakna
    val blink = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 4800))
            blink.animateTo(0.08f, tween(70))
            blink.animateTo(1f, tween(110))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val s = min(size.width, size.height) / 400f
        val cx = size.width / 2f
        val bob = sin(breath * 2 * PI.toFloat()) * 3f * s
        val hc = size.height * 0.40f + bob
        val eyeY = hc + 6f * s
        val eyeH = 40f * s * eyeOpen * blink.value
        val my = hc + 58f * s
        val talkOpen = if (talking) talkAmp else 0f
        val openAmt = maxOf(talkOpen, mouthO)

        rotate(tilt, pivot = Offset(cx, hc + 80f * s)) {
            // peeche ke baal (Niya ke lambe baal)
            if (isNiya) {
                drawRoundRect(
                    pal.hair, Offset(cx - 100f * s, hc - 100f * s),
                    Size(200f * s, 310f * s), CornerRadius(90f * s)
                )
            }

            // kandhe / kurta
            drawRoundRect(
                pal.outfit, Offset(cx - 95f * s, hc + 105f * s),
                Size(190f * s, 300f * s), CornerRadius(60f * s)
            )
            // gardan
            drawRoundRect(
                SKIN_SHADE, Offset(cx - 22f * s, hc + 60f * s),
                Size(44f * s, 60f * s), CornerRadius(16f * s)
            )

            // kaan
            drawCircle(SKIN, 14f * s, Offset(cx - 85f * s, hc + 12f * s))
            drawCircle(SKIN, 14f * s, Offset(cx + 85f * s, hc + 12f * s))

            // chehra
            drawOval(SKIN, Offset(cx - 85f * s, hc - 95f * s), Size(170f * s, 190f * s))

            // gaal ki laali
            if (blush > 0.01f) {
                drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.55f * blush), 14f * s, Offset(cx - 58f * s, hc + 34f * s))
                drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.55f * blush), 14f * s, Offset(cx + 58f * s, hc + 34f * s))
            }

            // saamne ke baal
            drawPath(if (isNiya) niyaBangs(cx, hc, s) else rockyHair(cx, hc, s), pal.hair)
            if (isNiya) {
                // flute-theme: chhota sunehra music note clip
                drawCircle(Color(0xFFFFC857), 6f * s, Offset(cx + 70f * s, hc - 70f * s))
            }

            // aankhen
            for (side in listOf(-1f, 1f)) {
                val ex = cx + side * 38f * s
                drawOval(pal.iris, Offset(ex - 17f * s, eyeY - eyeH / 2), Size(34f * s, eyeH))
                drawCircle(
                    Color(0xFF1A1A2E), 8f * s,
                    Offset(ex + gazeX * 6f * s, eyeY + gazeY * 5f * s + eyeH * 0.05f)
                )
                drawCircle(
                    Color.White, 4f * s,
                    Offset(ex - 6f * s + gazeX * 6f * s, eyeY - eyeH * 0.2f + gazeY * 5f * s)
                )
            }

            // bhaunhen
            val browY = eyeY - 32f * s
            for (side in listOf(-1f, 1f)) {
                val outerX = cx + side * 56f * s
                val innerX = cx + side * 18f * s
                drawLine(
                    color = pal.hair,
                    start = Offset(outerX, browY - 2f * s),
                    end = Offset(innerX, browY + brow * 10f * s),
                    strokeWidth = 5f * s,
                    cap = StrokeCap.Round
                )
            }

            // naak (chhota sa)
            drawCircle(SKIN_SHADE, 3f * s, Offset(cx, hc + 30f * s))

            // aansu
            if (tears > 0.01f) {
                for (side in listOf(-1f, 1f)) {
                    drawCircle(
                        Color(0xFF7EC8FF).copy(alpha = tears), 5f * s,
                        Offset(cx + side * 38f * s, eyeY + 32f * s)
                    )
                }
            }

            // munh
            if (openAmt > 0.12f) {
                val mh = (4f + 16f * openAmt) * s
                val mw = 26f * s - 10f * s * mouthO
                drawOval(MOUTH, Offset(cx - mw / 2, my - mh / 2), Size(mw, mh))
            } else {
                val p = Path().apply {
                    moveTo(cx - 24f * s, my)
                    quadraticBezierTo(cx, my + curve * 20f * s, cx + 24f * s, my)
                }
                drawPath(p, MOUTH, style = Stroke(width = 4f * s, cap = StrokeCap.Round))
            }

            // soch ke waqt upar dots
            if (emotion == Emotion.THINKING) {
                for (i in 0..2) {
                    drawCircle(
                        Color(0xFF9E8CC9).copy(alpha = 0.3f + 0.7f * breath),
                        6f * s, Offset(cx + 90f * s + i * 16f * s, hc - 120f * s)
                    )
                }
            }
        }
    }
}

private fun niyaBangs(cx: Float, hc: Float, s: Float): Path = Path().apply {
    moveTo(cx - 90f * s, hc + 5f * s)
    quadraticBezierTo(cx - 80f * s, hc - 110f * s, cx, hc - 102f * s)
    quadraticBezierTo(cx + 85f * s, hc - 110f * s, cx + 90f * s, hc + 5f * s)
    quadraticBezierTo(cx + 60f * s, hc - 60f * s, cx + 10f * s, hc - 50f * s)
    quadraticBezierTo(cx - 45f * s, hc - 62f * s, cx - 90f * s, hc + 5f * s)
    close()
}

private fun rockyHair(cx: Float, hc: Float, s: Float): Path = Path().apply {
    moveTo(cx - 90f * s, hc - 5f * s)
    quadraticBezierTo(cx - 100f * s, hc - 115f * s, cx - 20f * s, hc - 108f * s)
    quadraticBezierTo(cx, hc - 135f * s, cx + 30f * s, hc - 110f * s)
    quadraticBezierTo(cx + 100f * s, hc - 115f * s, cx + 90f * s, hc - 5f * s)
    quadraticBezierTo(cx + 60f * s, hc - 70f * s, cx + 20f * s, hc - 62f * s)
    quadraticBezierTo(cx - 60f * s, hc - 70f * s, cx - 90f * s, hc - 5f * s)
    close()
}
