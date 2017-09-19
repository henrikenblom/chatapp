package com.enblom.chatapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.media.ExifInterface
import android.support.v4.content.FileProvider
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
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


fun Context.ChatActivityIntent(chatKey: String, chatName: String): Intent {
    val intent = Intent(this, ChatActivity::class.java)
    intent.putExtra("chatKey", chatKey)
    intent.putExtra("chatName", chatName)
    return intent
}

class ChatActivity : ConnectedActivity() {

    private val MAX_BITMAP_SIZE_TARGET: Double = 1200.0
    private val IMAGE_QUALITY = 45
    private val GALLERY_REQUEST_CODE = 6711
    private val REQUEST_IMAGE_CAPTURE_CODE = 6712
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
    private lateinit var currentContext: Context
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        currentContext = this
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

        cameraButton.onClick {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                photoFile = createImageFile()
                val photoURI = FileProvider.getUriForFile(applicationContext,
                        "com.enblom.chatapp",
                        photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_CODE)

            }
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
        } else if (requestCode == REQUEST_IMAGE_CAPTURE_CODE) {
            MediaScannerConnection.scanFile(this, arrayOf(photoFile.toString()), null) { path, contentUri ->
                monitorUploadTask(submitImage(contentUri), this::deleteImageFile)
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

        baos.close()
        bitmap?.recycle()

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

    fun monitorUploadTask(uploadTask: UploadTask, callback: () -> Unit = {}) {

        runOnUiThread {

            if (uploadTask.isInProgress) {

                uploadProgressBar.visibility = View.VISIBLE

                uploadTask.addOnCompleteListener {
                    uploadProgressBar.visibility = View.GONE
                    callback()
                }.addOnProgressListener {
                    uploadProgressBar.progress = (100f * (it.bytesTransferred / it.totalByteCount.toDouble())).toInt()
                }

            } else {
                callback()
            }

        }

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

    private fun deleteImageFile() {
        photoFile?.delete()
    }

    private fun createImageFile(): File {

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        )
    }

    private fun getBitmap(uri: Uri): Bitmap? {

        var input = contentResolver.openInputStream(uri)
        val boundsOptions = BitmapFactory.Options()
        boundsOptions.inJustDecodeBounds = true
        boundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
        BitmapFactory.decodeStream(input, null, boundsOptions)
        input.close()

        if (boundsOptions.outWidth == -1 || boundsOptions.outHeight == -1) {
            return null
        }

        val originalSize = if (boundsOptions.outHeight > boundsOptions.outWidth)
            boundsOptions.outHeight
        else
            boundsOptions.outWidth
        val ratio = if (originalSize > MAX_BITMAP_SIZE_TARGET) originalSize / MAX_BITMAP_SIZE_TARGET else 1.0
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio)
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
        input = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input.close()

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, getImageRotationMatrix(uri), false)

    }

    private fun getImageRotationMatrix(uri: Uri): Matrix {

        val matrix = Matrix()
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.preRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.preRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.preRotate(270f)
            }
        } catch (e: IOException) {
            // noop
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (ignored: IOException) {
                }
            }
        }

        return matrix

    }

    private fun getPowerOfTwoForSampleRatio(ratio: Double): Int {
        val k = Integer.highestOneBit(Math.floor(ratio).toInt())
        return if (k == 0)
            1
        else
            k
    }

}
