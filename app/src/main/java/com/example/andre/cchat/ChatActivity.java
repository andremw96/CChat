package com.example.andre.cchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverId;
    private String messageReceiverName;

    private Toolbar chatToolbar;

    private TextView userNameTitle;
    private TextView userLastSeen;
    private CircleImageView userChatProfileImage;

    private ImageButton sendMessageButton;
    private ImageButton selectImageButton;

    private EditText inputMessageText;

    private DatabaseReference rootRef;
    private DatabaseReference notificationsReference;

    private FirebaseAuth mAuth;
    private String messageSenderID;

    private RecyclerView userMessagesList;

    private final List<Messages> messageList = new ArrayList<>();

    private LinearLayoutManager linearLayoutManager;

    private MessagesAdapter messageAdapter;

    private static int Gallery_Pick = 1;
    private StorageReference MessageImageStorageRef;

    private ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootRef = FirebaseDatabase.getInstance().getReference();

        notificationsReference = FirebaseDatabase.getInstance().getReference().child("Notifications_Message");
        notificationsReference.keepSynced(true);

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();

        if ( (getIntent().getExtras().get("visit_user_id").toString() != null) && (getIntent().getExtras().get("user_name").toString() != null)) {
            messageReceiverId = getIntent().getExtras().get("visit_user_id").toString();
            messageReceiverName = getIntent().getExtras().get("user_name").toString();
        }

        MessageImageStorageRef = FirebaseStorage.getInstance().getReference().child("Messages_Pictures");

        loadingBar = new ProgressDialog(this);

        chatToolbar = (Toolbar) findViewById(R.id.chat_bar_layout);
        setSupportActionBar(chatToolbar);

        ActionBar actionBar = getSupportActionBar();
        // add back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View action_bar_view = layoutInflater.inflate(R.layout.chat_custom_bar, null);

        // connect action bar to custom view
        actionBar.setCustomView(action_bar_view);

        // casting activitychat_xml atribute
        sendMessageButton = (ImageButton) findViewById(R.id.chat_send_message_btn);
        selectImageButton = (ImageButton) findViewById(R.id.chat_select_image_btn);
        inputMessageText = (EditText) findViewById(R.id.chat_input_message);
        userMessagesList = (RecyclerView) findViewById(R.id.chat_message_list_user);

        messageAdapter = new MessagesAdapter(messageList);

        linearLayoutManager = new LinearLayoutManager(this);

        userMessagesList.setHasFixedSize(true);

        userMessagesList.setLayoutManager(linearLayoutManager);

        userMessagesList.setAdapter(messageAdapter);


        // setting username, dll di custom bar layout sesuai dgn profil orang yg dipilih user
        userNameTitle = (TextView) findViewById(R.id.custom_chat_profile_name);
        userLastSeen = (TextView) findViewById(R.id.custom_chat_user_last_seen);
        userChatProfileImage = (CircleImageView) findViewById(R.id.custom_chat_profile_image);

        // ngambil nama dan ditampilin ke texxtview
        userNameTitle.setText(messageReceiverName);

        rootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @Override
            // retrieve user image & user last seen
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                final String online = dataSnapshot.child("online").getValue().toString();
                final String thumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                if(online.equals("true"))
                {
                    userLastSeen.setText("Online");
                }
                else
                {
                    LastSeenTime getTime = new LastSeenTime();

                    // convert data to long
                    long last_seen = Long.parseLong(online);

                    String lastSeenDisplayTime = getTime.getTimeAgo(last_seen, getApplicationContext()).toString();

                    userLastSeen.setText(lastSeenDisplayTime);
                }

                // load images offline
                Picasso.with(ChatActivity.this).load(thumbImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).
                        into(userChatProfileImage, new Callback() {
                            @Override
                            // onsuccess akan melload picture offline
                            public void onSuccess() {

                            }

                            @Override
                            // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                            public void onError() {
                                Picasso.with(ChatActivity.this).load(thumbImage).placeholder(R.drawable.default_profile).into(userChatProfileImage);
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

       /* rootRef.child("Chat").child(messageSenderID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!dataSnapshot.hasChild(messageReceiverId)){

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + messageSenderID + "/" + messageReceiverId, chatAddMap);
                    chatUserMap.put("Chat/" + messageReceiverId + "/" + messageSenderID, chatAddMap);

                    rootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError != null){

                                Log.d("CHAT_LOG", databaseError.getMessage().toString());

                            }

                        }
                    });

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/


        sendMessageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                kirimPesan();
            }
        });


        // open gallery
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });

        FetchMessages();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Gallery_Pick && resultCode == RESULT_OK && data != null)
        {
            loadingBar.setTitle("Mengirim Gambar");
            loadingBar.setMessage("Mohon Tunggu, ketika gambar anda sedang dikirim ...");
            loadingBar.show();

            Uri ImageUri = data.getData();

            final String message_sender_ref = "Messages/" + messageSenderID + "/" + messageReceiverId;
            final String message_receiver_ref = "Messages/" + messageReceiverId + "/" + messageSenderID;

            DatabaseReference message_key = rootRef.child("Messages").child(messageSenderID).child(messageReceiverId).push();
            final String message_push_id = message_key.getKey();

            StorageReference filePath = MessageImageStorageRef.child(message_push_id + ".jpg");
            filePath.putFile(ImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task)
                {
                    if(task.isSuccessful())
                    {
                        // get url of the image from firebase storage for image
                        final String downloadUrl = task.getResult().getDownloadUrl().toString();


                        // store to database
                        Map messageTextBody = new HashMap();
                        messageTextBody.put("message", downloadUrl);
                        messageTextBody.put("seen", false);
                        messageTextBody.put("type", "image");
                        messageTextBody.put("time", ServerValue.TIMESTAMP);
                        messageTextBody.put("from", messageSenderID);

                        Map messageBodyDetails = new HashMap();
                        messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);
                        messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

                        rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child("seen").setValue(true);
                        rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child("timestamp").setValue(ServerValue.TIMESTAMP);

                        rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child("seen").setValue(false);
                        rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child("timestamp").setValue(ServerValue.TIMESTAMP);

                        rootRef.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null)
                                {
                                    Log.d("Chat_Log", databaseError.getMessage().toString());
                                }

                                inputMessageText.setText("");

                                loadingBar.dismiss();
                            }
                        });

                        Toast.makeText(ChatActivity.this, "Gambar telah terkirim", Toast.LENGTH_SHORT).show();

                        loadingBar.dismiss();
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "Gambar gagal terkirim, Coba Lagi", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }



    private void FetchMessages()
    {
        //DatabaseReference messageRef = rootRef.child("Messages").child(messageSenderID).child(messageReceiverId);

        // pertama akan load 10 pesan
        // terus jika di refresh oleh user maka mcurrent page berubah jadi 2 di oncreate, maka pesan jadi 20
       // Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        rootRef.child("Messages").child(messageSenderID).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s)
                    {
                        Messages messages = dataSnapshot.getValue(Messages.class);

                        messageList.add(messages);
                        messageAdapter.notifyDataSetChanged();

                        // ketika fetchmessage, halaman page lgsg ke paling bawah alias pesan terakhir
                        userMessagesList.scrollToPosition(messageList.size() - 1);

                       // mRefreshLayoutList.setRefreshing(false);
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
    }

    private void kirimPesan()
    {
        String messageText = inputMessageText.getText().toString();

        if(TextUtils.isEmpty(messageText))
        {
            Toast.makeText(ChatActivity.this, "Silahkan Isi Pesan Anda", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // membuat reference ke database ( tabel )
            String message_sender_ref = "Messages/" + messageSenderID + "/" + messageReceiverId;
            String message_receiver_ref = "Messages/" + messageReceiverId + "/" + messageSenderID;

            // membuat unique ID dari message
            DatabaseReference message_key = rootRef.child("Messages").child(messageSenderID).child(messageReceiverId).push();
            String message_push_id = message_key.getKey();

            // store to database
            Map messageTextBody = new HashMap();
            messageTextBody.put("message", messageText);
            messageTextBody.put("seen", false);
            messageTextBody.put("type", "text");
            messageTextBody.put("time", ServerValue.TIMESTAMP);
            messageTextBody.put("from", messageSenderID);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);
            messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

            rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child("seen").setValue(true);
            rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child("timestamp").setValue(ServerValue.TIMESTAMP);

            rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child("seen").setValue(false);
            rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child("timestamp").setValue(ServerValue.TIMESTAMP);

            // menyimpan data notifikasi ke firebase
            HashMap<String, String> notificationsData = new HashMap<String, String>();
            notificationsData.put("from", messageSenderID);
            notificationsData.put("type", "sent_message");
            notificationsData.put("isi_pesan", messageText);
            notificationsReference.child(messageReceiverId).push().setValue(notificationsData);

            inputMessageText.setText("");

            rootRef.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference)
                {
                    if(databaseError != null)
                    {

                        Log.d("Chat_Log", databaseError.getMessage().toString());

                    }

                }
            });
        }
    }
}
