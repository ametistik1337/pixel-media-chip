package com.pixel.mediachip.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixel.mediachip.R
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.components.ChipPreset
import com.pixel.mediachip.ui.components.ExpressivePillSwitch
import com.pixel.mediachip.ui.components.ExpressiveSegmentedButton
import com.pixel.mediachip.ui.components.ExpressiveStepSlider
import com.pixel.mediachip.ui.components.PresetRow
import com.pixel.mediachip.ui.components.SettingsCard
import com.pixel.mediachip.ui.preview.LivePreviewCard
import com.pixel.mediachip.ui.viewmodel.SettingsUiEvent
import com.pixel.mediachip.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

/**
 * Main Settings Screen for PixelMediaChip.
 *
 * Implements Google Material Design 3 Expressive guidelines:
 * - Dynamic Monet color scheme tokens
 * - Pinned compact LivePreviewCard at top with status bar canvas simulation
 * - Structured group cards in scrollable LazyColumn:
 *   0. Fast Presets (scrolls away with content)
 *   1. Layout & Modes
 *   2. Dimensions & Typography
 *   3. Camera Cutout & Squish
 *   4. Audio Visualizer
 *   5. System Actions & Diagnostics
 * - Strict anti-slop: zero emojis, zero hardcoded hex colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var selectedPreset by remember { androidx.compose.runtime.mutableStateOf<ChipPreset?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (settingsState.hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            viewModel.refreshDiagnostics()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.section_diagnostics),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pinned Compact Interactive Status Bar Live Preview (Sticky Header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LivePreviewCard(
                    settings = settingsState.toPreviewSettings(),
                    showPresets = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Scrollable Settings Sections
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 0: Fast Presets (scrolls with content, NOT pinned!)
                item {
                    SettingsCard(
                        title = "Быстрые пресеты",
                        subtitle = "Готовые стили оформления для чипса"
                    ) {
                        PresetRow(
                            selectedPreset = selectedPreset,
                            onPresetSelected = { preset ->
                                selectedPreset = preset
                                viewModel.applyPreset(preset)
                            },
                            hapticEnabled = settingsState.hapticEnabled
                        )
                    }
                }

                // Card 1: Layout & Modes
                item {
                SettingsCard(
                    title = stringResource(R.string.section_layout_modes),
                    subtitle = stringResource(R.string.pref_swipe_gestures_desc)
                ) {
                    Text(
                        text = stringResource(R.string.section_display_mode),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    ExpressiveSegmentedButton(
                        selectedMode = settingsState.layoutMode,
                        onModeSelected = { mode ->
                            viewModel.updateAndCommit { it.copy(layoutMode = mode) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.section_squish),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    SquishModeSegmentedButton(
                        selectedMode = settingsState.squishMode,
                        onModeSelected = { mode ->
                            viewModel.updateAndCommit { it.copy(squishMode = mode) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ExpressivePillSwitch(
                        checked = settingsState.showArtist,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(showArtist = checked) }
                        },
                        label = stringResource(R.string.pref_show_artist),
                        description = stringResource(R.string.pref_show_artist_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressivePillSwitch(
                        checked = settingsState.marqueeOneLine,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(marqueeOneLine = checked) }
                        },
                        label = stringResource(R.string.pref_marquee),
                        description = stringResource(R.string.pref_marquee_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressivePillSwitch(
                        checked = settingsState.monetTint,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(monetTint = checked) }
                        },
                        label = stringResource(R.string.pref_monet_tint),
                        description = stringResource(R.string.pref_monet_tint_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressivePillSwitch(
                        checked = settingsState.swipeGestures,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(swipeGestures = checked) }
                        },
                        label = stringResource(R.string.pref_swipe_gestures),
                        description = stringResource(R.string.pref_swipe_gestures_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressivePillSwitch(
                        checked = settingsState.hapticEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(hapticEnabled = checked) }
                        },
                        label = stringResource(R.string.pref_haptic),
                        description = stringResource(R.string.pref_haptic_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )
                }
            }

            // Card 2: Dimensions & Typography
            item {
                SettingsCard(
                    title = stringResource(R.string.section_dimensions),
                    subtitle = stringResource(R.string.section_dimensions_subtitle)
                ) {
                    ExpressiveStepSlider(
                        value = settingsState.maxWidthDp.toFloat(),
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(maxWidthDp = raw.roundToInt()) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 60f..220f,
                        step = 5f,
                        label = stringResource(R.string.slider_max_width),
                        unit = "dp",
                        defaultValue = ChipSettings.DEFAULT_MAX_WIDTH_DP.toFloat(),
                        onReset = {
                            viewModel.updateAndCommit { it.copy(maxWidthDp = ChipSettings.DEFAULT_MAX_WIDTH_DP) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressiveStepSlider(
                        value = settingsState.maxTextWidthDp.toFloat(),
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(maxTextWidthDp = raw.roundToInt()) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 60f..220f,
                        step = 5f,
                        label = stringResource(R.string.slider_text_width),
                        unit = "dp",
                        defaultValue = ChipSettings.DEFAULT_MAX_TEXT_WIDTH_DP.toFloat(),
                        onReset = {
                            viewModel.updateAndCommit { it.copy(maxTextWidthDp = ChipSettings.DEFAULT_MAX_TEXT_WIDTH_DP) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressiveStepSlider(
                        value = settingsState.cornerRadiusDp.toFloat(),
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(cornerRadiusDp = raw.roundToInt()) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 4f..24f,
                        step = 1f,
                        label = stringResource(R.string.slider_corner_radius),
                        unit = "dp",
                        defaultValue = ChipSettings.DEFAULT_CORNER_RADIUS_DP.toFloat(),
                        onReset = {
                            viewModel.updateAndCommit { it.copy(cornerRadiusDp = ChipSettings.DEFAULT_CORNER_RADIUS_DP) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressiveStepSlider(
                        value = settingsState.titleSizeSp,
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(titleSizeSp = raw) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 8.0f..14.0f,
                        step = 0.5f,
                        label = stringResource(R.string.slider_title_size),
                        unit = "sp",
                        defaultValue = ChipSettings.DEFAULT_TITLE_SIZE_SP,
                        onReset = {
                            viewModel.updateAndCommit { it.copy(titleSizeSp = ChipSettings.DEFAULT_TITLE_SIZE_SP) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressiveStepSlider(
                        value = settingsState.artistSizeSp,
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(artistSizeSp = raw) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 6.0f..12.0f,
                        step = 0.5f,
                        label = stringResource(R.string.slider_artist_size),
                        unit = "sp",
                        defaultValue = ChipSettings.DEFAULT_ARTIST_SIZE_SP,
                        onReset = {
                            viewModel.updateAndCommit { it.copy(artistSizeSp = ChipSettings.DEFAULT_ARTIST_SIZE_SP) }
                        },
                        enabled = settingsState.showArtist,
                        hapticEnabled = settingsState.hapticEnabled
                    )
                }
            }

            // Card 3: Camera Cutout & Squish
            item {
                SettingsCard(
                    title = stringResource(R.string.pref_eat_camera),
                    subtitle = stringResource(R.string.pref_eat_camera_desc)
                ) {
                    ExpressivePillSwitch(
                        checked = settingsState.eatCamera,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(eatCamera = checked) }
                        },
                        label = stringResource(R.string.pref_eat_camera),
                        description = stringResource(R.string.pref_eat_camera_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressiveStepSlider(
                        value = settingsState.cameraEatDepthDp.toFloat(),
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(cameraEatDepthDp = raw.roundToInt()) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 8f..32f,
                        step = 2f,
                        label = stringResource(R.string.slider_camera_depth),
                        unit = "dp",
                        defaultValue = ChipSettings.DEFAULT_CAMERA_EAT_DEPTH_DP.toFloat(),
                        onReset = {
                            viewModel.updateAndCommit { it.copy(cameraEatDepthDp = ChipSettings.DEFAULT_CAMERA_EAT_DEPTH_DP) }
                        },
                        enabled = settingsState.eatCamera,
                        hapticEnabled = settingsState.hapticEnabled
                    )
                }
            }

            // Card 4: Audio Visualizer
            item {
                SettingsCard(
                    title = stringResource(R.string.section_visualizer),
                    subtitle = stringResource(R.string.section_visualizer_subtitle)
                ) {
                    ExpressivePillSwitch(
                        checked = settingsState.visualizerChipEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(visualizerChipEnabled = checked) }
                        },
                        label = stringResource(R.string.pref_vis_chip),
                        description = stringResource(R.string.pref_vis_chip_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressivePillSwitch(
                        checked = settingsState.visualizerCardEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateAndCommit { it.copy(visualizerCardEnabled = checked) }
                        },
                        label = stringResource(R.string.pref_vis_card),
                        description = stringResource(R.string.pref_vis_card_desc),
                        hapticEnabled = settingsState.hapticEnabled
                    )

                    ExpressiveStepSlider(
                        value = settingsState.visualizerSensitivity.toFloat(),
                        onValueChange = { raw ->
                            viewModel.updateState { it.copy(visualizerSensitivity = raw.roundToInt()) }
                        },
                        onValueChangeFinished = {
                            viewModel.commitSettings()
                        },
                        valueRange = 50f..200f,
                        step = 5f,
                        label = stringResource(R.string.slider_vis_sensitivity),
                        unit = "%",
                        defaultValue = ChipSettings.DEFAULT_VISUALIZER_SENSITIVITY.toFloat(),
                        onReset = {
                            viewModel.updateAndCommit { it.copy(visualizerSensitivity = ChipSettings.DEFAULT_VISUALIZER_SENSITIVITY) }
                        },
                        hapticEnabled = settingsState.hapticEnabled
                    )
                }
            }

            // Card 5: System Actions & Diagnostics
            item {
                SystemActionsSection(
                    diagnostics = diagnostics,
                    onRestartSystemUi = { viewModel.restartSystemUi() },
                    onResetDefaults = { viewModel.resetToDefaults() },
                    hapticEnabled = settingsState.hapticEnabled
                )
            }

            // Bottom spacer for comfortable scrolling
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}

/**
 * Segmented button specifically for squish mode selection:
 * - To Icon (SQUISH_TO_ICON = 0)
 * - Hide (SQUISH_HIDE = 1)
 * - Disabled (SQUISH_DISABLED = 2)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SquishModeSegmentedButton(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val modes = listOf(
        Triple(ChipSettings.SQUISH_TO_ICON, stringResource(R.string.squish_to_icon), Icons.Rounded.Compress),
        Triple(ChipSettings.SQUISH_HIDE, stringResource(R.string.squish_hide), Icons.Rounded.VisibilityOff),
        Triple(ChipSettings.SQUISH_DISABLED, stringResource(R.string.squish_disabled), Icons.Rounded.MusicNote)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        modes.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = {
                    if (selectedMode != mode) {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onModeSelected(mode)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selectedMode == mode) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
