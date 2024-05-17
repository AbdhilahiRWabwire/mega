package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.appcompat.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.AndroidTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaTheme

/**
 * Raised default text button
 */
@Composable
fun RaisedDefaultMegaButton(
    textId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = RaisedDefaultMegaButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
) {
    Text(
        text = stringResource(id = textId),
        style = MaterialTheme.typography.button
    )
}

/**
 * Raised default error button
 */
@Composable
fun RaisedDefaultErrorMegaButton(
    textId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = RaisedErrorMegaButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
) {
    Text(
        text = stringResource(id = textId),
        style = MaterialTheme.typography.button
    )
}

/**
 * Raised progress button
 */
@Composable
fun RaisedProgressMegaButton(
    modifier: Modifier = Modifier,
) = RaisedDefaultMegaButton(
    onClick = {},
    modifier = modifier,
) {
    MegaCircularProgressIndicator(modifier = Modifier.size(24.dp), useInverseColor = true)
}

/**
 * Raised default text button
 */
@Composable
private fun RaisedDefaultMegaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    shape = MaterialTheme.shapes.medium,
    colors = MegaTheme.colors.raisedButtonColors,
    border = if (enabled) {
        null
    } else {
        BorderStroke(1.dp, MegaTheme.colors.border.disabled)
    },
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    elevation = ButtonDefaults.elevation(8.dp),
) {
    content()
}

/**
 * Raised error text button
 */
@Composable
private fun RaisedErrorMegaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    shape = MaterialTheme.shapes.medium,
    colors = MegaTheme.colors.raisedErrorButtonColors,
    border = null,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    elevation = ButtonDefaults.elevation(8.dp),
) {
    content()
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewRaisedDefaultMegaButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(true, false).forEach {
                Text(
                    style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface),
                    text = if (it) "Enabled" else "Disabled",
                )
                RaisedDefaultMegaButton(
                    textId = R.string.search_menu_title,
                    onClick = {},
                    enabled = it
                )
            }
        }
    }
}


@CombinedTextAndThemePreviews
@Composable
private fun RaisedErrorMegaButtonPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(true, false).forEach {
                Text(
                    style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface),
                    text = if (it) "Enabled" else "Disabled",
                )
                RaisedDefaultErrorMegaButton(
                    textId = R.string.search_menu_title,
                    onClick = {},
                    enabled = it
                )
            }
        }
    }
}


@CombinedThemePreviews
@Composable
private fun RaisedProgressMegaButtonPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        RaisedProgressMegaButton()
    }
}