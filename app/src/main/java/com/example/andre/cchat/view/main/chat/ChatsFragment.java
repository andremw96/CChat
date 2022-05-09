package com.example.andre.cchat.view.main.chat;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.andre.cchat.R;
import com.example.andre.cchat.model.Conv;
import com.example.andre.cchat.view.main.chat.adapter.ConvViewHolder;
import com.example.andre.cchat.view.main.chat.adapter.ConversationAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private RecyclerView myChatsList;

    private DatabaseReference usersReference;
    private DatabaseReference messagesReference;
    private DatabaseReference convReference;

    String onlineUserId;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // linking mMainView ke recyclerView
        myChatsList = (RecyclerView) view.findViewById(R.id.chats_fragment_list);

        // casting mAuth sehingga bisa memakai getUid
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();

        // get the friend list for the online user
        DatabaseReference friendsReference = FirebaseDatabase.getInstance().getReference().child("Friends").child(onlineUserId);

        convReference = FirebaseDatabase.getInstance().getReference().child("Chat").child(onlineUserId);
        convReference.keepSynced(true);

        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        usersReference.keepSynced(true);

        messagesReference = FirebaseDatabase.getInstance().getReference().child("Messages").child(onlineUserId);

        // set the layout manager
        myChatsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myChatsList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void onStart() {
        super.onStart();

        Query convQuery = convReference.orderByChild("timestamp");
        FirebaseRecyclerAdapter<Conv, ConvViewHolder> firebaseRecyclerAdapter = new ConversationAdapter(
                convQuery,
                messagesReference,
                usersReference
        );

        myChatsList.setAdapter(firebaseRecyclerAdapter);
    }
}
