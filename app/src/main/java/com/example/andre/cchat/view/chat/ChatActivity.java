package com.example.andre.cchat.view.chat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.andre.cchat.R;
import com.example.andre.cchat.model.LastSeenTime;
import com.example.andre.cchat.model.Messages;
import com.example.andre.cchat.view.chat.adapter.MessagesAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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

public class ChatActivity extends AppCompatActivity implements MessagesAdapter.ClickListener {
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

    private TextView userLastSeen;
    private TextView tempPublicKey;
    private TextView tempPrivateKey;
    private CircleImageView userChatProfileImage;

    private EditText inputMessageText;

    private DatabaseReference rootRef;
    private DatabaseReference notificationsReference;

    private FirebaseAuth mAuth;
    private String messageSenderID;
    private String messageSenderEmail;

    private RecyclerView userMessagesList;

    private final List<Messages> messageList = new ArrayList<>();

    private MessagesAdapter messageAdapter;

    private static final int GALLERY_PICK = 1;
    private StorageReference messageImageStorageRef;

    private ProgressDialog loadingBar;

    private String outputString;
    private static final String AES_CBC_PKCS_5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String USERS_STRING = "Users";
    private static final String MESSAGES_STRING = "Messages";
    private static final String TIMESTAMP_STRING = "timestamp";
    private static final String SEEN_STRING = "seen";

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

        messageReceiverId = getIntent().getExtras().get("visit_user_id").toString();
        String messageReceiverName = getIntent().getExtras().get("user_name").toString();

        messageImageStorageRef = FirebaseStorage.getInstance().getReference().child("Messages_Pictures");

        loadingBar = new ProgressDialog(this);

        Toolbar chatToolbar = (Toolbar) findViewById(R.id.chat_bar_layout);
        setSupportActionBar(chatToolbar);

        ActionBar actionBar = getSupportActionBar();
        // add back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View actionBarView = layoutInflater.inflate(R.layout.chat_custom_bar, null);

        // connect action bar to custom view
        actionBar.setCustomView(actionBarView);

        // casting activitychat_xml atribute
        ImageButton sendMessageButton = (ImageButton) findViewById(R.id.chat_send_message_btn);
        ImageButton selectImageButton = (ImageButton) findViewById(R.id.chat_select_image_btn);
        inputMessageText = (EditText) findViewById(R.id.chat_input_message);
        userMessagesList = (RecyclerView) findViewById(R.id.chat_message_list_user);

        messageAdapter = new MessagesAdapter(messageList, this, ChatActivity.this);
        messageAdapter.setClickListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        userMessagesList.setHasFixedSize(true);
        userMessagesList.setLayoutManager(linearLayoutManager);

        userMessagesList.setAdapter(messageAdapter);

        // setting username, dll di custom bar layout sesuai dgn profil orang yg dipilih user
        TextView userNameTitle = (TextView) findViewById(R.id.custom_chat_profile_name);
        userLastSeen = (TextView) findViewById(R.id.custom_chat_user_last_seen);
        userChatProfileImage = (CircleImageView) findViewById(R.id.custom_chat_profile_image);
        tempPublicKey = (TextView) findViewById(R.id.textTempPublicKey);
        tempPrivateKey = (TextView) findViewById(R.id.textTempPrivateKey);

        tempPublicKey.setText("TEMPORARY PUBLIC KEY");

        // ngambil nama dan ditampilin ke texxtview
        userNameTitle.setText(messageReceiverName);

        rootRef.child(USERS_STRING).child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @Override
            // retrieve user image & user last seen
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String online = dataSnapshot.child("online").getValue().toString();
                final String thumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                if (online.equals("true")) {
                    userLastSeen.setText("Online");
                } else {
                    // convert data to long
                    long lastSeen = Long.parseLong(online);

                    if (lastSeen != 0) {
                        String lastSeenDisplayTime = LastSeenTime.getTimeAgo(lastSeen, getApplicationContext());
                        userLastSeen.setText(lastSeenDisplayTime);
                    } else {
                        userLastSeen.setText("Online");
                    }
                }

