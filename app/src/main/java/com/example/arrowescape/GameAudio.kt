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

    // Pentatonic steps, so consecutive taps sound like a little melody.
    private val pitches = floatArrayOf(0.75f, 0.8333f, 1.0f, 1.125f, 1.25f, 1.5f)
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

    fun playEscape() {
        if (!soundOn) return
        // Wander up or down the scale by one or two steps.
        val step = listOf(-2, -1, 1, 2).random()
        lastPitch = (lastPitch + step).coerceIn(0, pitches.lastIndex)
        pool.play(escapeId, 0.7f, 0.7f, 1, 0, pitches[lastPitch])
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
