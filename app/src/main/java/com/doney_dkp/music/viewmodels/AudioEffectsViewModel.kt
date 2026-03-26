package com.doney_dkp.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doney_dkp.music.constants.BassBoostEnabledKey
import com.doney_dkp.music.constants.BassBoostStrengthKey
import com.doney_dkp.music.constants.EqualizerBandGainsKey
import com.doney_dkp.music.constants.EqualizerEnabledKey
import com.doney_dkp.music.constants.EqualizerPreset
import com.doney_dkp.music.constants.EqualizerPresetKey
import com.doney_dkp.music.playback.AudioEffectsManager
import com.doney_dkp.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioEffectsState(
    val isEqualizerSupported: Boolean = false,
    val isBassBoostSupported: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val numberOfBands: Int = 0,
    val bandLevelRangeMin: Short = -1500,
    val bandLevelRangeMax: Short = 1500,
    val centerFrequenciesHz: List<Int> = emptyList(),
    val bandGainsMb: List<Short> = emptyList(),
    val currentPreset: EqualizerPreset = EqualizerPreset.NORMAL,
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0,
)

@HiltViewModel
class AudioEffectsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val audioEffectsManager: AudioEffectsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    // 5-band gain templates in millibels; interpolated to actual device band count at runtime.
    private val presetGains: Map<EqualizerPreset, List<Short>> = mapOf(
        EqualizerPreset.NORMAL       to listOf( 0,    0,    0,    0,    0),
        EqualizerPreset.BASS_BOOST   to listOf( 600,  400,  0,    0,    0),
        EqualizerPreset.TREBLE_BOOST to listOf( 0,    0,    0,    400,  600),
        EqualizerPreset.VOCAL        to listOf(-200,  0,    400,  200, -200),
        EqualizerPreset.ROCK         to listOf( 400,  200, -200,  200,  400),
    ).mapValues { (_, v) -> v.map { it.toShort() } }

    init {
        loadFromDataStore()
        // Re-load whenever MusicService attaches to a new audio session
        viewModelScope.launch {
            audioEffectsManager.sessionAttached
                .filter { it }
                .collect { loadFromDataStore() }
        }
    }

    private fun loadFromDataStore() {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()

            val preset = runCatching {
                EqualizerPreset.valueOf(prefs[EqualizerPresetKey] ?: "")
            }.getOrDefault(EqualizerPreset.NORMAL)

            val rawGains = prefs[EqualizerBandGainsKey] ?: ""
            val gains = if (rawGains.isNotEmpty())
                rawGains.split(",").mapNotNull { it.trim().toShortOrNull() }
            else
                List(audioEffectsManager.numberOfBands) { 0.toShort() }

            _state.value = AudioEffectsState(
                isEqualizerSupported = audioEffectsManager.isEqualizerSupported,
                isBassBoostSupported = audioEffectsManager.isBassBoostSupported,
                equalizerEnabled     = prefs[EqualizerEnabledKey] ?: false,
                numberOfBands        = audioEffectsManager.numberOfBands,
                bandLevelRangeMin    = audioEffectsManager.bandLevelRange.first,
                bandLevelRangeMax    = audioEffectsManager.bandLevelRange.second,
                centerFrequenciesHz  = audioEffectsManager.centerFrequencies,
                bandGainsMb          = padOrTrim(gains, audioEffectsManager.numberOfBands),
                currentPreset        = preset,
                bassBoostEnabled     = prefs[BassBoostEnabledKey] ?: false,
                bassBoostStrength    = (prefs[BassBoostStrengthKey] ?: 0).toShort(),
            )
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        audioEffectsManager.setEqualizerEnabled(enabled)
        _state.value = _state.value.copy(equalizerEnabled = enabled)
        viewModelScope.launch {
            context.dataStore.edit { it[EqualizerEnabledKey] = enabled }
        }
    }

    fun setBandGain(band: Int, gainMb: Short) {
        audioEffectsManager.setBandLevel(band, gainMb)
        val newGains = _state.value.bandGainsMb.toMutableList().also {
            if (band < it.size) it[band] = gainMb
        }
        _state.value = _state.value.copy(
            bandGainsMb = newGains,
            currentPreset = EqualizerPreset.CUSTOM,
        )
        viewModelScope.launch {
            context.dataStore.edit {
                it[EqualizerBandGainsKey] = newGains.joinToString(",")
                it[EqualizerPresetKey] = EqualizerPreset.CUSTOM.name
            }
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) return
        val template = presetGains[preset] ?: return
        val n = audioEffectsManager.numberOfBands
        val mapped = (0 until n).map { b ->
            val idx = if (n > 1) ((b.toFloat() / (n - 1)) * 4).toInt().coerceIn(0, 4) else 0
            template.getOrElse(idx) { 0.toShort() }
        }
        mapped.forEachIndexed { i, g -> audioEffectsManager.setBandLevel(i, g) }
        _state.value = _state.value.copy(bandGainsMb = mapped, currentPreset = preset)
        viewModelScope.launch {
            context.dataStore.edit {
                it[EqualizerBandGainsKey] = mapped.joinToString(",")
                it[EqualizerPresetKey] = preset.name
            }
        }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        audioEffectsManager.setBassBoostEnabled(enabled)
        _state.value = _state.value.copy(bassBoostEnabled = enabled)
        viewModelScope.launch {
            context.dataStore.edit { it[BassBoostEnabledKey] = enabled }
        }
    }

    fun setBassBoostStrength(strength: Short) {
        audioEffectsManager.setBassBoostStrength(strength)
        _state.value = _state.value.copy(bassBoostStrength = strength)
        viewModelScope.launch {
            context.dataStore.edit { it[BassBoostStrengthKey] = strength.toInt() }
        }
    }

    private fun padOrTrim(list: List<Short>, size: Int): List<Short> =
        list.take(size).toMutableList().also {
            while (it.size < size) it.add(0.toShort())
        }
}
