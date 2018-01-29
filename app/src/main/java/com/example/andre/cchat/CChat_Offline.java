package com.example.andre.cchat;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

/**
 * Created by Wijaya_PC on 23-Jan-18.
 */

public class CChat_Offline extends Application
{
    private DatabaseReference UsersReference;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;


    @Override
    public void onCreate() {
        super.onCreate();

        // load semua data teks yg ada di firebase
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //load picture offline
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttpDownloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);

        // melihat user yg online
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // jika usernya login ke akunnya or it means usernya online
        if(currentUser != null)
        {
            String online_user_id = mAuth.getCurrentUser().getUid();
            // create new node on users, yg akan mengecek apakah user tersebut online atau tidak
            // dgn true / false

            UsersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(online_user_id);

            UsersReference.addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    // jika user close app, maka aplikasi akan set node online ke false
                    UsersReference.child("online").onDisconnect().setValue(ServerValue.TIMESTAMP);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }
}
