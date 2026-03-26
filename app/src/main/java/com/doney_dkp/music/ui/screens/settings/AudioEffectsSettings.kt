package com.doney_dkp.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.doney_dkp.music.LocalPlayerAwareWindowInsets
import com.doney_dkp.music.R
import com.doney_dkp.music.constants.EqualizerPreset
import com.doney_dkp.music.ui.component.IconButton
import com.doney_dkp.music.ui.component.ListPreference
import com.doney_dkp.music.ui.component.PreferenceEntry
import com.doney_dkp.music.ui.component.PreferenceGroupTitle
import com.doney_dkp.music.ui.component.SwitchPreference
import com.doney_dkp.music.ui.utils.backToMain
import com.doney_dkp.music.viewmodels.AudioEffectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AudioEffectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        // ── Equalizer section ─────────────────────────────────────────────────
        PreferenceGroupTitle(title = stringResource(R.string.equalizer))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_equalizer)) },
            icon = { Icon(painterResource(R.drawable.equalizer), contentDescription = null) },
            checked = state.equalizerEnabled,
            onCheckedChange = viewModel::setEqualizerEnabled,
            isEnabled = state.isEqualizerSupported,
        )

        if (!state.isEqualizerSupported) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.equalizer)) },
                description = stringResource(R.string.equalizer_not_supported),
            )
        } else {
            // Preset selector — CUSTOM excluded from selectable list; it shows as description only
            ListPreference(
                title = { Text(stringResource(R.string.eq_preset)) },
                icon = { Icon(painterResource(R.drawable.tune), contentDescription = null) },
                selectedValue = state.currentPreset,
                values = EqualizerPreset.entries.filter { it != EqualizerPreset.CUSTOM },
                valueText = { preset ->
                    when (preset) {
                        EqualizerPreset.NORMAL       -> stringResource(R.string.eq_preset_normal)
                        EqualizerPreset.BASS_BOOST   -> stringResource(R.string.eq_preset_bass_boost)
                        EqualizerPreset.TREBLE_BOOST -> stringResource(R.string.eq_preset_treble_boost)
                        EqualizerPreset.VOCAL        -> stringResource(R.string.eq_preset_vocal)
                        EqualizerPreset.ROCK         -> stringResource(R.string.eq_preset_rock)
                        EqualizerPreset.CUSTOM       -> stringResource(R.string.eq_preset_custom)
                    }
                },
                onValueSelected = viewModel::applyPreset,
                isEnabled = state.equalizerEnabled,
            )

            // Per-band sliders
            state.bandGainsMb.forEachIndexed { band, gainMb ->
                val freqLabel = state.centerFrequenciesHz.getOrNull(band)?.let { hz ->
                    if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
                } ?: "Band ${band + 1}"
                val dbLabel = "%.1f dB".format(gainMb / 100f)

                PreferenceEntry(
                    title = { Text(freqLabel) },
                    description = dbLabel,
                    isEnabled = state.equalizerEnabled,
                    content = {
                        Slider(
                            value = gainMb.toFloat(),
                            valueRange = state.bandLevelRangeMin.toFloat()..state.bandLevelRangeMax.toFloat(),
                            onValueChange = { viewModel.setBandGain(band, it.toInt().toShort()) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
        }

        // ── Bass Boost section ────────────────────────────────────────────────
        PreferenceGroupTitle(title = stringResource(R.string.bass_boost))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_bass_boost)) },
            icon = { Icon(painterResource(R.drawable.volume_up), contentDescription = null) },
            checked = state.bassBoostEnabled,
            onCheckedChange = viewModel::setBassBoostEnabled,
            isEnabled = state.isBassBoostSupported,
        )

        if (state.isBassBoostSupported && state.bassBoostEnabled) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.bass_boost_strength)) },
                description = "${state.bassBoostStrength / 10}%",
                content = {
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        valueRange = 0f..1000f,
                        onValueChange = { viewModel.setBassBoostStrength(it.toInt().toShort()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.audio_effects)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
