package com.mraphaelpy.terriflow

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporting {

    fun init() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        crashlytics.setCustomKey("versionName", BuildConfig.VERSION_NAME)
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            crashlytics.setUserId(auth.currentUser?.uid.orEmpty())
        }
    }
}
