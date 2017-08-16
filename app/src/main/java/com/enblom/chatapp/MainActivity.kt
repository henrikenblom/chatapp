package com.enblom.chatapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import com.firebase.ui.database.ClassSnapshotParser
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.makeramen.roundedimageview.RoundedTransformationBuilder
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

fun Context.MainActivityIntent(): Intent {
    return Intent(this, MainActivity::class.java)
}

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val mUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    val userProfileParser = ClassSnapshotParser<UserProfile>(UserProfile::class.java)
    private val mLinearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
    private val animationDuration = 300L

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainToolbar.setTitle(R.string.title_activity_main)
        setSupportActionBar(mainToolbar)

        mLinearLayoutManager.stackFromEnd = true
        chatList.layoutManager = mLinearLayoutManager

        fab.setOnClickListener {
            startActivity(NewChatActivityIntent())
        }

        val toggle = ActionBarDrawerToggle(this, drawer_layout, mainToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        drawer_layout.addDrawerListener(toggle)

        toggle.syncState()

        drawer_layout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawer_layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                viewUserEmail.text = mUser?.email
                viewUserDisplayName.text = mUser?.displayName
                viewUserImage.loadUrlAsRoundImage(mUser?.photoUrl.toString())
            }
        })

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        verifyCredentials()

    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        verifyCredentials()
    }

    override fun onStart() {

        super.onStart()

        if (mUser != null) {
            chatList.adapter = getAdapter()
            putNotificationToken()
        }

    }

    private fun verifyCredentials() {

        if (mUser != null) {
            mUser.getIdToken(true).addOnCompleteListener {
                if (!it.isSuccessful)
                    gotoLogin()
            }
        } else {
            gotoLogin()
        }

    }

    private fun gotoLogin() {
        startActivity(LoginActivityIntent(LoginActivity.POSTLOGIN_GOTO_MAIN))
        finish()
    }

    private fun putNotificationToken() {

        databaseReference
                .child("userprofiles")
                .child(mUser?.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) = Unit
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val userProfile: UserProfile? = userProfileParser.parseSnapshot(snapshot)
                        val tokens = userProfile?.notificationTokens
                        val token = FirebaseInstanceId.getInstance().token

                        if (tokens != null
                                && !tokens.contains(token)) {

                            tokens.add(token)

                            databaseReference
                                    .child("userprofiles")
                                    .child(mUser?.uid)
                                    .child("notificationTokens")
                                    .setValue(tokens)

                        }

                    }

                })

    }

    fun ImageView.loadUrlAsRoundImage(url: String) {

        val transformation = RoundedTransformationBuilder()
                .oval(true)
                .build()

        Picasso.with(context).load(url).fit().transform(transformation).into(this)

    }

    private fun signOut() {

        FirebaseAuth.getInstance().signOut()

        Auth.GoogleSignInApi.signOut(ChatApp.instance.googleApiHelper.googleApiClient).setResultCallback {
            if (it.isSuccess) {
                gotoLogin()
            }
        }

    }

    private fun getAdapter(): FirebaseRecyclerAdapter<Any, ChatListEntryHolder> {

        val uid = mUser?.uid
        val query = databaseReference.child("user_chats").child(uid).orderByChild("orderBy")
        val parser = ClassSnapshotParser<Any>(Any::class.java)
        val firebaseArray = FirebaseArray(query, parser)

        return object : FirebaseRecyclerAdapter<Any, ChatListEntryHolder>(
                firebaseArray,
                R.layout.chatlist_entry,
                ChatListEntryHolder::class.java) {
            public override fun populateViewHolder(entryHolder: ChatListEntryHolder, chatName: Any, position: Int) {
                val entryMap = chatName as HashMap<String, Any>
                val snapshot = firebaseArray[position]
                entryHolder.bind(entryMap["chat_name"] as String,
                        snapshot.key,
                        snapshot.ref.child("photos").orderByKey())
            }

            override fun onDataChanged() {
                if (itemCount == 0) {
                    fadeIn(noActiveChats)
                    fadeOut(loadChatsProgressBar)
                    fadeOut(chatList)
                } else {
                    fadeIn(chatList)
                    fadeOut(loadChatsProgressBar)
                    fadeOut(noActiveChats)
                }
            }
        }
    }

    private fun fadeOut(fromView: View) {
        if (fromView.visibility == View.VISIBLE) {
            fromView.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            fromView.visibility = View.GONE
                        }
                    })
        }
    }

    private fun fadeIn(view: View) {
        if (view.visibility != View.VISIBLE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null)
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        if (id == R.id.nav_sign_out) {
            signOut()
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}