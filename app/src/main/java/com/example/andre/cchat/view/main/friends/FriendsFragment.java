package com.example.andre.cchat.view.main.friends;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.andre.cchat.R;
import com.example.andre.cchat.model.Friends;
import com.example.andre.cchat.view.main.friends.adapter.FriendsAdapter;
import com.example.andre.cchat.view.main.friends.adapter.FriendsViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {
    private RecyclerView mFriendsList;

    private DatabaseReference friendsReference;
    private DatabaseReference usersReference;

    String onlineUserId;

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // linking mMainView ke recyclerView
        mFriendsList = (RecyclerView) view.findViewById(R.id.friends_list);

        // casting mAuth sehingga bisa memakai getUid
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();

        friendsReference = FirebaseDatabase.getInstance().getReference().child("Friends").child(onlineUserId);
        friendsReference.keepSynced(true);

        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        usersReference.keepSynced(true);

        // kita harus set layout manager untuk recycler view
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> firebaseRecyclerAdapter = new FriendsAdapter(
                friendsReference,
                usersReference
        );

        mFriendsList.setAdapter(firebaseRecyclerAdapter);
    }
}
