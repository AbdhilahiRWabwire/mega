package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SupportTicket
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Create support ticket
 *
 */
class CreateSupportTicketUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
) {
    suspend operator fun invoke(
        description: String,
        logFileName: String?,
    ): SupportTicket {
        val (appVersion, sdkVersion) = environmentRepository.getAppInfo()
        val (device, languageCode) = environmentRepository.getDeviceInfo()
        val accountDetails = getAccountDetailsUseCase(false)

        return SupportTicket(
            androidAppVersion = appVersion,
            sdkVersion = sdkVersion,
            device = device,
            currentLanguage = languageCode,
            accountEmail = accountDetails.email,
            accountType = accountDetails.accountTypeString,
            description = description,
            logFileName = logFileName,
            deviceSdkVersionInt = environmentRepository.getDeviceSdkVersionInt(),
            deviceSdkVersionName = environmentRepository.getDeviceSdkVersionName()
        )
    }
}