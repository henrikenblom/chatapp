package com.enblom.chatapp

import android.content.Context
import android.os.Bundle
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient

class GoogleApiHelper(var context: Context) : GoogleApiClient.ConnectionCallbacks {

    var googleApiClient: GoogleApiClient

    init {
        googleApiClient = createGoogleApiClient()
        connect()
    }

    private fun connect() {
        if (!googleApiClient.isConnected) {
            googleApiClient.connect()
        }
    }

    /*
    * Initialize Google Sign-in options
    * */
    private fun createGoogleSignInOptions() = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

    /*
    * Initialize Google API Client
    * */
    private fun createGoogleApiClient() = GoogleApiClient.Builder(context)
            .addConnectionCallbacks(this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, createGoogleSignInOptions())
            .build()

    override fun onConnected(p0: Bundle?) {

    }

    override fun onConnectionSuspended(p0: Int) {

    }

}