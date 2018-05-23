package com.example.andre.cchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.Spinner;
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

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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

    String outputString;
    String AES = "AES/CBC/PKCS5Padding";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootRef = FirebaseDatabase.getInstance().getReference();

        notificationsReference = FirebaseDatabase.getInstance().getReference().child("Notifications_Message");
        notificationsReference.keepSynced(true);

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();

        if ( (getIntent().getExtras().get("visit_user_id").toString() != null) && (getIntent().getExtras().get("user_name").toString() != null) ) {
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

        sendMessageButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                showUpdateDialog();

                return false;
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

    private void showUpdateDialog(){
        String messageText = inputMessageText.getText().toString();

        LayoutInflater linf = LayoutInflater.from(this);
        final View inflator = linf.inflate(R.layout.update_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Enkripsi Pesan");
        alert.setView(inflator);

        final EditText editTextPesanEnkripsi = (EditText) inflator.findViewById(R.id.edit_pesan_enkripsi);
        final EditText editKunciEnkripsi = (EditText) inflator.findViewById(R.id.edit_kunci_enkripsi);

        editTextPesanEnkripsi.setText(messageText);

        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String messageText = editTextPesanEnkripsi.getText().toString();
                String inputPassword = editKunciEnkripsi.getText().toString();

                Log.d("messageText", messageText);
                Log.d("inputPassword", inputPassword);

                if(TextUtils.isEmpty(messageText))
                {
                    Toast.makeText(ChatActivity.this, "Silahkan Isi Pesan Anda", Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(inputPassword))
                {
                    Toast.makeText(ChatActivity.this, "Silahkan Isi Kunci Anda", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        outputString = encrypt(messageText, inputPassword);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    inputMessageText.setText(outputString);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alert.show();
    };


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

    private String encrypt(String Data, String password) throws Exception
    {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        SecretKeySpec key = generateKey(password, salt);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = c.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] encryptedText = c.doFinal(Data.getBytes("UTF-8"));

        // concatenate salt + iv + ciphertext
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt);
        outputStream.write(iv);
        outputStream.write(encryptedText);


        //  byte[] encVal = c.doFinal(Data.getBytes());
        String encryptedValue = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

        String x = new String(outputStream.toByteArray(), "US-ASCII");
        Log.d("pesan terenkripsi", x);

        return encryptedValue;
    }

    private SecretKeySpec generateKey(String password, byte[] salt) throws Exception
    {
        byte[] bytes = password.getBytes("UTF-8");

       /* final MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES"); */

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = f.generateSecret(spec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        String xsecretkeyspec = Base64.encodeToString(bytes, Base64.DEFAULT);
        Log.d("kunci stlh UTF-8", xsecretkeyspec);

        String xxsecretkeyspec = Base64.encodeToString(key, Base64.DEFAULT);
        Log.d("kunci enkripsi base64", xxsecretkeyspec);

        String s = new String(bytes, "US-ASCII");
        Log.d("kunci stlh UTF-8 ASCII", s);

        String x = new String(key, "US-ASCII");
        Log.d("kunci enkripsi ASCII", x);

        return secretKeySpec;

    }
}
