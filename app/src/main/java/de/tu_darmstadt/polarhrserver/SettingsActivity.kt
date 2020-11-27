package de.tu_darmstadt.polarhrserver

import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {


        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            generateSettings("ACC")
            generateSettings("ECG")
        }

        private fun generateSettings(name: String) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val keyAvailTypes = "${name}_AVAILABLE_TYPES"
            if (prefs.contains(keyAvailTypes)) {
                val types = prefs.getStringSet(keyAvailTypes, null)
                types?.forEach { generateSelectionSetting(name, it) }
            }
        }

        private fun generateSelectionSetting(name: String, type: String) {
            val pm = PreferenceManager.getDefaultSharedPreferences(context)

            val pref = findPreference<ListPreference>("${name}_$type")
            pref?.apply {
                entries =  pm.getStringSet("${name}_AVAILABLE_$type", setOf())?.toTypedArray() ?: arrayOf()
                entryValues = entries
            }
        }
    }
}