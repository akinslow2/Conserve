package com.gemini.energy.presentation.audit

import CompanyCamAPI.CompanyCamServiceFactory
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.v7.app.AppCompatActivity
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.users.FullAccount
import com.gemini.energy.App
import com.gemini.energy.R
import kotlinx.android.synthetic.main.activity_settings.*
import timber.log.Timber

class SettingsActivity: AppCompatActivity() {

    private var dropBox: DropBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        dropBox = DropBox()

        val bundle: Bundle = Bundle()
        bundle.putParcelable("dropBox", dropBox)

        val fragment = SettingsFragment()
        fragment.arguments = bundle

        fragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .commit()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("!! Settings Activity - **** - ON RESUME !!")
        DropBox.captureAuthToken()
    }

    fun authorizeCompanyCam() {
        val authIntent = CompanyCamServiceFactory.authorizeIntent()
        if (authIntent?.resolveActivity(packageManager) != null)
            startActivity(authIntent)
    }

}

class SettingsFragment: PreferenceFragment() {

    private var dropBox: DropBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.app_preferences)
        if (arguments != null) {
            dropBox = arguments.getParcelable("dropBox")
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference): Boolean {
        return when(preference.key) {
            getString(R.string.drop_box_auth) -> {

                // 1. Check if there is Auth Token in the shared preference
                // 2. If there is logout from DropBox
                // 3. If there is not start the OAuth process

                if (DropBox.hasToken()) {
                    Timber.d("DropBox Auth Token - True :: Preference Click Test")
                    DropBox.clearAuthToken()
                    updateDropboxAccountInfo(null)
                } else {
                    Auth.startOAuth2Authentication(App.instance, getString(R.string.app_key))
                }

                true
            }
            getString(R.string.companyCamAuthPreferenceKey) -> {
                if (CompanyCamServiceFactory.bearerToken() == null)
                    (activity as SettingsActivity).authorizeCompanyCam()
                else {
                    CompanyCamServiceFactory.logout()
                    updateComapanyCamPreference()
                }
                return true
            }
            else -> { super.onPreferenceTreeClick(preferenceScreen, preference) }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("!! Settings Fragment - **** - ON RESUME !!")

        dropBox?.let {
            if (DropBox.hasToken()) {
                GetCurrentAccountTask(DropBox.getClient(), object : GetCurrentAccountTask.Callback {
                    override fun onComplete(result: FullAccount?) {
                        updateDropboxAccountInfo(result)
                    }
                    override fun onError(e: Exception?) { e?.printStackTrace() }
                }).execute()
            }
        }

        updateComapanyCamPreference()
    }

    fun updateDropboxAccountInfo(result: FullAccount?) {
        val info = "Logout and Login to Switch User Account"
        val default = "Access your DropBox Account via OAuth."
        val pref = findPreference(getString(R.string.drop_box_auth))
        pref.title = result?.name?.displayName?:getString(R.string.drop_box_auth)
        pref.summary = "${result?.email?:default} - $info"
    }

    private fun updateComapanyCamPreference() {
        val pref = findPreference(getString(R.string.companyCamAuthPreferenceKey))

        if (CompanyCamServiceFactory.bearerToken() == null) {
            pref.title = getString(R.string.companyCamAuthPreferenceTitle)
            pref.summary = getString(R.string.companyCamAuthPreferenceSummary)
        }
        else {
            pref.title = "Logout from Company Cam"
            pref.summary = "You will not be able to upload photos until you sign back in."
        }
    }
}


