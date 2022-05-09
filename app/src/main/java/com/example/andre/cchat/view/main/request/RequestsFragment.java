package com.example.andre.cchat.view.main.request;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.andre.cchat.R;
import com.example.andre.cchat.model.Requests;
import com.example.andre.cchat.view.main.request.adapter.RequestsAdapter;
import com.example.andre.cchat.view.main.request.adapter.RequestsViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView mRequestsList;

    private DatabaseReference friendRequestsReference;
    private DatabaseReference usersReference;

    private DatabaseReference friendsReference;

    private String onlineUserId;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();

        friendRequestsReference = FirebaseDatabase.getInstance().getReference().child("Friend_Requests").child(onlineUserId);

        // digunakan untuk mengambil data2 user
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");

        friendsReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        DatabaseReference friendsReqReference = FirebaseDatabase.getInstance().getReference().child("Friend_Requests");

        mRequestsList = (RecyclerView) view.findViewById(R.id.requests_list);

        mRequestsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        // show the new friend request at the top
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mRequestsList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Requests, RequestsViewHolder> firebaseRecyclerAdapter = new RequestsAdapter(
                friendRequestsReference,
                usersReference,
                friendsReference,
                onlineUserId
        );

        mRequestsList.setAdapter(firebaseRecyclerAdapter);
    }
}
