package com.kerrysgame.audio

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator

/**
 * Plays game SFX.
 *
 * If raw assets exist (axe_swing, wood_crack, wood_collect), SoundPool is used.
 * Otherwise, lightweight tone placeholders are played so the game still has feedback.
 */
class SoundEffectManager(context: Context) {
    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 75)
    }.getOrNull()
    private val soundPool = SoundPool.Builder().setMaxStreams(4).build()

    private val swingSoundId = loadIfExists(context, "axe_swing")
    private val crackSoundId = loadIfExists(context, "wood_crack")
    private val collectSoundId = loadIfExists(context, "wood_collect")

    private fun loadIfExists(context: Context, fileName: String): Int {
        val resId = context.resources.getIdentifier(fileName, "raw", context.packageName)
        return if (resId != 0) soundPool.load(context, resId, 1) else 0
    }

    fun playSwing() {
        if (swingSoundId != 0) {
            soundPool.play(swingSoundId, 1f, 1f, 1, 0, 1f)
        } else {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        }
    }

    fun playCrack() {
        if (crackSoundId != 0) {
            soundPool.play(crackSoundId, 1f, 1f, 1, 0, 1f)
        } else {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
        }
    }

    fun playCollect() {
        if (collectSoundId != 0) {
            soundPool.play(collectSoundId, 1f, 1f, 1, 0, 1f)
        } else {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 45)
        }
    }

    fun release() {
        soundPool.release()
        toneGenerator?.release()
    }
}