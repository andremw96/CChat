package com.example.andre.cchat.view.main.request.adapter

import android.widget.Button
import android.widget.Toast
import com.example.andre.cchat.R
import com.example.andre.cchat.model.Requests
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class RequestsAdapter(
    private val friendsReqReference: DatabaseReference,
    private val usersReference: DatabaseReference,
    private val friendsReference: DatabaseReference,
    private val onlineUserId: String
) : FirebaseRecyclerAdapter<Requests, RequestsViewHolder>(
    Requests::class.java,
    R.layout.friend_request_all_users_layout,
    RequestsViewHolder::class.java,
    friendsReqReference
) {
    override fun populateViewHolder(
        viewHolder: RequestsViewHolder,
        model: Requests?,
        position: Int
    ) {
        val listsUserId = getRef(position).key
        val getTypeRequestRef = getRef(position).child("request_type").ref
        val context = viewHolder.mView.context

        getTypeRequestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val requestType = dataSnapshot.value.toString()

                    // jika user yg sedang login itu, menerima request dari orang lain
                    // maka data orang yg mengirim request akan muncul
                    if (requestType == "received") {
                        usersReference.child(listsUserId)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val userName = dataSnapshot.child("user_name").value.toString()
                                    val userThumbImage =
                                        dataSnapshot.child("user_thumb_image").value.toString()
                                    val userStatus =
                                        dataSnapshot.child("user_status").value.toString()
                                    viewHolder.setUserName(userName)
                                    viewHolder.setUserThumbImage(context, userThumbImage)
                                    viewHolder.setUserStatus(userStatus)
                                    val reqSentBtn: Button =
                                        viewHolder.mView.findViewById(R.id.request_accept_btn)
                                    reqSentBtn.setOnClickListener {
                                        val calForDate = Calendar.getInstance()
                                        val currentDate =
                                            SimpleDateFormat("dd-MMMM-yyyy", Locale.getDefault())
                                        val saveCurrentDate = currentDate.format(calForDate.time)

                                        // menyimpan data senderID, receiverID, dan tanggal meeka mulai berteman
                                        friendsReference.child(onlineUserId).child(listsUserId)
                                            .child("date").setValue(saveCurrentDate)
                                            .addOnSuccessListener {
                                                friendsReference.child(listsUserId)
                                                    .child(onlineUserId)
                                                    .child("date").setValue(saveCurrentDate)
                                                    .addOnSuccessListener {
                                                        friendsReqReference.child(onlineUserId)
                                                            .child(listsUserId).removeValue()
                                                            .addOnCompleteListener { task ->
                                                                if (task.isSuccessful) {
                                                                    friendsReqReference.child(listsUserId)
                                                                        .child(onlineUserId)
                                                                        .removeValue()
                                                                        .addOnCompleteListener { task ->
                                                                            if (task.isSuccessful) {
                                                                                Toast.makeText(context, "Permintaan Pertemanan Diterima", Toast.LENGTH_SHORT).show()

                                                                            }
                                                                        }
                                                                }
                                                            }
                                                    }
                                            }
                                    }
                                    val reqDeclineBtn: Button = viewHolder.mView.findViewById(R.id.request_decline_btn)
                                    reqDeclineBtn.setOnClickListener {
                                        friendsReqReference.child(onlineUserId)
                                            .child(listsUserId)
                                            .removeValue()
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    friendsReqReference.child(listsUserId)
                                                        .child(onlineUserId).removeValue()
                                                        .addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Permintaan Pertemanan Ditolak",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                }
                                            }
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                    } else if (requestType == "sent") {
                        usersReference.child(listsUserId)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val userName = dataSnapshot.child("user_name").value.toString()
                                    val userThumbImage =
                                        dataSnapshot.child("user_thumb_image").value.toString()
                                    val userStatus =
                                        dataSnapshot.child("user_status").value.toString()
                                    viewHolder.setUserName(userName)
                                    viewHolder.setUserThumbImage(context, userThumbImage)
                                    viewHolder.setUserStatus(userStatus)
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        val reqSentBtn: Button =
                            viewHolder.mView.findViewById(R.id.request_accept_btn)
                        reqSentBtn.text = "Req Sent"
                        val reqDeclineBtn: Button =
                            viewHolder.mView.findViewById(R.id.request_decline_btn)
                        reqDeclineBtn.text = "Batal"
                        reqDeclineBtn.setOnClickListener {
                            friendsReqReference.child(onlineUserId).child(listsUserId)
                                .removeValue()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        friendsReqReference.child(listsUserId)
                                            .child(onlineUserId)
                                            .removeValue()
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(
                                                        context,
                                                        "Permintaan Pertemanan Dibatalkan",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    }
                                }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


    }
}