                // load images offline
                Picasso.with(ChatActivity.this).load(thumbImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).
                        into(userChatProfileImage, new Callback() {
                            @Override
                            // onsuccess akan melload picture offline
                            public void onSuccess() {}

                            @Override
                            // onEror berarti load ofline gagal, maka load gambar lgsg ke database
                            public void onError() {
                                Picasso.with(ChatActivity.this).load(thumbImage).placeholder(R.drawable.default_profile).into(userChatProfileImage);
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        sendMessageButton.setOnClickListener(view -> kirimPesan());

        sendMessageButton.setOnLongClickListener(view -> {
            showUpdateDialog();

            return false;
        });


        // open gallery
        selectImageButton.setOnClickListener(view -> {
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GALLERY_PICK);
        });

        fetchMessages();

        DatabaseReference getUserDataReference = FirebaseDatabase.getInstance().getReference().child(USERS_STRING).child(messageReceiverId);
        getUserDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String kunciPublicBHexa = dataSnapshot.child("user_public_key").getValue().toString(); // kunci publik bentuknya HEXA

                tempPublicKey.setText(kunciPublicBHexa);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        DatabaseReference getUserDataReference2 = FirebaseDatabase.getInstance().getReference().child(USERS_STRING).child(messageSenderID);
        getUserDataReference2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String encryptedPrivateKey = dataSnapshot.child("user_private_key").getValue().toString(); // kunci private bentuknya HEXA
                Log.d("encrypted private key", encryptedPrivateKey);

                try {
                    String decryptPrivateKey = decryptPrivateKey(encryptedPrivateKey, messageSenderEmail);

                    tempPrivateKey.setText(decryptPrivateKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void showUpdateDialog() { // dialog enkripsi
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

        alert.setPositiveButton("ok", (dialog, whichButton) -> {
            final String messageText1 = editTextPesanEnkripsi.getText().toString();
            final String inputPassword = editPwdUser.getText().toString(); //ini password user
            final String privateKey1 = editPrivateKey.getText().toString();//ini kunci private

            if (TextUtils.isEmpty(messageText1)) {
                Toast.makeText(ChatActivity.this, "Silahkan Isi Pesan Anda", Toast.LENGTH_SHORT).show();
            }
            if (TextUtils.isEmpty(inputPassword)) {
                Toast.makeText(ChatActivity.this, "Silahkan Isi Password Anda", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    mAuth.signInWithEmailAndPassword(messageSenderEmail, inputPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            try {
                                //start
                                long lStartTime = System.nanoTime();

                                //task
                                outputString = (encrypt(messageText1, privateKey1)).trim();

                                //end
                                long lEndTime = System.nanoTime();

                                //time elapsed
                                long output = lEndTime - lStartTime;

                                Log.d("Output", "Waktu Enkripsi dalam milliseconds: " + output / 1000000);

                                inputMessageText.setText(outputString);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(ChatActivity.this, "password tidak dikenali, silahkan Cek kembali password anda",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    inputMessageText.setEnabled(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        alert.setNegativeButton("Cancel", (dialog, whichButton) -> dialog.cancel());
        alert.show();
    }

    // buat gambar
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null) {
            loadingBar.setTitle("Mengirim Gambar");
            loadingBar.setMessage("Mohon Tunggu, ketika gambar anda sedang dikirim ...");
            loadingBar.show();

            Uri imageUri = data.getData();

            final String messageSenderRef = MESSAGES_STRING + "/" + messageSenderID + "/" + messageReceiverId;
            final String messageReceiverRef = MESSAGES_STRING + "/" + messageReceiverId + "/" + messageSenderID;

            DatabaseReference messageKey = rootRef.child(MESSAGES_STRING).child(messageSenderID).child(messageReceiverId).push();
            final String messagePushId = messageKey.getKey();

            StorageReference filePath = messageImageStorageRef.child(messagePushId + ".jpg");
            filePath.putFile(imageUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // get url of the image from firebase storage for image
                    final String downloadUrl = task.getResult().getDownloadUrl().toString();


                    // store to database
                    Map<String, Object> messageTextBody = new HashMap<>();
                    messageTextBody.put("message", downloadUrl);
                    messageTextBody.put("seen", false);
                    messageTextBody.put("type", "image");
                    messageTextBody.put("time", ServerValue.TIMESTAMP);
                    messageTextBody.put("from", messageSenderID);

                    Map<String, Object> messageBodyDetails = new HashMap<>();
                    messageBodyDetails.put(messageSenderRef + "/" + messagePushId, messageTextBody);
                    messageBodyDetails.put(messageReceiverRef + "/" + messagePushId, messageTextBody);

                    rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child(SEEN_STRING).setValue(true);
                    rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child(TIMESTAMP_STRING).setValue(ServerValue.TIMESTAMP);

                    rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child(SEEN_STRING).setValue(false);
                    rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child(TIMESTAMP_STRING).setValue(ServerValue.TIMESTAMP);

                    rootRef.updateChildren(messageBodyDetails, (databaseError, databaseReference) -> {
                        if (databaseError != null) {
                            Log.d("Chat_Log", databaseError.getMessage());
                        }

                        inputMessageText.setText("");

                        loadingBar.dismiss();
                    });

                    Toast.makeText(ChatActivity.this, "Gambar telah terkirim", Toast.LENGTH_SHORT).show();

                    loadingBar.dismiss();
                } else {
                    Toast.makeText(ChatActivity.this, "Gambar gagal terkirim, Coba Lagi", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            });
        }
    }


    private void fetchMessages() {
        // pertama akan load 10 pesan
        // terus jika di refresh oleh user maka mcurrent page berubah jadi 2 di oncreate, maka pesan jadi 20
        rootRef.child(MESSAGES_STRING).child(messageSenderID).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Messages messages = dataSnapshot.getValue(Messages.class);

                        messageList.add(messages);
                        messageAdapter.notifyDataSetChanged();

                        // ketika fetchmessage, halaman page lgsg ke paling bawah alias pesan terakhir
                        userMessagesList.scrollToPosition(messageList.size() - 1);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) { }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
    }

    private void kirimPesan() {
        String messageText = inputMessageText.getText().toString();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(ChatActivity.this, R.string.please_fill_message, Toast.LENGTH_SHORT).show();
        } else {
            // membuat reference ke database ( tabel )
            String messageSenderRef = MESSAGES_STRING + "/" + messageSenderID + "/" + messageReceiverId;
            String messageReceiverRef = MESSAGES_STRING + "/" + messageReceiverId + "/" + messageSenderID;

            // membuat unique ID dari message
            DatabaseReference messageKey = rootRef.child(MESSAGES_STRING).child(messageSenderID).child(messageReceiverId).push();
            String messagePushId = messageKey.getKey();

            // store to database
            Map<String, Object> messageTextBody = new HashMap<>();
            messageTextBody.put("message", messageText);
            messageTextBody.put("seen", false);
            messageTextBody.put("type", "text");
            messageTextBody.put("time", ServerValue.TIMESTAMP);
            messageTextBody.put("from", messageSenderID);

            Map<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushId, messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushId, messageTextBody);

            rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child(SEEN_STRING).setValue(true);
            rootRef.child("Chat").child(messageSenderID).child(messageReceiverId).child(TIMESTAMP_STRING).setValue(ServerValue.TIMESTAMP);

            rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child(SEEN_STRING).setValue(false);
            rootRef.child("Chat").child(messageReceiverId).child(messageSenderID).child(TIMESTAMP_STRING).setValue(ServerValue.TIMESTAMP);

            // menyimpan data notifikasi ke firebase
            HashMap<String, String> notificationsData = new HashMap<String, String>();
            notificationsData.put("from", messageSenderID);
            notificationsData.put("type", "sent_message");
            notificationsData.put("isi_pesan", messageText);
            notificationsReference.child(messageReceiverId).push().setValue(notificationsData);

            inputMessageText.setText("");
            inputMessageText.setEnabled(true);

            rootRef.updateChildren(messageBodyDetails, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    Log.d("Chat_Log", databaseError.getMessage());
                }
            });
        }
    }

    private String encrypt(String Data, String password) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        SecretKeySpec key = generateKey(password, salt);
        Cipher c = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
        c.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = c.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] encryptedText = c.doFinal(Data.getBytes(StandardCharsets.UTF_8));

        // concatenate salt + iv + ciphertext
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt);
        outputStream.write(iv);
        outputStream.write(encryptedText);


        //  byte[] encVal = c.doFinal(Data.getBytes());
        String encryptedValue = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

        String x = outputStream.toString(String.valueOf(StandardCharsets.US_ASCII));
        Log.d("pesan terenkripsi", x);

        return encryptedValue;
    }

    public String decrypt(String outputString, String password) throws Exception {
        byte[] decodeValue = Base64.decode(outputString, Base64.DEFAULT);

        byte[] salt = Arrays.copyOfRange(decodeValue, 0, 16);
        byte[] iv = Arrays.copyOfRange(decodeValue, 16, 32);
        byte[] ct = Arrays.copyOfRange(decodeValue, 32, decodeValue.length);

        SecretKeySpec key = generateKey(password, salt);
        Cipher c = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);

        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plaintext = c.doFinal(ct);
        // c.init(Cipher.DECRYPT_MODE, key);

        String x = new String(plaintext, StandardCharsets.US_ASCII);
        Log.d("pesan terdekripsi", x);

        return new String(plaintext, StandardCharsets.UTF_8);

        // byte[] decValue = c.doFinal(decodeValue);
        //  String decryptedValue = new String(decValue);
        //  return decryptedValue;
        // return null;
    }

    private SecretKeySpec generateKey(String password, byte[] salt) throws Exception {
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

    public String decryptPrivateKey(String outputString, String password) throws Exception {
        byte[] decodeValue = Base64.decode(outputString, Base64.DEFAULT);

        byte[] salt = Arrays.copyOfRange(decodeValue, 0, 16);
        byte[] iv = Arrays.copyOfRange(decodeValue, 16, 32);
        byte[] ct = Arrays.copyOfRange(decodeValue, 32, decodeValue.length);

        SecretKeySpec key = generateKeyForPrivate(password, salt);
        Cipher c = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);

        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plaintext = c.doFinal(ct);
        // c.init(Cipher.DECRYPT_MODE, key);

        String x = new String(plaintext, StandardCharsets.US_ASCII);
        Log.d("pesan terdekripsi", x);

        return new String(plaintext, StandardCharsets.UTF_8);

        // byte[] decValue = c.doFinal(decodeValue);
        //  String decryptedValue = new String(decValue);
        //  return decryptedValue;
        // return null;
    }

    private SecretKeySpec generateKeyForPrivate(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = f.generateSecret(spec).getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        return secretKeySpec;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
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
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public void itemClicked(View view, int position) {

        String TAG = "messagePosition";
        Log.d(TAG, "You clicked on " + position);

        Messages messages = messageList.get(position);
        String pesanTerenkripsi = messages.getMessage();
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

        btnDekripsi.setOnClickListener(view1 -> {
            final String messageText = editTextPesanDekripsi.getText().toString();
            final String inputPassword = editPwdUser.getText().toString();
            final String privateKey1 = editKunciDekripsi.getText().toString();

            if (TextUtils.isEmpty(messageText)) {
                Toast.makeText(ChatActivity.this, R.string.please_fill_message, Toast.LENGTH_SHORT).show();
            }
            if (TextUtils.isEmpty(inputPassword)) {
                Toast.makeText(ChatActivity.this, "Silahkan Isi Password Anda", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    mAuth.signInWithEmailAndPassword(messageSenderEmail, inputPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            try {
                                //start
                                long lStartTime = System.nanoTime();

                                //task
                                outputString = decrypt(messageText, privateKey1);
                                txtPesanTerdekripsi.setText(outputString);

                                //end
                                long lEndTime = System.nanoTime();

                                //time elapsed
                                long output = lEndTime - lStartTime;

                                System.out.println("Waktu Dekripsi dalam milliseconds: " + output / 1000000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(ChatActivity.this, "password tidak dikenali, silahkan Cek kembali password anda",
                                    Toast.LENGTH_SHORT).show();
                        }

                        loadingBar.dismiss();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        alert.setPositiveButton("ok", (dialog, whichButton) -> dialog.cancel());

        alert.show();
    }
}
