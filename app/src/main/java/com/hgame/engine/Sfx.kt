package com.hgame.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Sound effects. All sounds are synthesised at runtime, so the app ships no audio assets. */
enum class Sfx { CLICK, TAP, POP, COIN, MERGE, HIT, BOOM, WIN, LOSE, JUMP, SHOOT, CARD, ERROR, TICK, NOTE }

object SoundBank {
    private const val RATE = 22050
    @Volatile private var pool: SoundPool? = null
    private val ids = IntArray(Sfx.values().size)
    var enabled = true

    fun init(context: Context) {
        if (pool != null) return
        val p = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
        val dir = File(context.cacheDir, "sfx-v1").apply { mkdirs() }
        for (s in Sfx.values()) {
            val f = File(dir, s.name.lowercase() + ".wav")
            if (!f.exists() || f.length() < 64) writeWav(f, synth(s))
            ids[s.ordinal] = p.load(f.absolutePath, 1)
        }
        pool = p
    }

    fun play(s: Sfx, volume: Float = 0.6f) {
        if (!enabled) return
        pool?.play(ids[s.ordinal], volume, volume, 1, 0, 1f)
    }

    // Original melody (scale degrees in semitones from C5) used by the music-tap games.
    private val melody = intArrayOf(0, 4, 7, 12, 7, 4, 5, 9, 12, 9, 5, 2, 7, 11, 14, 11, 7, 4, 0, 4, 7, 4, 2, -1, 0)

    /** Plays the [n]-th note of a looping melody using a pitched piano-like sample. */
    fun playNote(n: Int, volume: Float = 0.7f) {
        if (!enabled) return
        val semis = melody[Math.floorMod(n, melody.size)]
        val rate = Math.pow(2.0, semis / 12.0).toFloat().coerceIn(0.5f, 2f)
        pool?.play(ids[Sfx.NOTE.ordinal], volume, volume, 1, 0, rate)
    }

    /** Builds the PCM samples of one effect (-1..1). */
    fun synth(s: Sfx): FloatArray = when (s) {
        Sfx.CLICK -> tone(0.035f, 1400f, 1400f, wave = 0, decay = 60f)
        Sfx.TAP -> tone(0.06f, 660f, 520f, wave = 0, decay = 40f)
        Sfx.POP -> tone(0.09f, 380f, 950f, wave = 0, decay = 25f)
        Sfx.COIN -> concat(tone(0.06f, 988f, 988f, 1, 20f), tone(0.12f, 1319f, 1319f, 1, 18f))
        Sfx.MERGE -> tone(0.13f, 440f, 880f, wave = 2, decay = 18f)
        Sfx.HIT -> noise(0.08f, 45f, 0.5f)
        Sfx.BOOM -> noise(0.45f, 7f, 0.92f)
        Sfx.WIN -> concat(tone(0.1f, 523f, 523f, 2, 10f), tone(0.1f, 659f, 659f, 2, 10f),
            tone(0.1f, 784f, 784f, 2, 10f), tone(0.25f, 1047f, 1047f, 2, 6f))
        Sfx.LOSE -> concat(tone(0.18f, 392f, 370f, 2, 6f), tone(0.18f, 330f, 311f, 2, 6f), tone(0.35f, 262f, 220f, 2, 5f))
        Sfx.JUMP -> tone(0.11f, 300f, 760f, wave = 1, decay = 20f, vol = 0.35f)
        Sfx.SHOOT -> tone(0.12f, 1300f, 280f, wave = 1, decay = 22f, vol = 0.3f)
        Sfx.CARD -> noise(0.05f, 70f, 0.2f)
        Sfx.ERROR -> tone(0.16f, 160f, 140f, wave = 1, decay = 12f, vol = 0.35f)
        Sfx.TICK -> tone(0.02f, 2000f, 2000f, wave = 0, decay = 120f, vol = 0.4f)
        Sfx.NOTE -> piano(0.6f, 523.25f)
    }

    private fun tone(sec: Float, f0: Float, f1: Float, wave: Int, decay: Float, vol: Float = 0.6f): FloatArray {
        val n = (sec * RATE).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val f = f0 + (f1 - f0) * t
            phase += 2 * PI * f / RATE
            val v = when (wave) {
                1 -> if (sin(phase) >= 0) 1.0 else -1.0           // square
                2 -> sin(phase) * 0.7 + sin(phase * 2) * 0.3        // soft organ
                else -> sin(phase)
            }
            val attack = minOf(1f, i / (RATE * 0.004f))
            out[i] = (v * vol * attack * exp(-decay * t * sec * 6.0)).toFloat()
        }
        return out
    }

    private fun noise(sec: Float, decay: Float, smooth: Float): FloatArray {
        val n = (sec * RATE).toInt()
        val out = FloatArray(n)
        val r = Random(7)
        var last = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / RATE
            last = last * smooth + (r.nextFloat() * 2 - 1) * (1 - smooth)
            out[i] = (last * 0.9f * exp(-decay * t) / maxOf(0.25f, 1 - smooth)).coerceIn(-1f, 1f)
        }
        return out
    }

    private fun piano(sec: Float, f: Float): FloatArray {
        val n = (sec * RATE).toInt()
        return FloatArray(n) { i ->
            val t = i.toFloat() / RATE
            val env = exp(-t * 6f) * minOf(1f, t * 400f)
            val v = sin(2 * PI * f * t) + 0.5 * sin(4 * PI * f * t) + 0.25 * sin(6 * PI * f * t)
            (v * env * 0.4).toFloat()
        }
    }

    private fun concat(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.sumOf { it.size })
        var o = 0
        for (p in parts) { p.copyInto(out, o); o += p.size }
        return out
    }

    private fun writeWav(f: File, data: FloatArray) {
        val bytes = data.size * 2
        val b = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(RATE).putInt(RATE * 2)
            .putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(bytes)
        for (v in data) b.putShort((v.coerceIn(-1f, 1f) * 32000).toInt().toShort())
        FileOutputStream(f).use { it.write(b.array()) }
    }
}
