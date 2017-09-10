package com.enblom.chatapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.firebase.ui.database.ClassSnapshotParser
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_chat.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.ByteArrayOutputStream


fun Context.ChatActivityIntent(chatKey: String, chatName: String): Intent {
    val intent = Intent(this, ChatActivity::class.java)
    intent.putExtra("chatKey", chatKey)
    intent.putExtra("chatName", chatName)
    return intent
}

class ChatActivity : ConnectedActivity() {

    private val MAX_BITMAP_SIZE: Double = 1920.0
    private val IMAGE_QUALITY = 80
    private val GALLERY_REQUEST_CODE = 67
    private val TEN_MINUTES = 600000L
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val mLinearLayoutManager: LinearLayoutManager = LinearLayoutManager(this)
    private val userProfileParser = ClassSnapshotParser<UserProfile>(UserProfile::class.java)
    private val userProfiles = HashMap<String, UserProfile>()
    private val profileReference: DatabaseReference = firebaseDatabase
            .getReference("userprofiles")
            .child(currentUser?.uid)
    private var storageRef = FirebaseStorage.getInstance().reference
    private var onDisconnectReference: OnDisconnect? = null
    private lateinit var userConnectionsReference: DatabaseReference
    private lateinit var chatKey: String
    private lateinit var chatName: String

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatKey = intent.getStringExtra("chatKey")
        chatName = intent.getStringExtra("chatName")

        setSupportActionBar(chatToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = chatName

        editText.parent = this

        submitButton.onClick {
            submitMessage()
        }

        photoLibraryButton.onClick {
            val intent = Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }

        userConnectionsReference = profileReference.child("connections").child(deviceId)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE) {

            if (data != null) {
                monitorUploadTask(submitImage(data.data))
            }

        }

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
                                onDisconnectReference = userConnectionsReference.onDisconnect()
                                onDisconnectReference?.removeValue()
                                userConnectionsReference.setValue(ServerValue.TIMESTAMP)
                                profileReference.child("lastSeenAt").onDisconnect().setValue(ServerValue.TIMESTAMP)
                            }

                        }

                    })
        }

    }

    override fun onPause() {
        super.onPause()
        userConnectionsReference.removeValue()
        profileReference.child("lastSeenAt").setValue(ServerValue.TIMESTAMP)
    }

    override fun getParentActivityIntent(): Intent {
        return MainActivityIntent()
    }

    private fun submitImage(uri: Uri): UploadTask {

        val bitmap = getBitmap(uri)
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, baos)
        val mediaReference = storageRef
                .child("media")
                .child(chatKey)
                .child(currentUser?.uid as String)
                .child(uri.hashCode().toString(16))

        return mediaReference.putBytes(baos.toByteArray())

    }

    fun submitMedia(uri: Uri): UploadTask {

        val mediaReference = storageRef
                .child("media")
                .child(chatKey)
                .child(currentUser?.uid as String)
                .child(uri.hashCode().toString(16))

        return mediaReference.putFile(uri)

    }

    fun monitorUploadTask(uploadTask: UploadTask) {

        uploadProgressBar.visibility = View.VISIBLE

        uploadTask.addOnProgressListener {
            uploadProgressBar.progress = (100f * (it.bytesTransferred / it.totalByteCount.toDouble())).toInt()
        }

        uploadTask.addOnCompleteListener {
            uploadProgressBar.visibility = View.GONE
        }

        uploadTask.addOnFailureListener({
        }).addOnSuccessListener({ taskSnapshot ->
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
        })

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
                    .child(chatKey)
                    .child("messages")
                    .push()
                    .setValue(message)

            editText.text.clear()

        }

    }

    private fun getAdapter(): FirebaseRecyclerAdapter<Message, ChatMessageHolder> {

        val query = databaseReference
                .child("chats")
                .child(chatKey)
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

                val type = message.type
                val path = message.path

                if (type != null
                        && type.startsWith("image")
                        && path != null) {

                    viewHolder?.bindImageMessage(storageRef.child(path), photoUrl, ownMessage, imageDecoration, submittedAt)

                } else {
                    viewHolder?.bindTextMessage(message.text, photoUrl, ownMessage, imageDecoration, submittedAt)
                }

            }

            override fun onDataChanged() {
                super.onDataChanged()
                mLinearLayoutManager.scrollToPosition(firebaseArray.size - 1)
            }

        }

    }

    fun getBitmap(uri: Uri): Bitmap? {

        var input = this.contentResolver.openInputStream(uri)

        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input.close()

        if (onlyBoundsOptions.outWidth == -1 || onlyBoundsOptions.outHeight == -1) {
            return null
        }

        val originalSize = if (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) onlyBoundsOptions.outHeight else onlyBoundsOptions.outWidth

        val ratio = if (originalSize > MAX_BITMAP_SIZE) originalSize / MAX_BITMAP_SIZE else 1.0

        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio)
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888//
        input = this.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input.close()
        return bitmap
    }

    private fun getPowerOfTwoForSampleRatio(ratio: Double): Int {
        val k = Integer.highestOneBit(Math.floor(ratio).toInt())
        return if (k == 0)
            1
        else
            k
    }

}
