package com.example.andre.cchat.view.allusers.adapter;

import android.content.Context;
import android.content.Intent;

import com.example.andre.cchat.model.AllUsers;
import com.example.andre.cchat.view.profile.ProfileActivity;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;

public class AllUsersAdapter extends FirebaseRecyclerAdapter<AllUsers, AllUsersViewHolder> {

    private final Context context;

    public AllUsersAdapter(Class<AllUsers> modelClass, int modelLayout, Class<AllUsersViewHolder> viewHolderClass, Query query, Context context) {
        super(modelClass, modelLayout, viewHolderClass, query);
        this.context = context;
    }

    @Override
    protected void populateViewHolder(AllUsersViewHolder viewHolder, AllUsers model, int position) {
        // mode.getusername itu buat ngambi data username dari firebase
        viewHolder.setUser_name(model.getUser_name());
        viewHolder.setUser_status(model.getUser_status());
        viewHolder.setUser_thumb_image(context.getApplicationContext(), model.getUser_thumb_image());

        viewHolder.mView.setOnClickListener(view -> {
            // kita butuh ID untuk membuka profie activity seseorang, disini akan menggunakan parameter position
            // getKey akan mengambil unique ID
            String visit_user_id = getRef(position).getKey();
            String user_name = model.getUser_name();

            Intent profileIntent = new Intent(context, ProfileActivity.class);
            profileIntent.putExtra("visit_user_id", visit_user_id);
            profileIntent.putExtra("user_name", user_name);
            context.startActivity(profileIntent);
        });
    }
}


