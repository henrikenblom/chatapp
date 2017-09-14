package com.enblom.chatapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.firebase.ui.database.ClassSnapshotParser
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_new_chat.*
import java.util.zip.CRC32


fun Context.NewChatActivityIntent(): Intent {
    return Intent(this, NewChatActivity::class.java)
}

class NewChatActivity : ConnectedActivity() {

    val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    val chatMembers = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_chat)
        setSupportActionBar(newChatToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        done_image_button.setOnClickListener {
            createChat()
        }

        if (currentUser != null) chatMembers.add(currentUser.uid)

        val mManager = LinearLayoutManager(this)
        mManager.reverseLayout = false

        userList.setHasFixedSize(false)
        userList.layoutManager = mManager

        databaseReference.child("chats").keepSynced(true)

    }

    override fun onStart() {

        super.onStart()

        if (currentUser != null) userList.adapter = getAdapter()

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getAdapter(): FirebaseRecyclerAdapter<UserProfile, UserListEntryHolder> {

        val query = databaseReference.child("userprofiles").orderByValue()
        val parser = ClassSnapshotParser<UserProfile>(UserProfile::class.java)
        val filterableFirebaseArray = FilterableFirebaseArray<UserProfile>(query, parser)

        filterableFirebaseArray.excludedKeys.add(currentUser?.uid)

        return object : FirebaseRecyclerAdapter<UserProfile, UserListEntryHolder>(
                filterableFirebaseArray,
                R.layout.userlist_entry,
                UserListEntryHolder::class.java) {
            public override fun populateViewHolder(entryHolder: UserListEntryHolder, data: UserProfile, position: Int) {

                val uid: String = data.uid as String
                entryHolder.bind(data)
                entryHolder.checkBox?.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked)
                        chatMembers.add(uid)
                    else
                        chatMembers.remove(uid)

                    if (chatMembers.size != 1) {
                        done_image_button.isEnabled = true
                        done_image_button.alpha = 1f
                    } else {
                        done_image_button.isEnabled = false
                        done_image_button.alpha = 0.5f
                    }
                }

            }

            override fun onDataChanged() {
                noFriendsTextView.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
                userList.visibility = if (itemCount == 0) View.GONE else View.VISIBLE
                userFetchProgressBar.visibility = View.GONE
            }
        }

    }

    private fun calculateMemberChecksum(): String {
        val chatChecksum = CRC32()
        for (chatMember: String in chatMembers.sorted())
            chatChecksum.update(chatMember.toByteArray())

        return chatChecksum.value.toString(16)
    }

    private fun createChat() {

        noFriendsTextView.visibility = View.GONE
        userList.visibility = View.GONE
        userFetchProgressBar.visibility = View.VISIBLE

        val chatKey = calculateMemberChecksum()

        databaseReference
                .child("user_chats")
                .child(currentUser?.uid)
                .child(chatKey)
                .addChildEventListener(object : ChildEventListener {
                    override fun onCancelled(p0: DatabaseError?) {
                    }

                    override fun onChildMoved(p0: DataSnapshot?, p1: String?) {
                    }

                    override fun onChildChanged(p0: DataSnapshot?, p1: String?) {
                    }

                    override fun onChildRemoved(p0: DataSnapshot?) {
                    }

                    override fun onChildAdded(snapshot: DataSnapshot?, p1: String?) {
                        if ("chat_name" == snapshot?.key) {
                            snapshot.ref.parent.removeEventListener(this)
                            startActivity(ChatActivityIntent(chatKey, snapshot.value as String))
                        }
                    }

                })

        databaseReference
                .child("chats").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) = Unit
            override fun onDataChange(memberSnapshot: DataSnapshot) {
                if (!memberSnapshot.hasChild(chatKey)) {
                    for (chatMember: String in chatMembers) databaseReference
                            .child("chats")
                            .child(chatKey)
                            .child("members")
                            .child(chatMember)
                            .child("creator")
                            .setValue(chatMember == currentUser?.uid)
                }
            }
        })

    }
}