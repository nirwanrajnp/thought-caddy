package com.nirwanrn.thoughtcaddy.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var savedUserEmail: String?
        get() = prefs.getString(KEY_SAVED_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_SAVED_EMAIL, value).apply()

    var hasDeclinedBiometric: Boolean
        get() = prefs.getBoolean(KEY_DECLINED_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_DECLINED_BIOMETRIC, value).apply()

    fun clearBiometricData() {
        prefs.edit()
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_DECLINED_BIOMETRIC)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "biometric_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_DECLINED_BIOMETRIC = "declined_biometric"
    }
}