package com.enblom.chatapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast

fun Context.LoginActivityIntent(postLoginGoto: Int, chatKey: String = "", chatName: String = ""): Intent {
    val intent = Intent(this, LoginActivity::class.java)
    intent.putExtra("postLoginGoto", postLoginGoto)
    intent.putExtra("chatKey", chatKey)
    intent.putExtra("chatName", chatName)
    return intent
}

class LoginActivity : AppCompatActivity() {

    companion object {
        val POSTLOGIN_GOTO_MAIN = 1
        val POSTLOGIN_GOTO_CHAT = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        signInButton.setOnClickListener {
            signIn()
        }

    }

    private fun signIn() {
        loginProgress.visibility = View.VISIBLE
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(ChatApp.instance.googleApiHelper.googleApiClient)
        startActivityForResult(signInIntent, ChatApp.instance.RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ChatApp.instance.RC_SIGN_IN) {

            val signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (signInResult.isSuccess) {
                firebaseAuthWithGoogle(signInResult.signInAccount)
            } else {
                loginProgress.visibility = View.INVISIBLE
                if (resultCode != RESULT_CANCELED)
                    toast("Google authentication failed.")
            }

        }

    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {

        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)

        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        ChatApp.instance.currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        if (intent.getIntExtra("postLoginGoto", 0) == POSTLOGIN_GOTO_CHAT) {
                            startActivity(ChatActivityIntent(intent.getStringExtra("chatKey"),
                                    intent.getStringExtra("chatName")))
                        } else {
                            startActivity(MainActivityIntent())
                        }
                        finish()
                    } else {
                        toast("Firebase authentication failed.")
                    }
                    loginProgress.visibility = View.INVISIBLE
                }

    }
}