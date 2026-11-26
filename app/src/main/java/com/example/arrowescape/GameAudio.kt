package com.example.arrowescape

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Sound effects (SoundPool) and looping background music (MediaPlayer), with on/off settings. */
class GameAudio(context: Context, private val prefs: SharedPreferences) {

    private val appContext = context.applicationContext

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val escapeId = pool.load(appContext, R.raw.escape, 1)
    private val blockedId = pool.load(appContext, R.raw.blocked, 1)
    private val winId = pool.load(appContext, R.raw.win, 1)

    private val sparkleId = pool.load(appContext, R.raw.sparkle, 1)
    private val comboId = pool.load(appContext, R.raw.combo, 1)

    // Pentatonic steps (SoundPool allows 0.5x..2x), so taps always sound musical.
    private val pitches = floatArrayOf(0.75f, 0.8333f, 1.0f, 1.125f, 1.25f, 1.5f, 1.6667f, 2.0f)
    private var lastPitch = 2

    private var music: MediaPlayer? = null
    private var inForeground = false

    var soundOn by mutableStateOf(prefs.getBoolean("sound_on", true))
        private set
    var musicOn by mutableStateOf(prefs.getBoolean("music_on", true))
        private set

    fun toggleSound() {
        soundOn = !soundOn
        prefs.edit().putBoolean("sound_on", soundOn).apply()
    }

    fun toggleMusic() {
        musicOn = !musicOn
        prefs.edit().putBoolean("music_on", musicOn).apply()
        if (musicOn) startMusic() else music?.pause()
    }

    /**
     * [combo] is how many arrows have escaped in quick succession (1 = a single tap).
     * The note climbs the scale and gets louder as the combo grows; from 3 a sparkle is
     * layered on top, and every 5th in a row adds a big chord bloom.
     */
    fun playEscape(combo: Int = 1) {
        if (!soundOn) return
        if (combo <= 1) {
            // Wander up or down the lower part of the scale.
            val step = listOf(-2, -1, 1, 2).random()
            lastPitch = (lastPitch + step).coerceIn(0, 4)
        } else {
            lastPitch = (combo + 1).coerceAtMost(pitches.lastIndex)
        }
        val vol = (0.6f + 0.06f * combo).coerceAtMost(1f)
        pool.play(escapeId, vol, vol, 1, 0, pitches[lastPitch])
        if (combo >= 3) {
            val sv = (0.25f + 0.08f * (combo - 3)).coerceAtMost(0.8f)
            val rate = (0.9f + 0.05f * (combo - 3)).coerceAtMost(1.5f)
            pool.play(sparkleId, sv, sv, 2, 0, rate)
        }
        if (combo >= 5 && combo % 5 == 0) {
            pool.play(comboId, 1f, 1f, 3, 0, 1f)
        }
    }

    fun playBlocked() {
        if (soundOn) pool.play(blockedId, 0.9f, 0.9f, 1, 0, 1f)
    }

    fun playWin() {
        if (soundOn) pool.play(winId, 0.8f, 0.8f, 1, 0, 1f)
    }

    /** Call from onResume. */
    fun onForeground() {
        inForeground = true
        startMusic()
    }

    /** Call from onPause. */
    fun onBackground() {
        inForeground = false
        music?.pause()
    }

    private fun startMusic() {
        if (!musicOn || !inForeground) return
        val player = music ?: MediaPlayer.create(appContext, R.raw.ambient)?.apply {
            isLooping = true
            setVolume(0.4f, 0.4f)
        }?.also { music = it }
        if (player != null && !player.isPlaying) player.start()
    }

    fun release() {
        music?.release()
        music = null
        pool.release()
    }
}
