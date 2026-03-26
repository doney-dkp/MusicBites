package com.doney_dkp.music.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectsManager @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    /**
     * Emits true once attachToSession() succeeds.
     * AudioEffectsViewModel observes this to refresh its UI state
     * after the MusicService attaches to the ExoPlayer audio session.
     */
    val sessionAttached = MutableStateFlow(false)

    var isEqualizerSupported = false
        private set
    var isBassBoostSupported = false
        private set
    var numberOfBands = 0
        private set
    var bandLevelRange: Pair<Short, Short> = (-1500).toShort() to 1500.toShort()
        private set
    var centerFrequencies: List<Int> = emptyList() // Hz
        private set

    /**
     * Attach Equalizer and BassBoost to the given ExoPlayer audio session.
     * Called once from MusicService.onCreate() after the player is built.
     */
    fun attachToSession(audioSessionId: Int) {
        release()

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            isEqualizerSupported = true
            numberOfBands = eq.numberOfBands.toInt()
            bandLevelRange = eq.bandLevelRange[0] to eq.bandLevelRange[1]
            centerFrequencies = (0 until numberOfBands).map { band ->
                eq.getCenterFreq(band.toShort()) / 1000 // milliHz → Hz
            }
            Log.d(TAG, "Equalizer attached: $numberOfBands bands, range ${bandLevelRange.first}..${bandLevelRange.second} mB")
        } catch (e: Exception) {
            isEqualizerSupported = false
            Log.w(TAG, "Equalizer not supported on this device", e)
        }

        try {
            bassBoost = BassBoost(0, audioSessionId)
            isBassBoostSupported = true
            Log.d(TAG, "BassBoost attached")
        } catch (e: Exception) {
            isBassBoostSupported = false
            Log.w(TAG, "BassBoost not supported on this device", e)
        }

        sessionAttached.value = true
    }

    /**
     * Apply all persisted settings at once.
     * Called from MusicService.onCreate() right after attachToSession().
     */
    fun applySettings(
        equalizerEnabled: Boolean,
        bandGainsMb: List<Short>,
        bassBoostEnabled: Boolean,
        bassBoostStrength: Short,
    ) {
        equalizer?.let { eq ->
            runCatching { eq.enabled = equalizerEnabled }
            bandGainsMb.forEachIndexed { i, gain ->
                if (i < numberOfBands) {
                    runCatching { eq.setBandLevel(i.toShort(), gain) }
                }
            }
        }
        bassBoost?.let { bb ->
            runCatching { bb.enabled = bassBoostEnabled }
            runCatching { bb.setStrength(bassBoostStrength) }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
    }

    fun setBandLevel(band: Int, levelMb: Short) {
        runCatching { equalizer?.setBandLevel(band.toShort(), levelMb) }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        runCatching { bassBoost?.enabled = enabled }
    }

    fun setBassBoostStrength(strength: Short) {
        runCatching { bassBoost?.setStrength(strength) }
    }

    /**
     * Release all audio effects. Called from MusicService.onDestroy().
     */
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { bassBoost?.release() }
        bassBoost = null
        sessionAttached.value = false
    }

    companion object {
        private const val TAG = "AudioEffectsManager"
    }
}
