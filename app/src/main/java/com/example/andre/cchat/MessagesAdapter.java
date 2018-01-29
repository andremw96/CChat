package com.example.andre.cchat;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Wijaya_PC on 26-Jan-18.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>
{
    private List<Messages> userMessagesList;

    private FirebaseAuth mAuth;

    private DatabaseReference usersReference;

    public MessagesAdapter(List<Messages> userMessagesList)
    {
        this.userMessagesList = userMessagesList;
    }



    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View V = LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_layout_users, parent, false);

        mAuth = FirebaseAuth.getInstance();

        return new MessageViewHolder(V);
    }



    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position)
    {
        String messageSenderID = mAuth.getCurrentUser().getUid();
        Messages messages = userMessagesList.get(position);

        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);
        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userName = dataSnapshot.child("user_name").getValue().toString();
                String userImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                Picasso.with(holder.userProfileImage.getContext()).load(userImage).placeholder(R.drawable.default_profile)
                        .into(holder.userProfileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Log.d("tipe pesan", fromMessageType);
        if(fromMessageType.equals("text"))
        {
            holder.messagePicture.setVisibility(View.INVISIBLE);

            // jika kita login sebagai kita, maka layout yg tampil adalah ini,,,
            // which is background ijo, color black
            if(fromUserID.equals(messageSenderID))
            {
                holder.messageText.setBackgroundResource(R.drawable.message_text_background_two);
                holder.messageText.setTextColor(Color.BLACK);
                holder.messageText.setGravity(Gravity.RIGHT);

             //  holder.userProfileImage.setVisibility(View.INVISIBLE);

            }
            else
            {
                holder.messageText.setBackgroundResource(R.drawable.message_text_background);
                holder.messageText.setTextColor(Color.WHITE);
                holder.messageText.setGravity(Gravity.LEFT);

               // holder.userProfileImage.setVisibility(View.VISIBLE);
            }

            holder.messageText.setText(messages.getMessage());
        }
        else
        {
            holder.messageText.setVisibility(View.INVISIBLE);
            holder.messageText.setPadding(0,0,0,0);

            Picasso.with(holder.userProfileImage.getContext()).load(messages.getMessage())
                    .placeholder(R.drawable.default_profile).into(holder.messagePicture);
        }
    }



    @Override
    public int getItemCount()
    {
        return userMessagesList.size();
    }



    // makiung view holder
    public class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView messageText;
        public CircleImageView userProfileImage;
        public ImageView messagePicture;

        public MessageViewHolder(View view)
        {
            super(view);

            messageText = (TextView) view.findViewById(R.id.message_text);

            userProfileImage = (CircleImageView) view.findViewById(R.id.messages_profile_image);

            messagePicture = (ImageView) view.findViewById(R.id.message_image_view);

        }
    }
}
