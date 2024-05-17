package mega.privacy.android.app.presentation.settings.reportissue.view

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueUiState
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_087
import mega.privacy.android.legacy.core.ui.controls.controlssliders.LabelledSwitch
import mega.privacy.android.legacy.core.ui.controls.dialogs.ProgressDialog
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
fun ReportIssueView(
    uiState: ReportIssueUiState,
    modifier: Modifier = Modifier,
    onDescriptionChanged: (String) -> Unit = {},
    onIncludeLogsChanged: (Boolean) -> Unit = {},
    cancelUpload: () -> Unit = {},
) {

    ProgressHandler(
        cancelUpload = cancelUpload,
        uploadProgress = uiState.uploadProgress
    )

    ReportIssueBody(
        modifier = modifier,
        uiState = uiState,
        onDescriptionChanged = onDescriptionChanged,
        onIncludeLogsChanged = onIncludeLogsChanged
    )
}

@Composable
private fun ReportIssueBody(
    modifier: Modifier,
    uiState: ReportIssueUiState,
    onDescriptionChanged: (String) -> Unit,
    onIncludeLogsChanged: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        modifier = modifier.padding(bottom = 8.dp),
    ) {
        if (uiState.error != null) {
            ErrorBanner(
                errorMessage = stringResource(id = uiState.error),
            )
        }

        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(R.string.settings_help_report_issue_instructions),
            color = if (!MaterialTheme.colors.isLight) white_alpha_087 else grey_alpha_087
        )
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = if (!MaterialTheme.colors.isLight) white_alpha_012 else grey_alpha_012,
            thickness = 1.dp
        )
        DescriptionTextField(
            description = uiState.description,
            onDescriptionChanged = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .defaultMinSize(minHeight = 150.dp)
        )
        if (uiState.includeLogsVisible) {
            LabelledSwitch(
                label = stringResource(id = R.string.settings_help_report_issue_attach_logs_label),
                checked = uiState.includeLogs,
                onCheckChanged = onIncludeLogsChanged,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressHandler(
    cancelUpload: () -> Unit,
    uploadProgress: Float?,
) {
    if (uploadProgress != null) {
        ProgressDialog(
            title = stringResource(id = R.string.settings_help_report_issue_uploading_log_file),
            progress = uploadProgress,
            onCancel = cancelUpload,
            cancelButtonText = stringResource(id = R.string.general_cancel)
        )
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Preview
@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun PreviewReportIssueView() {
    var checkedState by remember { mutableStateOf(false) }
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        Scaffold {
            ReportIssueView(
                uiState = ReportIssueUiState(
                    description = "",
                    includeLogs = checkedState,
                    canSubmit = true,
                    includeLogsVisible = true,
                    error = R.string.settings_help_report_issue_error,
                ),
                onIncludeLogsChanged = { checkedState = !checkedState },
            )
        }
    }
}
