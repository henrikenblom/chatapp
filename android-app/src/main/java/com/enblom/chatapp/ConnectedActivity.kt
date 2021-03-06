package com.enblom.chatapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import java.util.zip.CRC32

/**
 * A abstract Activity class to use for Firebase connected activities.
 *
 * Also provides logic for identifying foreground/background state of the app.
 */
abstract class ConnectedActivity : AppCompatActivity() {

    val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    lateinit var deviceId: String

    protected fun verifyCredentials() {

        if (currentUser != null) {
            if (ChatApp.instance.connected) {
                currentUser.getIdToken(true).addOnCompleteListener {
                    if (!it.isSuccessful) {
                        gotoLogin()
                    }
                }
            }
        } else {
            gotoLogin()
        }
    }

    private fun gotoLogin() {
        startActivity(LoginActivityIntent(LoginActivity.POSTLOGIN_GOTO_MAIN))
        finish()
    }

    protected fun signOut() {

        FirebaseAuth.getInstance().signOut()

        Auth.GoogleSignInApi.signOut(ChatApp.instance.googleApiHelper.googleApiClient).setResultCallback {
            if (it.isSuccess) {
                gotoLogin()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyCredentials()
        val checksum = CRC32()
        var instance: FirebaseInstanceId? = FirebaseInstanceId.getInstance()

        if (instance != null) {
            val token = instance.token
            if (!token.isNullOrEmpty()) {
                checksum.update(instance.token?.toByteArray())
                deviceId = checksum.value.toString(16)
            }
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        verifyCredentials()
    }

    override fun onStart() {
        super.onStart()
        ChatApp.instance.inForeground = true
    }

    override fun onPause() {
        super.onPause()
        ChatApp.instance.inForeground = false
    }

}