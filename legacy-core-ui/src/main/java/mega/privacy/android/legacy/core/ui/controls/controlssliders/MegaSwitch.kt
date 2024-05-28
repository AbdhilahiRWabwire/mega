package mega.privacy.android.legacy.core.ui.controls.controlssliders

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.grey_010
import mega.privacy.android.shared.original.core.ui.theme.grey_100
import mega.privacy.android.shared.original.core.ui.theme.grey_400
import mega.privacy.android.shared.original.core.ui.theme.grey_400_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.teal_100
import mega.privacy.android.shared.original.core.ui.theme.teal_200
import mega.privacy.android.shared.original.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.teal_300

/**
 * Material switch with MEGA colours
 */
@Composable
fun MegaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
    colors = if (MaterialTheme.colors.isLight) lightSwitchColors() else darkSwitchColors()
)


@Composable
private fun lightSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = teal_300,
    checkedTrackColor = teal_100,
    checkedTrackAlpha = 1f, //default is 0.38
    uncheckedThumbColor = grey_010,
    uncheckedTrackColor = grey_400_alpha_038, //alpha will be override, but keep it here for reference
    uncheckedTrackAlpha = 0.38f,
)

@Composable
private fun darkSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = teal_200,
    checkedTrackColor = teal_200_alpha_038,
    checkedTrackAlpha = 0.38f,
    uncheckedThumbColor = grey_100,
    uncheckedTrackColor = grey_400,
    uncheckedTrackAlpha = 1f,
)

@CombinedThemePreviews
@Composable
private fun MegaSwitchPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    var checked by remember { mutableStateOf(initialValue) }
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaSwitch(
            checked = checked,
            onCheckedChange = { checked = !checked })
    }
}
