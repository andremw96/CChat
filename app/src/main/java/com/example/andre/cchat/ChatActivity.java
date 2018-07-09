package com.example.andre.cchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.util.TimingLogger;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import de.frank_durr.ecdh_curve25519.ECDHCurve25519;
import de.hdodenhof.circleimageview.CircleImageView;
import okio.ByteString;

public class ChatActivity extends AppCompatActivity implements MessagesAdapter.ClickListener{
    public static final String TAG = ECDHCurve25519.class.getName();

    static {
        try {
            System.loadLibrary("ecdhcurve25519");
            Log.i(TAG, "Loaded ecdhcurve25519 library.");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static String messageReceiverId;
    private String messageReceiverName;

    private Toolbar chatToolbar;

    private TextView userNameTitle;
    private TextView userLastSeen;
    private TextView tempPublicKey;
    private TextView tempPrivateKey;
    private CircleImageView userChatProfileImage;

    private ImageButton sendMessageButton;
    private ImageButton selectImageButton;

    private EditText inputMessageText;

    private DatabaseReference rootRef;
    private DatabaseReference notificationsReference;
    private DatabaseReference usersReference;

    private FirebaseAuth mAuth;
    private static String messageSenderID;
    private static String messageSenderEmail;

    private RecyclerView userMessagesList;

    private final List<Messages> messageList = new ArrayList<>();

    private LinearLayoutManager linearLayoutManager;

    private MessagesAdapter messageAdapter;

    private static int Gallery_Pick = 1;
    private StorageReference MessageImageStorageRef;

    private ProgressDialog loadingBar;

    private String outputString;
    private String pesanTerenkripsi;
    private String AES = "AES/CBC/PKCS5Padding";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootRef = FirebaseDatabase.getInstance().getReference();

        notificationsReference = FirebaseDatabase.getInstance().getReference().child("Notifications_Message");
        notificationsReference.keepSynced(true);

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();
        messageSenderEmail = mAuth.getCurrentUser().getEmail();

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

        messageAdapter = new MessagesAdapter(messageList, this, ChatActivity.this);
        messageAdapter.setClickListener(this);

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        userMessagesList.setHasFixedSize(true);
        userMessagesList.setLayoutManager(linearLayoutManager);

        userMessagesList.setAdapter(messageAdapter);


        // setting username, dll di custom bar layout sesuai dgn profil orang yg dipilih user
        userNameTitle = (TextView) findViewById(R.id.custom_chat_profile_name);
        userLastSeen = (TextView) findViewById(R.id.custom_chat_user_last_seen);
        userChatProfileImage = (CircleImageView) findViewById(R.id.custom_chat_profile_image);
        tempPublicKey = (TextView) findViewById(R.id.textTempPublicKey);
        tempPrivateKey = (TextView) findViewById(R.id.textTempPrivateKey);

        tempPublicKey.setText("TEMPORARY PUBLIC KEY");

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
                    System.out.printf("last_seen "+last_seen);

                    if (last_seen != 0)
                    {
                        String lastSeenDisplayTime = getTime.getTimeAgo(last_seen, getApplicationContext()).toString();
                        System.out.printf("lastSeenDisplayTime "+lastSeenDisplayTime);
                        userLastSeen.setText(lastSeenDisplayTime);
                    }
                    else
                    {
                        userLastSeen.setText("Online");
                    }
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

        DatabaseReference getUserDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child(messageReceiverId);
        getUserDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String kunci_public_B_hexa = dataSnapshot.child("user_public_key").getValue().toString(); // kunci publik bentuknya HEXA

                if (kunci_public_B_hexa != null)
                {
                    tempPublicKey.setText(kunci_public_B_hexa);
                    // String tempPublicKeyString = tempPublicKey.getText().toString();
                }
                else
                {
                    Toast.makeText(ChatActivity.this, "User tersebut tidak punya kunci publik, tidak bisa enkripsi pesan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference getUserDataReference2 = FirebaseDatabase.getInstance().getReference().child("Users").child(messageSenderID);
        getUserDataReference2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String encryptedprivatekey = dataSnapshot.child("user_private_key").getValue().toString(); // kunci private bentuknya HEXA
                Log.d("encrypted private key", encryptedprivatekey);

                try {
                    String decryptedprivatekey = decryptPrivateKey(encryptedprivatekey, messageSenderEmail);

                    if (encryptedprivatekey != null)
                    {
                        tempPrivateKey.setText(decryptedprivatekey);
                        Log.d("decrypted private key", decryptedprivatekey);
                        // String tempPublicKeyString = tempPublicKey.getText().toString();
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "User tersebut tidak punya kunci publik, tidak bisa enkripsi pesan", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /*
    @Override
    protected void onStart() {
        super.onStart();

        Query chatQuery = rootRef.child("Messages").child(messageSenderID).child(messageReceiverId);

        final FirebaseRecyclerAdapter<Messages, MessageViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Messages, MessageViewHolder>
                (
                        Messages.class,
                        R.layout.messages_layout_users,
                        MessageViewHolder.class,
                        chatQuery
                )
        {
            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder, Messages model, int position)
            {
                viewHolder.setText(model.getMessage());
                String messageSenderID = mAuth.getCurrentUser().getUid();

                usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(messageSenderID);
                usersReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String userImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                        viewHolder.setUser_thumb_image(getApplicationContext(), userImage);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                pesanTerenkripsi = null;

                String fromUserID = messageSenderID;
                String fromMessageType = "text";


                Log.d("tipe pesan", fromMessageType);
                if(fromMessageType.equals("text"))
                {
                    //viewHolder.messagePicture.setVisibility(View.INVISIBLE);

                    // jika kita login sebagai kita, maka layout yg tampil adalah ini,,,
                    // which is background ijo, color black
                    if(fromUserID.equals(messageSenderID))
                    {
                        viewHolder.mView.setBackgroundResource(R.drawable.message_text_background_two);
                     //   viewHolder.setTextColor(Color.BLACK);
                     //   viewHolder.setGravity(Gravity.RIGHT);

                     //   viewHolder.

                        //  holder.userProfileImage.setVisibility(View.INVISIBLE);

                    }
                    else
                    {
                        viewHolder.mView.setBackgroundResource(R.drawable.message_text_background);
                       // viewHolder.mView.setTextColor(Color.WHITE);
                       // viewHolder.mView.setGravity(Gravity.LEFT);
                        // holder.userProfileImage.setVisibility(View.VISIBLE);
                    }

                   // viewHolder.messageText.setText(messages.getMessage());
                }
                else
                {
                   // viewHolder.messageText.setVisibility(View.INVISIBLE);
                   // viewHolder.messageText.setPadding(0,0,0,0);

                   // Picasso.with(viewHolder.userProfileImage.getContext()).load(messages.getMessage())
                   //         .placeholder(R.drawable.default_profile).into(viewHolder.messagePicture);
                }
            }
        };

        userMessagesList.setAdapter(firebaseRecyclerAdapter);
    }*/

/*
    // makiung view holder
    public static class MessageViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public ImageView messagePicture;

        public MessageViewHolder(View view)
        {
            super(view);

            mView = view;

            //messagePicture = (ImageView) view.findViewById(R.id.message_image_view);

        }

        public void setText(String message)
        {
            TextView messageText = (TextView) mView.findViewById(R.id.message_text);
            messageText.setText(message);
        }

        public void  setUser_thumb_image(final Context ctx, final String user_thumb_image)
        {
            final CircleImageView thumb_image = (CircleImageView) mView.findViewById(R.id.messages_profile_image);

            // load images offline
            Picasso.with(ctx).load(user_thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).
                    into(thumb_image, new Callback() {
                        @Override
                        // onsuccess akan melload picture offline
                        public void onSuccess() {

                        }

                        @Override
                        // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                        public void onError() {
                            Picasso.with(ctx).load(user_thumb_image).placeholder(R.drawable.default_profile).into(thumb_image);
                        }
                    });
        }


    }*/


    private void showUpdateDialog(){ // dialog enkripsi
        String messageText = inputMessageText.getText().toString();
        String privateKey = tempPrivateKey.getText().toString();

        LayoutInflater linf = LayoutInflater.from(this);
        final View inflator = linf.inflate(R.layout.update_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Enkripsi Pesan");
        alert.setView(inflator);

        final EditText editTextPesanEnkripsi = (EditText) inflator.findViewById(R.id.edit_pesan);
        final EditText editPwdUser = (EditText) inflator.findViewById(R.id.edit_kunci); //ini password user
        final EditText editPrivateKey = (EditText) inflator.findViewById(R.id.edit_kunci_private); //ini kunci private user
        final Button btnDekripsi = (Button) inflator.findViewById(R.id.btn_dekrip_pesan);
        final TextView txtPesanTerdekripsi = (TextView) inflator.findViewById(R.id.txtPesanTerdekripsi);

        btnDekripsi.setVisibility(View.GONE);
        txtPesanTerdekripsi.setVisibility(View.GONE);

        editTextPesanEnkripsi.setText(messageText);
        editPrivateKey.setText(privateKey);

        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                final String messageText = editTextPesanEnkripsi.getText().toString();
                final String inputPassword = editPwdUser.getText().toString(); //ini password user
                final String privateKey = editPrivateKey.getText().toString();//ini kunci private

                Log.d("messageText", messageText);
                Log.d("inputPassword", inputPassword);
                Log.d("private key", privateKey);

                if(TextUtils.isEmpty(messageText))
                {
                    Toast.makeText(ChatActivity.this, "Silahkan Isi Pesan Anda", Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(inputPassword))
                {
                    Toast.makeText(ChatActivity.this, "Silahkan Isi Password Anda", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        mAuth.signInWithEmailAndPassword(messageSenderEmail, inputPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task)
                        {
                            if(task.isSuccessful())
                            {
                                try {
                                    //start
                                    long lStartTime = System.nanoTime();

                                    //task
                                    outputString = (encrypt(messageText, privateKey)).trim();

                                    //end
                                    long lEndTime = System.nanoTime();

                                    //time elapsed
                                    long output = lEndTime - lStartTime;

                                    System.out.println("Waktu Enkripsi dalam milliseconds: " + output / 1000000);

                                    inputMessageText.setText(outputString);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                Toast.makeText(ChatActivity.this, "password tidak dikenali, silahkan Cek kembali password anda",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }});

                        inputMessageText.setEnabled(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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


    // buat gambar
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
            inputMessageText.setEnabled(true);

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

    public String decrypt(String outputString, String password) throws Exception
    {
        byte[] decodeValue = Base64.decode(outputString, Base64.DEFAULT);

        byte[] salt = Arrays.copyOfRange(decodeValue, 0, 16);
        byte[] iv = Arrays.copyOfRange(decodeValue, 16, 32);
        byte[] ct = Arrays.copyOfRange(decodeValue, 32, decodeValue.length);

        SecretKeySpec key = generateKey(password, salt);
        Cipher c = Cipher.getInstance(AES);

        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plaintext = c.doFinal(ct);
        // c.init(Cipher.DECRYPT_MODE, key);

        String x = new String(plaintext, "US-ASCII");
        Log.d("pesan terdekripsi", x);

        return new String(plaintext, "UTF-8");

        // byte[] decValue = c.doFinal(decodeValue);
        //  String decryptedValue = new String(decValue);
        //  return decryptedValue;
        // return null;
    }

    private SecretKeySpec generateKey(String password, byte[] salt) throws Exception
    {
        //String private_key_a_hex = "D09280F20000000000B478AA7D76370F000059000000430034E39EBF7D76374F";
       // String public_key_a_hex = "0D3028981B0CC6968F7DA1B316170B77E4AD362AFD31967C27BE74D8B45DBE7F";
      //  String private_key_b_hex = "18990FF50600000000B478AA00D59EBF000059000000430034E39EBF7D76374F";


        //byte[] kunci_private_a = hexStringToByteArray(private_key_a_hex);
        //byte[] kunci_public_a = hexStringToByteArray(public_key_a_hex);

       // byte[] kunci_private_b = hexStringToByteArray(private_key_b_hex);


       // System.out.println(Arrays.toString(kunci_private_a));
       // System.out.println(Arrays.toString(kunci_public_a));

       // System.out.println(Arrays.toString(kunci_private_b));
      //  System.out.println(Arrays.toString(kunci_public_B));

       // byte[] shared_secret_A = ECDHCurve25519.generate_shared_secret(kunci_private_a, kunci_public_B);
        //byte[] shared_secret_B = ECDHCurve25519.generate_shared_secret(kunci_private_b, kunci_public_a);

        // sharedsecreta != sharedsecretB

      //  String shared_secret_A_str = bytesToHex(shared_secret_A);
       // String shared_secret_B_str = bytesToHex(shared_secret_B);
       // Log.d("shared_secret_A_str", shared_secret_A_str);
       // Log.d("shared_secret_B_str", shared_secret_B_str);

        String public_key_user2 = tempPublicKey.getText().toString();
        Log.d("public_key_user2", public_key_user2);

        byte[] kunci_public_user2 = hexStringToByteArray(public_key_user2); // kunci public user 2

        byte[] kunci_private_user1 = hexStringToByteArray(password); // kunci privatenya user 1

        byte[] shared_secret = ECDHCurve25519.generate_shared_secret(kunci_private_user1, kunci_public_user2);

        String shared_secret_str = bytesToHex(shared_secret);

        Log.d("shared_secret_str", shared_secret_str);


        KeySpec spec = new PBEKeySpec(shared_secret_str.toCharArray(), salt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = f.generateSecret(spec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        return secretKeySpec;

    }

    public String decryptPrivateKey(String outputString, String password) throws Exception
    {
        byte[] decodeValue = Base64.decode(outputString, Base64.DEFAULT);

        byte[] salt = Arrays.copyOfRange(decodeValue, 0, 16);
        byte[] iv = Arrays.copyOfRange(decodeValue, 16, 32);
        byte[] ct = Arrays.copyOfRange(decodeValue, 32, decodeValue.length);

        SecretKeySpec key = generateKeyForPrivate(password, salt);
        Cipher c = Cipher.getInstance(AES);

        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plaintext = c.doFinal(ct);
        // c.init(Cipher.DECRYPT_MODE, key);

        String x = new String(plaintext, "US-ASCII");
        Log.d("pesan terdekripsi", x);

        return new String(plaintext, "UTF-8");

        // byte[] decValue = c.doFinal(decodeValue);
        //  String decryptedValue = new String(decValue);
        //  return decryptedValue;
        // return null;
    }

    private SecretKeySpec generateKeyForPrivate(String password, byte[] salt) throws Exception
    {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = f.generateSecret(spec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        return secretKeySpec;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Override
    public void itemClicked(View view, int position) {

        String TAG = "messagePosition";
        Log.d(TAG, "You clicked on "+position);

        Messages messages = messageList.get(position);
        pesanTerenkripsi = messages.getMessage();
        String privateKey = tempPrivateKey.getText().toString();
        System.out.println(pesanTerenkripsi);

        LayoutInflater linf = LayoutInflater.from(this);
        final View inflator = linf.inflate(R.layout.update_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Dekripsi Pesan");
        alert.setView(inflator);

        final EditText editTextPesanDekripsi = (EditText) inflator.findViewById(R.id.edit_pesan);
        final EditText editPwdUser = (EditText) inflator.findViewById(R.id.edit_kunci); //ini pass user
        final EditText editKunciDekripsi = (EditText) inflator.findViewById(R.id.edit_kunci_private); //ini kunci private
        final Button btnDekripsi = (Button) inflator.findViewById(R.id.btn_dekrip_pesan);
        final TextView txtPesanTerdekripsi = (TextView) inflator.findViewById(R.id.txtPesanTerdekripsi);

        btnDekripsi.setVisibility(View.VISIBLE);
        txtPesanTerdekripsi.setVisibility(View.VISIBLE);

        editTextPesanDekripsi.setText(pesanTerenkripsi);
        editTextPesanDekripsi.setEnabled(false);
        editKunciDekripsi.setText(privateKey);

        btnDekripsi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String messageText = editTextPesanDekripsi.getText().toString();
                final String inputPassword = editPwdUser.getText().toString();
                final String privateKey = editKunciDekripsi.getText().toString();

                Log.d("messageText", messageText);
                Log.d("inputPassword", inputPassword);
                Log.d("privateKey", privateKey);

                if(TextUtils.isEmpty(messageText))
                {
                    Toast.makeText(ChatActivity.this, "Silahkan Isi Pesan Anda", Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(inputPassword))
                {
                    Toast.makeText(ChatActivity.this, "Silahkan Isi Password Anda", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        mAuth.signInWithEmailAndPassword(messageSenderEmail, inputPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task)
                        {
                            if(task.isSuccessful())
                            {
                                try
                                {
                                    //start
                                    long lStartTime = System.nanoTime();

                                    //task
                                    outputString = decrypt(messageText, privateKey);
                                    txtPesanTerdekripsi.setText(outputString);

                                    //end
                                    long lEndTime = System.nanoTime();

                                    //time elapsed
                                    long output = lEndTime - lStartTime;

                                    System.out.println("Waktu Dekripsi dalam milliseconds: " + output / 1000000);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                Toast.makeText(ChatActivity.this, "password tidak dikenali, silahkan Cek kembali password anda",
                                        Toast.LENGTH_SHORT).show();
                            }

                            loadingBar.dismiss();
                        }
                });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alert.show();
    }
}
