package com.enblom.chatapp

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.fabric.sdk.android.Fabric
import org.jetbrains.anko.toast

class ChatApp : Application() {

    val RC_SIGN_IN = 9001
    lateinit var googleApiHelper: GoogleApiHelper
    var connected = false
    var starting = true
    var inForeground = false

    companion object {
        lateinit var instance: ChatApp
    }

    override fun onCreate() {

        super.onCreate()
        Fabric.with(this, Crashlytics())
        instance = this
        googleApiHelper = GoogleApiHelper(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        FirebaseDatabase.getInstance().getReference(".info/connected")
                .addValueEventListener(object : ValueEventListener {

                    override fun onCancelled(error: DatabaseError?) = Unit

                    override fun onDataChange(snapshot: DataSnapshot?) {
                        ChatApp.instance.connected = snapshot?.getValue(Boolean::class.java) as Boolean
                        if (inForeground) {
                            if (connected) {
                                toast(R.string.connected)
                                starting = false
                            } else if (!starting) {
                                toast(R.string.disconnected)
                            }
                        }
                    }

                })

    }
}