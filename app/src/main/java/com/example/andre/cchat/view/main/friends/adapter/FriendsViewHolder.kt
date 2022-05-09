package com.example.andre.cchat.view.main.friends.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.andre.cchat.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class FriendsViewHolder(var mView: View) : RecyclerView.ViewHolder(mView) {
    fun setDate(date: String) {
        val sinceFriendDate = mView.findViewById<View>(R.id.all_users_status) as TextView
        sinceFriendDate.text = "Berteman sejak : \n$date"
    }

    fun setUserName(userName: String?) {
        val userNameDisplay = mView.findViewById<View>(R.id.all_users_username) as TextView
        userNameDisplay.text = userName
    }

    fun setUserThumbImage(ctx: Context?, userThumbImage: String?) {
        val thumb_image = mView.findViewById<View>(R.id.all_users_profile_image) as CircleImageView

        // load images offline
        Picasso.with(ctx).load(userThumbImage).networkPolicy(NetworkPolicy.OFFLINE)
            .placeholder(R.drawable.default_profile).into(thumb_image, object : Callback {
                // onsuccess akan melload picture offline
                override fun onSuccess() {}

                // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                override fun onError() {
                    Picasso.with(ctx).load(userThumbImage).placeholder(R.drawable.default_profile)
                        .into(thumb_image)
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
