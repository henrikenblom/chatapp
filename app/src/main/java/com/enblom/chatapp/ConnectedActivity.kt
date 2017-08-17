package com.enblom.chatapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

abstract class ConnectedActivity : AppCompatActivity() {

    val firebaseDatabase = FirebaseDatabase.getInstance()
    val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    protected fun verifyCredentials() {
        if (ChatApp.instance.connected) {
            if (currentUser != null) {
                currentUser.getIdToken(true).addOnCompleteListener {
                    if (it.isSuccessful) {
                        ChatApp.instance.currentUserId = currentUser.uid
                    } else {
                        gotoLogin()
                    }
                }
            } else {
                gotoLogin()
            }
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