package com.pixel.mediachip.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixel.mediachip.R
import com.pixel.mediachip.ui.components.SettingsCard
import com.pixel.mediachip.ui.theme.PillShape
import com.pixel.mediachip.ui.theme.SettingsCardShape
import com.pixel.mediachip.ui.theme.tactilePress
import com.pixel.mediachip.ui.viewmodel.SystemDiagnostics

/**
 * System actions and diagnostics section for PixelMediaChip.
 *
 * Displays:
 * - Real-time diagnostics for Root access, RECORD_AUDIO, and SYSTEM_ALERT_WINDOW.
 * - Root Restart SystemUI action with confirmation dialog.
 * - Reset to defaults action with confirmation dialog.
 *
 * Strict anti-slop: zero emojis, zero hardcoded colors.
 */
@Composable
fun SystemActionsSection(
    diagnostics: SystemDiagnostics,
    onRestartSystemUi: () -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var showRestartDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    SettingsCard(
        title = stringResource(R.string.section_diagnostics),
        subtitle = stringResource(R.string.section_diagnostics_subtitle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Diagnostics Status Rows
            DiagnosticStatusRow(
                title = stringResource(R.string.diag_root),
                subtitle = stringResource(R.string.diag_root_desc),
                icon = Icons.Rounded.Security,
                isGranted = diagnostics.isRootAvailable,
                grantedLabel = stringResource(R.string.status_available),
                deniedLabel = stringResource(R.string.status_not_detected)
            )

            DiagnosticStatusRow(
                title = stringResource(R.string.diag_record_audio),
                subtitle = stringResource(R.string.diag_record_audio_desc),
                icon = Icons.Rounded.Mic,
                isGranted = diagnostics.isRecordAudioGranted,
                grantedLabel = stringResource(R.string.status_granted),
                deniedLabel = stringResource(R.string.status_denied)
            )

            DiagnosticStatusRow(
                title = stringResource(R.string.diag_overlay),
                subtitle = stringResource(R.string.diag_overlay_desc),
                icon = Icons.Rounded.Layers,
                isGranted = diagnostics.isOverlayGranted,
                grantedLabel = stringResource(R.string.status_granted),
                deniedLabel = stringResource(R.string.status_denied)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Restart SystemUI Button
                FilledTonalButton(
                    onClick = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        showRestartDialog = true
                    },
                    shape = PillShape,
                    modifier = Modifier
                        .weight(1f)
                        .tactilePress(pressedScale = 0.96f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_restart_systemui),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Reset Defaults Button
                OutlinedButton(
                    onClick = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        showResetDialog = true
                    },
                    shape = PillShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .tactilePress(pressedScale = 0.96f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_reset_settings),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // Confirmation Dialog: Restart SystemUI
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_restart_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_restart_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        onRestartSystemUi()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_restart_confirm),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = SettingsCardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // Confirmation Dialog: Reset to Defaults
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_reset_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_reset_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetDefaults()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_reset_confirm),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = SettingsCardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/**
 * Individual status indicator row with icon, title, subtitle, and tonal pill badge.
 */
@Composable
private fun DiagnosticStatusRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    grantedLabel: String,
    deniedLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = PillShape,
            color = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            border = if (!isGranted) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            } else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.RemoveCircleOutline,
                    contentDescription = null,
                    tint = if (isGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isGranted) grantedLabel else deniedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
