package com.example.garmento.core.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {
    val SESSION_KEY = booleanPreferencesKey("is_logged_in")
    val USERNAME_KEY = stringPreferencesKey("username")
    val ROLE_KEY = stringPreferencesKey("role")
    val USER_ID_KEY = stringPreferencesKey("user_id")
    val EMAIL_KEY = stringPreferencesKey("email")
    val COMPANY_ID_KEY = stringPreferencesKey("company_id")
    val LOGIN_TIME_KEY = longPreferencesKey("login_time")
    val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    val GRACE_PERIOD_KEY = intPreferencesKey("grace_period_days")

    const val ADMIN_USERNAME = "admin"
    const val ADMIN_PASSWORD = "admin"

    const val DATABASE_NAME = "garmento_db"
    const val DATASTORE_NAME = "garmento_settings"

    const val WHATSAPP_BASE_URL = "https://wa.me/"
    const val SMS_BASE_URL = "smsto:"

    const val PORTAL_BASE_URL = "https://kfxldnebbqkwkzsxirny.supabase.co/customer/"

    const val OVERAGE_RATE_PER_ITEM = 12.5

    const val SESSION_DURATION_MS = 7L * 24 * 60 * 60 * 1000
}