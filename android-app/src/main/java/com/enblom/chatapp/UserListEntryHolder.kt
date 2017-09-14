package com.enblom.chatapp

import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class UserListEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val textView: TextView? = itemView.findViewById(R.id.displayNameView)
    private val imageView: ImageView? = itemView.findViewById(R.id.imageView)
    val checkBox: CheckBox? = itemView.findViewById(R.id.checkBox)
    private var uid: String? = ""

    fun bind(userProfile: UserProfile) {
        setDisplayName(userProfile.displayName)
        setProfileImage(userProfile.photoURL)
        uid = userProfile.uid
        itemView.setOnClickListener {
            checkBox?.toggle()
        }
    }

    private fun setDisplayName(displayName: String?) {
        textView?.text = displayName
    }

    private fun setProfileImage(photoUrl: String?) {
        Glide.with(itemView.context)
                .load(Uri.parse(photoUrl))
                .transform(CircleTransform(itemView.context))
                .into(imageView)
    }

}