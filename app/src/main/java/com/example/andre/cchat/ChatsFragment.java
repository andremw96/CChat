package com.example.andre.cchat;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private View mMainView;

    private RecyclerView myChatsList;

    private DatabaseReference FriendsReference;
    private DatabaseReference UsersReference;
    private DatabaseReference MessagesReference;
    private DatabaseReference ConvReference;

    private FirebaseAuth mAuth;

    String online_user_id;


    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mMainView = inflater.inflate(R.layout.fragment_chats, container, false);

        // linking mMainView ke recyclerView
        myChatsList = (RecyclerView) mMainView.findViewById(R.id.chats_fragment_list);

        // casting mAuth sehingga bisa memakai getUid
        mAuth = FirebaseAuth.getInstance();
        online_user_id = mAuth.getCurrentUser().getUid();

        // get the friend list for the online user
        FriendsReference = FirebaseDatabase.getInstance().getReference().child("Friends").child(online_user_id);

        ConvReference = FirebaseDatabase.getInstance().getReference().child("Chat").child(online_user_id);
        ConvReference.keepSynced(true);

        UsersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        UsersReference.keepSynced(true);

        MessagesReference = FirebaseDatabase.getInstance().getReference().child("Messages").child(online_user_id);

        // set the layout manager
        myChatsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myChatsList.setLayoutManager(linearLayoutManager);



        // Inflate the layout for this fragment
        return mMainView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Query convQuery = ConvReference.orderByChild("timestamp");

            FirebaseRecyclerAdapter<Conv, ChatsFragment.ConvViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Conv, ConvViewHolder>
                (
                        Conv.class,
                        R.layout.all_users_display_layout,
                        ChatsFragment.ConvViewHolder.class,
                        convQuery
                )
        {
            @Override
            protected void populateViewHolder(final ChatsFragment.ConvViewHolder viewHolder, final Conv model, int position)
            {
                final String list_user_id = getRef(position).getKey();

                Query lastMessageQuery = MessagesReference.child(list_user_id).limitToLast(1);

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String data = dataSnapshot.child("message").getValue().toString();
                        viewHolder.setMessage(data, model.isSeen());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                UsersReference.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot)
                    {
                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                        String userThumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                     //   String userStatus = dataSnapshot.child("user_status").getValue().toString();

                        if(dataSnapshot.hasChild("online"))
                        {
                            String online_status = (String) dataSnapshot.child("online").getValue().toString();

                            viewHolder.setUserOnline(online_status);
                        }

                        viewHolder.setUserName(userName);
                        viewHolder.setUserThumbImage(getContext(), userThumbImage);
                       // viewHolder.setUserStatus(userStatus);

                        viewHolder.mView.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                // biar gk eror, dibuat validasi
                                // misal ada user tidak online 6 bulan, dan ketika dia buka app mychat bisa eror
                                // maka dikasi validasi
                                if(dataSnapshot.child("online").exists())
                                {
                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    chatIntent.putExtra("visit_user_id", list_user_id);
                                    chatIntent.putExtra("user_name", userName);
                                    startActivity(chatIntent);
                                }
                                else
                                {
                                    UsersReference.child(list_user_id).child("online").
                                            setValue(ServerValue.TIMESTAMP).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid)
                                        {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("visit_user_id", list_user_id);
                                            chatIntent.putExtra("user_name", userName);
                                            startActivity(chatIntent);
                                        }
                                    });
                                }
                            }
                        });


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        myChatsList.setAdapter(firebaseRecyclerAdapter);
    }


    public static class ConvViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public ConvViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setMessage(String message, boolean isSeen){

            TextView userStatusView = (TextView) mView.findViewById(R.id.all_users_status);
            userStatusView.setText(message);

            if(!isSeen){
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            } else {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
            }

        }

        public void setUserName(String userName)
        {
            TextView userNameDisplay = (TextView) mView.findViewById(R.id.all_users_username);
            userNameDisplay.setText(userName);
        }


        public void setUserThumbImage(final Context ctx, final String userThumbImage)
        {
            final CircleImageView thumb_image = (CircleImageView) mView.findViewById(R.id.all_users_profile_image);

            // load images offline
            Picasso.with(ctx).load(userThumbImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).
                    into(thumb_image, new Callback() {
                        @Override
                        // onsuccess akan melload picture offline
                        public void onSuccess() {

                        }

                        @Override
                        // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                        public void onError() {
                            Picasso.with(ctx).load(userThumbImage).placeholder(R.drawable.default_profile).into(thumb_image);
                        }
                    });
        }

        public void setUserOnline(String online_status)
        {
            ImageView onlineStatusView = (ImageView) mView.findViewById(R.id.online_status);

            // cek value online_status yg didapat dari database
            // jika true berarti user itu online
            if(online_status.equals("true"))
            {
                onlineStatusView.setVisibility(View.VISIBLE);
            }
            else
            {
                onlineStatusView.setVisibility(View.INVISIBLE);
            }
        }

        /* public void setUserStatus(String userStatus)
        {
            TextView user_status = (TextView) mView.findViewById(R.id.all_users_status);
            user_status.setText(userStatus);
        } */
    }
}
