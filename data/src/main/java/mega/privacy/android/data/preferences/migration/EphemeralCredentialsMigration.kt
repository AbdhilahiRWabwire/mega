package mega.privacy.android.data.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import dagger.Lazy
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.mapper.login.EphemeralCredentialsPreferenceMapper
import javax.inject.Inject

internal class EphemeralCredentialsMigration @Inject constructor(
    private val databaseHandler: Lazy<DatabaseHandler>,
    private val ephemeralCredentialsPreferenceMapper: EphemeralCredentialsPreferenceMapper,
) : DataMigration<Preferences> {

    override suspend fun cleanUp() {
        databaseHandler.get().clearEphemeral()
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        databaseHandler.get().ephemeral != null

    override suspend fun migrate(currentData: Preferences): Preferences {
        // it only run if shouldMigrate as true so ephemeral not null
        val ephemeral = databaseHandler.get().ephemeral
        checkNotNull(ephemeral)
        return currentData.toMutablePreferences().apply {
            ephemeralCredentialsPreferenceMapper(this, ephemeral)
        }
    }
}