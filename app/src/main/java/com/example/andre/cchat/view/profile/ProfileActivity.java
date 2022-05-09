package com.example.andre.cchat.view.profile;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.andre.cchat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private Button sendFriendRequestButton;
    private Button declineFriendRequestButton;
    private TextView profileName;
    private TextView profileStatus;
    private ImageView profileImage;

    private DatabaseReference friendRequestReference;
    private DatabaseReference FriendsReference;
    private DatabaseReference notificationsReference;

    // variabe untuk ngecek apakah sudah menjadi teman / tidak
    private String CURRENT_STATE;

    private String senderUserId;
    private String receiverUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // reference database
        friendRequestReference = FirebaseDatabase.getInstance().getReference().child("Friend_Requests");
        friendRequestReference.keepSynced(true);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        senderUserId = mAuth.getCurrentUser().getUid();

        FriendsReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        FriendsReference.keepSynced(true);

        notificationsReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
        notificationsReference.keepSynced(true);

        DatabaseReference userProfileReference = FirebaseDatabase.getInstance().getReference().child("Users");

        // casting
        sendFriendRequestButton = (Button) findViewById(R.id.profile_visit_send_req_btn);
        declineFriendRequestButton = (Button) findViewById(R.id.profile_visit_decline_req_btn);
        profileName = (TextView) findViewById(R.id.profile_visit_username);
        profileStatus = (TextView) findViewById(R.id.profile_visit_user_status);
        profileImage = (ImageView) findViewById(R.id.profile_visit_user_image);

        CURRENT_STATE = "not_friends";

        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();

        userProfileReference.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            // retrieve data from firebase
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("user_name").getValue().toString();
                String status = dataSnapshot.child("user_status").getValue().toString();
                String image = dataSnapshot.child("user_image").getValue().toString();

                profileName.setText(name);
                profileStatus.setText(status);
                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_profile).into(profileImage);

                friendRequestReference.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // datasnapshop milik friendrequest
                        // // cek friend request
                        // it means, ada friend request dari user

                        if (dataSnapshot.hasChild(receiverUserId)) {
                            // mengambil data request type
                            String req_type = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();
                            // cancel friend request
                            if (req_type.equals("sent")) {
                                CURRENT_STATE = "request_sent";
                                sendFriendRequestButton.setText("Batalkan Permintaan Pertemanan");

                                declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                declineFriendRequestButton.setEnabled(false);
                            }
                            // accept friend request
                            else if (req_type.equals("received")) {
                                CURRENT_STATE = "request_received";
                                sendFriendRequestButton.setText("Terima Permintaan Pertemanan");

                                declineFriendRequestButton.setVisibility(View.VISIBLE);
                                declineFriendRequestButton.setEnabled(true);

                                declineFriendRequestButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        // menolak permintaan pertemanan
                                        declineFriendRequest();
                                    }
                                });
                            }
                        }

                        // jika friend request tidak ada, pada dasarnya 2 orang itu sudah teman, di else
                        // ini akan mengejerkan unfriend ( 2 orang berteman bisa unfriend )
                        else {
                            FriendsReference.child(senderUserId).
                                    addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            // cek 2 user berteman atau tidak dari sender_user_id, jika sender_user_id pnya anak receiver_user_id, berarti mereka berteman
                                            // jika iya maka bisa unfriend
                                            if (dataSnapshot.hasChild(receiverUserId)) {
                                                CURRENT_STATE = "friends";
                                                sendFriendRequestButton.setText("Batalkan Pertemanan");

                                                declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                                declineFriendRequestButton.setEnabled(false);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // button declinefriendrequest / tolak permintaan teman hanya akan muncul ketika user menerima permintaan teman dari user lain
        // selain itu button ini tidak akan muncul
        declineFriendRequestButton.setVisibility(View.INVISIBLE);
        declineFriendRequestButton.setEnabled(false);


        // dicek dlu, apakah senderID sama receiverID sama atau tidak
        // untuk mencegah user bisa add friend diri sendiri
        if (!senderUserId.equals(receiverUserId)) {
            sendFriendRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // di-disablle dlu, btuh validasi
                    sendFriendRequestButton.setEnabled(false);


                    if (CURRENT_STATE.equals("not_friends")) {
                        // mengirim permintaan teman ke user lain
                        sendFriendRequest();
                    }

                    if (CURRENT_STATE.equals("request_sent")) {
                        // membatalkan permintaan teman yang sudah dikirim
                        CancelFriendRequest();
                    }

                    if (CURRENT_STATE.equals("request_received")) {
                        // menierima permintaan teman dari user lain
                        AcceptFriendRequest();
                    }

                    if (CURRENT_STATE.equals("friends")) {
                        // meng-unfriend teman
                        UnFriendaFriend();
                    }
                }
            });
        } else {
            sendFriendRequestButton.setVisibility(View.INVISIBLE);
            declineFriendRequestButton.setVisibility(View.INVISIBLE);
        }

    }


    // mengirim permintaan teman ke user lain
    private void sendFriendRequest() {
        // node sender kirim ke receiver maka tipe requestnya menjadi sent
        // node receiver menerima permintaan dari sender maka tipe request menjadi receiver
        friendRequestReference.child(senderUserId).child(receiverUserId).
                child("request_type").setValue("sent").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                friendRequestReference.child(receiverUserId).child(senderUserId)
                        .child("request_type").setValue("received")
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                // menyimpan data notifikasi ke firebase
                                HashMap<String, String> notificationsData = new HashMap<>();
                                notificationsData.put("from", senderUserId);
                                notificationsData.put("type", "request");

                                notificationsReference.child(receiverUserId).push().setValue(notificationsData)
                                        .addOnCompleteListener(task11 -> {
                                            if (task11.isSuccessful()) {
                                                sendFriendRequestButton.setEnabled(true);
                                                CURRENT_STATE = "request_sent";
                                                sendFriendRequestButton.setText("Batalkan Permintaan Pertemanan");

                                                declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                                declineFriendRequestButton.setEnabled(false);
                                            }
                                        });
                            }
                        });
            }
        });
    }


    // membatalkan permintaan teman yang sudah dikirim
    private void CancelFriendRequest() {
        friendRequestReference.child(senderUserId).child(receiverUserId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendRequestReference.child(receiverUserId).child(senderUserId).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        sendFriendRequestButton.setEnabled(true);
                                        CURRENT_STATE = "not_friends";
                                        sendFriendRequestButton.setText("Kirim Permintaan Pertemanan");

                                        declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                        declineFriendRequestButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }


    // menierima permintaan teman dari user lain
    private void AcceptFriendRequest() {
        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy", Locale.getDefault());
        final String saveCurrentDate = currentDate.format(calForDate.getTime());

        // menyimpan data senderID, receiverID, dan tanggal meeka mulai berteman
        FriendsReference.child(senderUserId).child(receiverUserId).child("date").setValue(saveCurrentDate)
                .addOnSuccessListener(aVoid -> FriendsReference.child(receiverUserId).child(senderUserId).child("date").setValue(saveCurrentDate)
                        .addOnSuccessListener(aVoid1 -> friendRequestReference.child(senderUserId).child(receiverUserId).removeValue()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        friendRequestReference.child(receiverUserId).child(senderUserId).removeValue()
                                                .addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        sendFriendRequestButton.setEnabled(true);
                                                        CURRENT_STATE = "friends";
                                                        sendFriendRequestButton.setText("Batalkan Pertemanan");

                                                        declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                                        declineFriendRequestButton.setEnabled(false);
                                                    }
                                                });
                                    }
                                })));
    }


    // meng-unfriend teman
    private void UnFriendaFriend() {
        FriendsReference.child(senderUserId).child(receiverUserId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FriendsReference.child(receiverUserId).child(senderUserId).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        sendFriendRequestButton.setEnabled(true);
                                        CURRENT_STATE = "not_friends";
                                        sendFriendRequestButton.setText("Kirim Permintaan Pertemanan");

                                        declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                        declineFriendRequestButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }

    // menolak permintaan pertemanan
    private void declineFriendRequest() {
        friendRequestReference.child(senderUserId).child(receiverUserId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendRequestReference.child(receiverUserId).child(senderUserId).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        sendFriendRequestButton.setEnabled(true);
                                        CURRENT_STATE = "not_friends";
                                        sendFriendRequestButton.setText("Kirim Permintaan Pertemanan");

                                        declineFriendRequestButton.setVisibility(View.INVISIBLE);
                                        declineFriendRequestButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }


}
