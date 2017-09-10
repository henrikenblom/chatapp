package com.enblom.chatapp

import android.content.Context
import android.support.v13.view.inputmethod.EditorInfoCompat
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v4.os.BuildCompat
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText

class EnrichedEditText : EditText {

    val mimeTypes = arrayOf("image/gif")
    lateinit var parent: ChatActivity

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {

        val ic = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes)

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->

            if (BuildCompat.isAtLeastNMR1()
                    && flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false
                }

            }

            Log.e("RICH_CONTENT", "${inputContentInfo.contentUri}")

            if (parent != null) {
                parent.monitorUploadTask(parent.submitMedia(inputContentInfo.contentUri))
            }

            inputContentInfo.releasePermission()

            true

        }

        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)

    }

}