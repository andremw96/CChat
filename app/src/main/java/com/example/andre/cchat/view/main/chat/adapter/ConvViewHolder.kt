package com.example.andre.cchat.view.main.chat.adapter

import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.andre.cchat.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ConvViewHolder(var mView: View) : RecyclerView.ViewHolder(mView) {
    fun setMessage(message: String?, isSeen: Boolean) {
        val userStatusView = mView.findViewById<View>(R.id.all_users_status) as TextView
        userStatusView.text = message
        if (!isSeen) {
            userStatusView.setTypeface(userStatusView.typeface, Typeface.BOLD)
        } else {
            userStatusView.setTypeface(userStatusView.typeface, Typeface.NORMAL)
        }
    }

    fun setUserName(userName: String?) {
        val userNameDisplay = mView.findViewById<View>(R.id.all_users_username) as TextView
        userNameDisplay.text = userName
    }

    fun setUserThumbImage(ctx: Context?, userThumbImage: String?) {
        val thumbImage = mView.findViewById<View>(R.id.all_users_profile_image) as CircleImageView

        // load images offline
        Picasso.with(ctx).load(userThumbImage).networkPolicy(NetworkPolicy.OFFLINE)
            .placeholder(R.drawable.default_profile).into(thumbImage, object : Callback {
                // onsuccess akan melload picture offline
                override fun onSuccess() {}

                // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                override fun onError() {
                    Picasso.with(ctx).load(userThumbImage).placeholder(R.drawable.default_profile)
                        .into(thumbImage)
                }
            })
    }

    fun setUserOnline(online_status: String) {
        val onlineStatusView = mView.findViewById<View>(R.id.online_status) as ImageView

        // cek value online_status yg didapat dari database
        // jika true berarti user itu online
        if (online_status == "true") {
            onlineStatusView.visibility = View.VISIBLE
        } else {
            onlineStatusView.visibility = View.INVISIBLE
        }
    }
}
