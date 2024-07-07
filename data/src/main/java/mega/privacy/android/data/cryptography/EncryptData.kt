package mega.privacy.android.data.cryptography

import android.util.Base64
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Class to encrypt given value
 */
@Suppress("RedundantSuspendModifier", "GetInstance")
@Singleton
class EncryptData @Inject constructor(
    @Named("aes_key") private val aesKey: ByteArray,
) {
    /**
     * Invoke
     * @param data string to be encrypted
     * @return encrypted value
     */
    suspend operator fun invoke(data: String?) = data?.let {
        runCatching {
            val skeySpec = SecretKeySpec(aesKey, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
            val encrypted = cipher.doFinal(it.toByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        }.onFailure {
            Timber.e(it, "Error encrypting DB field")
        }.getOrNull()
    }
}