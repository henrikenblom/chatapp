package com.enblom.chatapp

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.firebase.ui.database.ClassSnapshotParser
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.github.curioustechizen.ago.RelativeTimeTextView
import com.google.firebase.database.Query
import org.jetbrains.anko.sdk25.coroutines.onClick

class ChatListEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val chatNameView: TextView? = itemView.findViewById(R.id.chatName)
    private val chatMemberImageList: RecyclerView? = itemView.findViewById(R.id.chatMemberImageList)
    private val lastActiveTimeView: RelativeTimeTextView? = itemView.findViewById(R.id.lastActiveTimeView)
    private val layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)

    fun bind(chatName: String, chatKey: String, query: Query, isGroupChat: Boolean, lastActive: Long) {

        chatNameView?.text = chatName
        chatMemberImageList?.layoutManager = layoutManager
        chatMemberImageList?.adapter = getAdapter(query)
        lastActiveTimeView?.setReferenceTime(lastActive)

        itemView.onClick {
            val context = itemView.context
            context.startActivity(context.ChatActivityIntent(chatKey, chatName))
        }

        chatMemberImageList?.onClick {
            val context = itemView.context
            context.startActivity(context.ChatActivityIntent(chatKey, chatName))
        }

    }

    private fun getAdapter(query: Query): FirebaseRecyclerAdapter<String, ChatMemberPhotoEntryHolder> {

        val parser = ClassSnapshotParser<String>(String::class.java)
        val firebaseArray = FirebaseArray(query, parser)

        return object : FirebaseRecyclerAdapter<String, ChatMemberPhotoEntryHolder>(
                firebaseArray,
                R.layout.chat_member_imagelist_entry,
                ChatMemberPhotoEntryHolder::class.java) {

            override fun populateViewHolder(viewHolder: ChatMemberPhotoEntryHolder?, model: String, position: Int) {
                viewHolder?.bind(model)
            }

        }

    }

}