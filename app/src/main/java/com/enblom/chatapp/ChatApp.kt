package com.enblom.chatapp

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.google.firebase.database.FirebaseDatabase
import io.fabric.sdk.android.Fabric

class ChatApp : Application() {

    val RC_SIGN_IN = 9001
    lateinit var googleApiHelper: GoogleApiHelper

    companion object {
        lateinit var instance: ChatApp
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        instance = this
        googleApiHelper = GoogleApiHelper(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}