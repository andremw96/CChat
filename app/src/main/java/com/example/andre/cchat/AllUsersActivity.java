package com.example.andre.cchat;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView allUsersList;
    private DatabaseReference allUsersRef;

    private EditText searchInputText;
    private ImageButton searchButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        mToolbar = (Toolbar) findViewById(R.id.all_users_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Semua User");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        allUsersList = (RecyclerView) findViewById(R.id.all_users_list);
        allUsersList.setHasFixedSize(true);
        allUsersList.setLayoutManager(new LinearLayoutManager(this));

        searchButton = (ImageButton) findViewById(R.id.search_people_button);
        searchInputText = (EditText) findViewById(R.id.search_input_text);

        // ambil data dari firebase yg tabelnya users
        allUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        // load data offline
        allUsersRef.keepSynced(true);

        searchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String searchUserName = searchInputText.getText().toString().toLowerCase();

                if(TextUtils.isEmpty(searchUserName))
                {
                    Toast.makeText(AllUsersActivity.this, "Isikan nama teman yang dicari", Toast.LENGTH_SHORT).show();
                }

                cariTeman(searchUserName);
            }
        });

    }

    private void cariTeman(String searchUserName)
    {
        Toast.makeText(this, "Sedang Mencari...", Toast.LENGTH_SHORT).show();

        Query searchFriends = allUsersRef.orderByChild("user_name_lowercase")
                .startAt(searchUserName).endAt(searchUserName + "\uf8ff");

        // we need firebase recycler adapter for retrieve data from firebase and display on our recycler view
        // first we need include library to use firebase adapter

        // 2nd paramater viewholder static class
        FirebaseRecyclerAdapter<AllUsers, AllUsersViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<AllUsers, AllUsersViewHolder>
                (
                        AllUsers.class,
                        R.layout.all_users_display_layout,
                        AllUsersViewHolder.class,
                        searchFriends
                )
        {
            @Override
            // digunakan untuk set values for recycler view
            protected void populateViewHolder(AllUsersViewHolder viewHolder, final AllUsers model, final int position)
            {
                // mode.getusername itu buat ngambi data username dari firebase
                viewHolder.setUser_name(model.getUser_name());
                viewHolder.setUser_status(model.getUser_status());
                viewHolder.setUser_thumb_image(getApplicationContext(), model.getUser_thumb_image());


                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // kita butuh ID untuk membuka profie activity seseorang, disini akan menggunakan parameter position
                        // getKey akan mengambil unique ID
                        String visit_user_id = getRef(position).getKey();
                        String user_name = model.getUser_name();

                        Intent profileIntent = new Intent(AllUsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("visit_user_id", visit_user_id);
                        profileIntent.putExtra("user_name", user_name);
                        startActivity(profileIntent);
                    }
                });
            }
        };

        allUsersList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class AllUsersViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public AllUsersViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setUser_name(String user_name)
        {
            // set nama di layout
            TextView name = (TextView) mView.findViewById(R.id.all_users_username);
            name.setText(user_name);
        }

        public void setUser_status(String user_status)
        {
            TextView status = (TextView) mView.findViewById(R.id.all_users_status);
            status.setText(user_status);
        }

        public void  setUser_thumb_image(final Context ctx, final String user_thumb_image)
        {
            final CircleImageView thumb_image = (CircleImageView) mView.findViewById(R.id.all_users_profile_image);

            // load images offline
            Picasso.with(ctx).load(user_thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).
                    into(thumb_image, new Callback() {
                        @Override
                        // onsuccess akan melload picture offline
                        public void onSuccess() {

                        }

                        @Override
                        // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                        public void onError() {
                            Picasso.with(ctx).load(user_thumb_image).placeholder(R.drawable.default_profile).into(thumb_image);
                        }
                    });
        }
    }
}
