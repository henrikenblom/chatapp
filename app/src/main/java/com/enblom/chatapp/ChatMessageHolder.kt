package com.enblom.chatapp

import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.curioustechizen.ago.RelativeTimeTextView
import com.makeramen.roundedimageview.RoundedTransformationBuilder
import com.squareup.picasso.Picasso

class ChatMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val emojiRegexUtil = EmojiRegexUtil()
    private val messageLayout: LinearLayout? = itemView.findViewById(R.id.messageLayout)
    private val messageTextView: TextView? = itemView.findViewById(R.id.messageTextView)
    private val otherImageView: ImageView? = itemView.findViewById(R.id.otherImageView)
    private val meImageView: ImageView? = itemView.findViewById(R.id.meImageView)
    private val messageTimeView: RelativeTimeTextView? = itemView.findViewById(R.id.messageTimeView)
    private val inlineImageView: ImageView? = itemView.findViewById(R.id.inlineImageView)
    private val cardView: CardView? = itemView.findViewById(R.id.cardView)

    enum class ImageDecoration {
        ME, OTHER, NONE
    }

    fun bindImageMessage(imageUri: Uri, photoUrl: String? = null, ownMessage: Boolean, imageDecoration: ImageDecoration, messageTime: Long?) {

        messageTextView?.visibility = View.GONE

        if (messageTime != null) {
            messageTimeView?.setReferenceTime(messageTime)
            messageTimeView?.layoutParams?.height = itemView.resources.getDimensionPixelSize(R.dimen.message_time)
        } else {
            messageTimeView?.layoutParams?.height = itemView.resources.getDimensionPixelSize(R.dimen.empty_message_time)
        }

        if (ownMessage) {
            if (photoUrl != null) meImageView?.loadUrlAsRoundImage(photoUrl)
            messageLayout?.gravity = Gravity.END
        } else {
            if (photoUrl != null) otherImageView?.loadUrlAsRoundImage(photoUrl)
            messageLayout?.gravity = Gravity.START
        }

        when (imageDecoration) {
            ImageDecoration.ME -> {
                meImageView?.visibility = View.VISIBLE
                otherImageView?.visibility = View.INVISIBLE
            }
            ImageDecoration.OTHER -> {
                meImageView?.visibility = View.INVISIBLE
                otherImageView?.visibility = View.VISIBLE
            }
            else -> {
                meImageView?.visibility = View.INVISIBLE
                otherImageView?.visibility = View.INVISIBLE
            }
        }

        inlineImageView?.visibility = View.VISIBLE
        inlineImageView?.loadUri(imageUri)

    }

    fun bindTextMessage(text: String?, photoUrl: String? = null, ownMessage: Boolean, imageDecoration: ImageDecoration, messageTime: Long?) {

        messageTextView?.text = text
        var messageBackgroundColor = ContextCompat.getColor(itemView.context, R.color.chatapp_my_message)
        var messageTextSize = itemView.resources.getDimension(R.dimen.chat_message_textsize)
        val emojiTextSize = messageTextSize * 2

        if (messageTime != null) {
            messageTimeView?.setReferenceTime(messageTime)
            messageTimeView?.layoutParams?.height = itemView.resources.getDimensionPixelSize(R.dimen.message_time)
        } else {
            messageTimeView?.layoutParams?.height = itemView.resources.getDimensionPixelSize(R.dimen.empty_message_time)
        }

        if (ownMessage) {
            if (photoUrl != null) meImageView?.loadUrlAsRoundImage(photoUrl)
            messageLayout?.gravity = Gravity.END
        } else {
            if (photoUrl != null) otherImageView?.loadUrlAsRoundImage(photoUrl)
            messageLayout?.gravity = Gravity.START
            messageBackgroundColor = ContextCompat.getColor(itemView.context, R.color.chatapp_other_message)
        }

        if (text != null
                && text.matches(emojiRegexUtil.fullEmojiRegex)) {
            messageBackgroundColor = ContextCompat.getColor(itemView.context, R.color.transparent)
            messageTextSize = emojiTextSize
        }

        cardView?.setCardBackgroundColor(messageBackgroundColor)
        messageTextView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, messageTextSize)

        when (imageDecoration) {
            ImageDecoration.ME -> {
                meImageView?.visibility = View.VISIBLE
                otherImageView?.visibility = View.INVISIBLE
            }
            ImageDecoration.OTHER -> {
                meImageView?.visibility = View.INVISIBLE
                otherImageView?.visibility = View.VISIBLE
            }
            else -> {
                meImageView?.visibility = View.INVISIBLE
                otherImageView?.visibility = View.INVISIBLE
            }
        }

    }

    private fun ImageView.loadUrlAsRoundImage(url: String) {

        val transformation = RoundedTransformationBuilder()
                .oval(true)
                .build()

        Picasso.with(context).load(url).fit().transform(transformation).into(this)

    }

    private fun ImageView.loadUri(uri: Uri) {
        Picasso.with(context).load(uri).into(this)
    }

}