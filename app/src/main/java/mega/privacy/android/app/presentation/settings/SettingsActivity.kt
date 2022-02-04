package mega.privacy.android.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.NAVIGATE_TO_INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.model.TargetPreference

private const val TITLE_TAG = "settingsActivityTitle"

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment().apply {
                    arguments = intent.extras
                })
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.action_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()

        if(caller is FragmentResultListener){
            supportFragmentManager.setFragmentResultListener(pref.key, this, caller)
//            In the calling fragment, implement FragmentResultListener to handle results for a specific preference key
//            In the Called Fragment, setFragmentResult using the preference key to pass back any results
        }

        title = pref.title
        return true
    }

    companion object{
        fun getIntent(context: Context, targetPreference: TargetPreference? = null): Intent {
            return Intent(context, SettingsActivity::class.java).apply {
                putExtra(INITIAL_PREFERENCE, targetPreference?.preferenceId)
                putExtra(NAVIGATE_TO_INITIAL_PREFERENCE, targetPreference?.requiresNavigation)
            }
        }

    }

}