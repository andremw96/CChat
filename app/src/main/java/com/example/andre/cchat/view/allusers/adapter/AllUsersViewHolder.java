package com.example.andre.cchat.view.allusers.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.andre.cchat.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersViewHolder extends RecyclerView.ViewHolder {
    public View mView;

    public AllUsersViewHolder(View itemView) {
        super(itemView);

        mView = itemView;
    }

    public void setUser_name(String user_name) {
        // set nama di layout
        TextView name = (TextView) mView.findViewById(R.id.all_users_username);
        name.setText(user_name);
    }

    public void setUser_status(String user_status) {
        TextView status = (TextView) mView.findViewById(R.id.all_users_status);
        status.setText(user_status);
    }

    public void setUser_thumb_image(final Context ctx, final String user_thumb_image) {
        final CircleImageView thumb_image = (CircleImageView) mView.findViewById(R.id.all_users_profile_image);

        // load images offline
        Picasso.with(ctx).load(user_thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).
                into(thumb_image, new Callback() {
                    @Override
                    // onsuccess akan melload picture offline
                    public void onSuccess() { }

                    @Override
                    // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                    public void onError() {
                        Picasso.with(ctx).load(user_thumb_image).placeholder(R.drawable.default_profile).into(thumb_image);
                    }
                });
    }
}
