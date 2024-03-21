package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Use Case to check if the cookie dialog should be shown with ads.
 */
class ShouldShowCookieDialogWithAdsUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     *  Check if the cookie dialog should be shown with ads.
     *
     * @param cookieSettings Cookie settings.
     * @param inAppAdvertisementFeature Feature flag to check if in-app ads are enabled.
     * @param isAdsEnabledFeature Feature flag to check if ads are enabled.
     * @param isExternalAdsEnabledFeature Feature flag to check if external ads are enabled.
     * @return True if cookie dialog should be shown with ads, false otherwise.
     */
    suspend operator fun invoke(
        cookieSettings: Set<CookieType>,
        isAdsEnabledFeature: ABTestFeature,
        isExternalAdsEnabledFeature: ABTestFeature,
    ): Boolean = coroutineScope {
        val features = listOf(
            isAdsEnabledFeature,
            isExternalAdsEnabledFeature
        )

        val featureFlags = features.map { feature ->
            async { getFeatureFlagValueUseCase(feature) }
        }

        featureFlags.all { it.await() }
                && !cookieSettings.contains(CookieType.ADS_CHECK)
    }
}