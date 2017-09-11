package com.enblom.chatapp

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

class ChatMemberPhotoEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val chatMemberImage: ImageView? = itemView.findViewById(R.id.chatMemberImage)

    fun bind(photoUrl: String) {
        Glide.with(itemView.context)
                .load(photoUrl)
                .transform(CircleTransform(itemView.context))
                .into(chatMemberImage)
    }

}