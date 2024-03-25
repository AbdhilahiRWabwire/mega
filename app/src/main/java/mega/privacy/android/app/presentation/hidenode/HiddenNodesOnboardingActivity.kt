package mega.privacy.android.app.presentation.hidenode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import javax.inject.Inject

@AndroidEntryPoint
class HiddenNodesOnboardingActivity : AppCompatActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val isOnboarding: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        intent.getBooleanExtra(IS_ONBOARDING, false)
    }

    private val viewModel: HiddenNodesOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isOnboarding) viewModel.setHiddenNodesOnboarded()

        setContent {
            val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                HiddenNodesOnboardingScreen(
                    isOnboarding = isOnboarding,
                    onClickBack = ::finish,
                    onClickContinue = ::handleContinue,
                )
            }
        }
    }

    private fun handleContinue() {
        if (isOnboarding) {
            setResult(RESULT_OK)
        } else {
            val intent = Intent(this, UpgradeAccountActivity::class.java)
            startActivity(intent)
        }

        finish()
    }

    companion object {
        private const val IS_ONBOARDING: String = "is_onboarding"

        fun createScreen(
            context: Context,
            isOnboarding: Boolean,
        ) = Intent(context, HiddenNodesOnboardingActivity::class.java).apply {
            putExtra(IS_ONBOARDING, isOnboarding)
        }
    }
}
