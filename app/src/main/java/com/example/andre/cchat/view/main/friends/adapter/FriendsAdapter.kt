package com.example.andre.cchat.view.main.friends.adapter

import android.app.AlertDialog
import android.content.Intent
import com.example.andre.cchat.R
import com.example.andre.cchat.model.Friends
import com.example.andre.cchat.view.chat.ChatActivity
import com.example.andre.cchat.view.profile.ProfileActivity
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*

class FriendsAdapter(
    friendsReference: DatabaseReference,
    private val usersReference: DatabaseReference,
) : FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(
    Friends::class.java,
    R.layout.all_users_display_layout,
    FriendsViewHolder::class.java,
    friendsReference
) {
    override fun populateViewHolder(
        viewHolder: FriendsViewHolder?,
        model: Friends?,
        position: Int
    ) {
        viewHolder!!.setDate(model!!.getDate())

        val listUserId = getRef(position).key
        val context = viewHolder.mView.context

        usersReference.child(listUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userName = dataSnapshot.child("user_name").value.toString()
                val userThumbImage = dataSnapshot.child("user_thumb_image").value.toString()
                if (dataSnapshot.hasChild("online")) {
                    viewHolder.setUserOnline(dataSnapshot.child("online").value.toString())
                }
                viewHolder.setUserName(userName)
                viewHolder.setUserThumbImage(context, userThumbImage)
                viewHolder.mView.setOnClickListener { // jika user ngeklik 1 user di friend fragment, maka akan muncul dialog box dgn 2 option
                    // pertama setting optionsnya dulu
                    val options = arrayOf<CharSequence>(
                        "Lihat Profil $userName",
                        "Mulai Percakapan"
                    )

                    // membuat alert dialog
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Select Options")
                    builder.setItems(
                        options
                    ) { _, position ->
                        // position 0 itu berarti user profile sesuai optionsp[ diatas
                        if (position == 0) {
                            val profileIntent = Intent(context, ProfileActivity::class.java)
                            profileIntent.putExtra("visit_user_id", listUserId)
                            profileIntent.putExtra("user_name", userName)
                            context.startActivity(profileIntent)
                        }
                        if (position == 1) {
                            // biar gk eror, dibuat validasi
                            // misal ada user tidak online 6 bulan, dan ketika dia buka app mychat bisa eror
                            // maka dikasi validasi
                            if (dataSnapshot.child("online").exists()) {
                                val chatIntent = Intent(context, ChatActivity::class.java)
                                chatIntent.putExtra("visit_user_id", listUserId)
                                chatIntent.putExtra("user_name", userName)
                                context.startActivity(chatIntent)
                            } else {
                                usersReference.child(listUserId).child("online")
                                    .setValue(ServerValue.TIMESTAMP).addOnSuccessListener {
                                        val chatIntent =
                                            Intent(context, ChatActivity::class.java)
                                        chatIntent.putExtra("visit_user_id", listUserId)
                                        chatIntent.putExtra("user_name", userName)
                                        context.startActivity(chatIntent)
                                    }
                            }
                        }
                    }
                    builder.show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}
