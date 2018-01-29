package com.example.andre.cchat;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView mRequestsList;

    private View mMainView;

    private DatabaseReference friendRequestsReference;
    private FirebaseAuth mAuth;
    private DatabaseReference usersReference;

    private DatabaseReference friendsReference;
    private DatabaseReference friendsReqReference;

    String online_user_id;


    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mMainView = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();
        online_user_id = mAuth.getCurrentUser().getUid();

        friendRequestsReference = FirebaseDatabase.getInstance().getReference().child("Friend_Requests").child(online_user_id);

        // digunakan untuk mengambil data2 user
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");

        friendsReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        friendsReqReference = FirebaseDatabase.getInstance().getReference().child("Friend_Requests");

        mRequestsList = (RecyclerView) mMainView.findViewById(R.id.requests_list);

        mRequestsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        // show the new friend request at the top
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mRequestsList.setLayoutManager(linearLayoutManager);




        // Inflate the layout for this fragment
        return mMainView;
    }



    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Requests, requestViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Requests, requestViewHolder>
                (
                        Requests.class,
                        R.layout.friend_request_all_users_layout,
                        RequestsFragment.requestViewHolder.class,
                        friendRequestsReference
                )
        {
            @Override
            protected void populateViewHolder(final requestViewHolder viewHolder, Requests model, int position)
            {
                final String lists_user_id = getRef(position).getKey();

                DatabaseReference get_typerequest_ref = getRef(position).child("request_type").getRef();

                get_typerequest_ref.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            String request_type = dataSnapshot.getValue().toString();

                            // jika user yg sedang login itu, menerima request dari orang lain
                            // maka data orang yg mengirim request akan muncul
                            if(request_type.equals("received"))
                            {
                                usersReference.child(lists_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                                        final String userThumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                                        final String userStatus = dataSnapshot.child("user_status").getValue().toString();

                                        viewHolder.setUserName(userName);
                                        viewHolder.setUserThumbImage(getContext(), userThumbImage);
                                        viewHolder.setUserStatus(userStatus);

                                        Button req_sent_btn = viewHolder.mView.findViewById(R.id.request_accept_btn);
                                        req_sent_btn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Calendar calForDate = Calendar.getInstance();
                                                SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
                                                final String saveCurrentDate = currentDate.format(calForDate.getTime());

                                                // menyimpan data senderID, receiverID, dan tanggal meeka mulai berteman
                                                friendsReference.child(online_user_id).child(lists_user_id).child("date").setValue(saveCurrentDate)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                friendsReference.child(lists_user_id).child(online_user_id).child("date").setValue(saveCurrentDate)
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid)
                                                                            {
                                                                                friendsReqReference.child(online_user_id).child(lists_user_id).removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if(task.isSuccessful())
                                                                                                {
                                                                                                    friendsReqReference.child(lists_user_id).child(online_user_id).removeValue()
                                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                @Override
                                                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                                                    if(task.isSuccessful())
                                                                                                                    {
                                                                                                                        Toast.makeText(getContext(), "Permintaan Pertemanan Diterima", Toast.LENGTH_SHORT).show();
                                                                                                                    }
                                                                                                                }
                                                                                                            });
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        });
                                                            }
                                                        });
                                            }
                                        });

                                        Button req_decline_btn = viewHolder.mView.findViewById(R.id.request_decline_btn);
                                        req_decline_btn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                friendsReqReference.child(online_user_id).child(lists_user_id).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful())
                                                                {
                                                                    friendsReqReference.child(lists_user_id).child(online_user_id).removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful())
                                                                                    {
                                                                                        Toast.makeText(getContext(), "Permintaan Pertemanan Ditolak", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                            else if (request_type.equals("sent"))
                            {
                                usersReference.child(lists_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                                        final String userThumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                                        final String userStatus = dataSnapshot.child("user_status").getValue().toString();

                                        viewHolder.setUserName(userName);
                                        viewHolder.setUserThumbImage(getContext(), userThumbImage);
                                        viewHolder.setUserStatus(userStatus);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                                Button req_sent_btn = viewHolder.mView.findViewById(R.id.request_accept_btn);
                                req_sent_btn.setText("Req Sent");

                                Button req_decline_btn = viewHolder.mView.findViewById(R.id.request_decline_btn);
                                req_decline_btn.setText("Batal");
                                req_decline_btn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        friendsReqReference.child(online_user_id).child(lists_user_id).removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful())
                                                        {
                                                            friendsReqReference.child(lists_user_id).child(online_user_id).removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful())
                                                                            {
                                                                                Toast.makeText(getContext(), "Permintaan Pertemanan Dibatalkan", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                });

                                // viewHolder.mView.findViewById(R.id.request_decline_btn).setVisibility(View.INVISIBLE);


                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }
        };
        mRequestsList.setAdapter(firebaseRecyclerAdapter);
    }



    public static class requestViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public requestViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setUserName(String userName)
        {
            TextView userNameDisplay = (TextView) mView.findViewById(R.id.request_profile_name);
            userNameDisplay.setText(userName);
        }

        public void setUserThumbImage(final Context ctx, final String userThumbImage)
        {
            final CircleImageView thumb_image = (CircleImageView) mView.findViewById(R.id.request_profile_image);

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

        public void setUserStatus(String userStatus)
        {
            TextView userStatusDisplay = (TextView) mView.findViewById(R.id.request_profile_status);
            userStatusDisplay.setText(userStatus);
        }
    }

}
