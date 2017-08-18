package com.enblom.chatapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.firebase.ui.database.ClassSnapshotParser
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chat.*
import org.jetbrains.anko.sdk25.coroutines.onClick

fun Context.ChatActivityIntent(chatKey: String, chatName: String): Intent {
    val intent = Intent(this, ChatActivity::class.java)
    intent.putExtra("chatKey", chatKey)
    intent.putExtra("chatName", chatName)
    return intent
}

class ChatActivity : ConnectedActivity() {

    private val TEN_MINUTES = 600000L
    val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    val mLinearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
    val userProfileParser = ClassSnapshotParser<UserProfile>(UserProfile::class.java)
    val userProfiles = HashMap<String, UserProfile>()
    val profileReference: DatabaseReference = firebaseDatabase
            .getReference("userprofiles")
            .child(currentUser?.uid)
    var userConnectionsReference: DatabaseReference? = null
    var onDisconnectReference: OnDisconnect? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val chatKey = intent.getStringExtra("chatKey")
        val chatName = intent.getStringExtra("chatName")

        setSupportActionBar(chatToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = chatName

        submitButton.onClick {
            submitMessage()
        }

        userConnectionsReference = profileReference.child("connections").child(deviceId)

        if (currentUser == null) {
            startActivity(LoginActivityIntent(LoginActivity.POSTLOGIN_GOTO_CHAT,
                    chatKey,
                    chatName))
        }

        chatMessageView.layoutManager = mLinearLayoutManager

        databaseReference
                .child("chats")
                .child(chatKey)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onCancelled(p0: DatabaseError?) {
                    }

                    override fun onDataChange(memberSnapshot: DataSnapshot) {
                        val members = (memberSnapshot.value as HashMap<String, Any>)["members"] as HashMap<String, Any>
                        var index = 1
                        for (memberKey in members.keys) {
                            databaseReference
                                    .child("userprofiles")
                                    .child(memberKey)
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onCancelled(p0: DatabaseError?) {
                                        }

                                        override fun onDataChange(profileSnapshot: DataSnapshot) {
                                            userProfiles.put(memberKey, userProfileParser.parseSnapshot(profileSnapshot))
                                            if (chatMessageView.adapter == null
                                                    && index++ == members.size) chatMessageView.adapter = getAdapter()
                                        }

                                    })
                        }
                    }

                })

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {

        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            onDisconnectReference?.cancel()

            FirebaseDatabase.getInstance().getReference(".info/connected")
                    .addValueEventListener(object : ValueEventListener {

                        override fun onCancelled(error: DatabaseError?) = Unit

                        override fun onDataChange(snapshot: DataSnapshot?) {

                            if (snapshot?.getValue(Boolean::class.java) as Boolean) {
                                onDisconnectReference = userConnectionsReference?.onDisconnect()
                                onDisconnectReference?.removeValue()
                                userConnectionsReference?.setValue(ServerValue.TIMESTAMP)
                                profileReference.child("lastSeenAt").onDisconnect().setValue(ServerValue.TIMESTAMP)
                            }

                        }

                    })
        }

    }

    override fun onPause() {
        super.onPause()
        userConnectionsReference?.removeValue()
        profileReference.child("lastSeenAt").setValue(ServerValue.TIMESTAMP)
    }

    override fun getParentActivityIntent(): Intent {
        return MainActivityIntent()
    }

    private fun submitMessage() {

        val text = editText.text.toString()

        if (currentUser != null
                && !text.isEmpty()) {

            val message = HashMap<String, String>()
            message.put("text", text)
            message.put("postedBy", currentUser.uid)

            databaseReference
                    .child("chats")
                    .child(intent.getStringExtra("chatKey"))
                    .child("messages")
                    .push()
                    .setValue(message)

            editText.text.clear()

        }

    }

    private fun getAdapter(): FirebaseRecyclerAdapter<Message, ChatMessageHolder> {

        val query = databaseReference
                .child("chats")
                .child(intent.getStringExtra("chatKey"))
                .child("messages")
                .orderByKey()
        val parser = ClassSnapshotParser<Message>(Message::class.java)
        val firebaseArray = FirebaseArray(query, parser)

        return object : FirebaseRecyclerAdapter<Message, ChatMessageHolder>(firebaseArray,
                R.layout.chat_message,
                ChatMessageHolder::class.java) {
            override fun populateViewHolder(viewHolder: ChatMessageHolder?, message: Message, position: Int) {

                val ownMessage = message.postedBy == currentUser?.uid
                var imageDecoration = if (ownMessage)
                    ChatMessageHolder.ImageDecoration.ME
                else
                    ChatMessageHolder.ImageDecoration.OTHER
                var photoUrl: String? = null
                var submittedAt: Long? = null

                if (position > 0) {
                    val lastMessage = parser.parseSnapshot(firebaseArray[position - 1])
                    if (lastMessage?.postedBy == message.postedBy)
                        imageDecoration = ChatMessageHolder.ImageDecoration.NONE
                    if (message.submittedAt != null
                            && lastMessage.submittedAt != null) {
                        if ((message.submittedAt as Long - lastMessage.submittedAt as Long) > TEN_MINUTES) {
                            submittedAt = message.submittedAt
                        }
                    }

                } else {
                    submittedAt = message.submittedAt
                }

                if (userProfiles.containsKey(message.postedBy))
                    photoUrl = userProfiles[message.postedBy]?.photoURL

                viewHolder?.bind(message.text, photoUrl, ownMessage, imageDecoration, submittedAt)

            }

            override fun onDataChanged() {
                super.onDataChanged()
                mLinearLayoutManager.scrollToPosition(firebaseArray.size - 1)
            }

        }

    }

}
