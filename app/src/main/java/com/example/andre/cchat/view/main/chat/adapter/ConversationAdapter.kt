package com.example.andre.cchat.view.main.chat.adapter

import android.content.Intent
import com.example.andre.cchat.R
import com.example.andre.cchat.model.Conv
import com.example.andre.cchat.view.chat.ChatActivity
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*

class ConversationAdapter(
    convQuery: Query,
    private val messageReference: DatabaseReference,
    private val userReference: DatabaseReference,
) : FirebaseRecyclerAdapter<Conv, ConvViewHolder>(
    Conv::class.java,
    R.layout.all_users_display_layout,
    ConvViewHolder::class.java,
    convQuery
) {
    override fun populateViewHolder(viewHolder: ConvViewHolder, model: Conv?, position: Int) {
        val listUserId = getRef(position).key

        val lastMessageQuery: Query = messageReference.child(listUserId).limitToLast(1)
        val context = viewHolder.itemView.context

        lastMessageQuery.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val data = dataSnapshot.child("message").value.toString()
                viewHolder.setMessage(data, model!!.isSeen)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })

        userReference.child(listUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userName = dataSnapshot.child("user_name").value.toString()
                val userThumbImage = dataSnapshot.child("user_thumb_image").value.toString()
                //   String userStatus = dataSnapshot.child("user_status").getValue().toString();
                if (dataSnapshot.hasChild("online")) {
                    viewHolder.setUserOnline(dataSnapshot.child("online").value.toString())
                }
                viewHolder.setUserName(userName)
                viewHolder.setUserThumbImage(context, userThumbImage)
                // viewHolder.setUserStatus(userStatus);
                viewHolder.mView.setOnClickListener { // biar gk eror, dibuat validasi
                    // misal ada user tidak online 6 bulan, dan ketika dia buka app mychat bisa eror
                    // maka dikasi validasi
                    if (dataSnapshot.child("online").exists()) {
                        val chatIntent = Intent(context, ChatActivity::class.java)
                        chatIntent.putExtra("visit_user_id", listUserId)
                        chatIntent.putExtra("user_name", userName)
                        context.startActivity(chatIntent)
                    } else {
                        userReference.child(listUserId).child("online")
                            .setValue(ServerValue.TIMESTAMP)
                            .addOnSuccessListener {
                                val chatIntent = Intent(context, ChatActivity::class.java)
                                chatIntent.putExtra("visit_user_id", listUserId)
                                chatIntent.putExtra("user_name", userName)
                                context.startActivity(chatIntent)
                            }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}
