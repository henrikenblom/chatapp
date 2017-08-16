package com.enblom.chatapp

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.makeramen.roundedimageview.RoundedTransformationBuilder
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

class ChatMemberPhotoEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val chatMemberImage: ImageView? = itemView.findViewById(R.id.chatMemberImage)
    private val roundedTransformation: Transformation? = RoundedTransformationBuilder()
            .oval(true)
            .build()

    fun bind(photoUrl: String) {
        Picasso.with(itemView.context).load(photoUrl).fit().transform(roundedTransformation).into(chatMemberImage)
    }

}