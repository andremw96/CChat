package com.example.andre.cchat.view.main.request.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.example.andre.cchat.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class RequestsViewHolder(var mView: View) : RecyclerView.ViewHolder(mView) {

    fun setUserName(userName: String?) {
        val userNameDisplay = mView.findViewById<View>(R.id.request_profile_name) as TextView
        userNameDisplay.text = userName
    }

    fun setUserThumbImage(ctx: Context?, userThumbImage: String?) {
        val thumb_image = mView.findViewById<View>(R.id.request_profile_image) as CircleImageView

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

    fun setUserStatus(userStatus: String?) {
        val userStatusDisplay = mView.findViewById<View>(R.id.request_profile_status) as TextView
        userStatusDisplay.text = userStatus
    }
}
