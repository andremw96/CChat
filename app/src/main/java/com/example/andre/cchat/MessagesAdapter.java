package com.example.andre.cchat;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Wijaya_PC on 26-Jan-18.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>
{
    private List<Messages> userMessagesList;

    private FirebaseAuth mAuth;

    private DatabaseReference usersReference;

    private Context context;
    private Activity parentActivity;

    private String pesanTerenkripsi;
    private String AES = "AES/CBC/PKCS5Padding";

    public MessagesAdapter(List<Messages> userMessagesList, Context context, Activity parentActivity) {
        this.userMessagesList = userMessagesList;
        this.context = context;
        this.parentActivity = parentActivity;
    }


    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View V = LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_layout_users, parent, false);

        mAuth = FirebaseAuth.getInstance();

        return new MessageViewHolder(V);
    }



    @Override
    public void onBindViewHolder(final MessageViewHolder holder, final int position)
    {
        pesanTerenkripsi = null;
        String messageSenderID = mAuth.getCurrentUser().getUid();
        final Messages messages = userMessagesList.get(position);

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

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String TAG = "messagePosition";
                Log.w(TAG, "You clicked on "+position);

                pesanTerenkripsi = messages.getMessage();
                System.out.println(pesanTerenkripsi);

                LayoutInflater linf = LayoutInflater.from(parentActivity);
                final View inflator = linf.inflate(R.layout.dekripsi_dialog, null);
                AlertDialog.Builder alert = new AlertDialog.Builder(parentActivity);

                alert.setTitle("Dekripsi Pesan");
                alert.setView(inflator);

                final EditText editTextPesanDekripsi = (EditText) inflator.findViewById(R.id.edit_pesan_dekripsi);

                editTextPesanDekripsi.setText(pesanTerenkripsi);

                alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                       // String messageText = editTextPesanDekripsi.getText().toString();

                      //  Log.d("messageText", messageText);

                       /* if(TextUtils.isEmpty(messageText))
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
                        }*/
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

                alert.show();

            }
        });
    }



    @Override
    public int getItemCount()
    {
        return userMessagesList.size();
    }



    // makiung view holder
    public static class MessageViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public TextView messageText;
        public CircleImageView userProfileImage;
        public ImageView messagePicture;

        public int adapterPosition;

        public MessageViewHolder(View view)
        {
            super(view);

            mView = view;

            messageText = (TextView) view.findViewById(R.id.message_text);

            userProfileImage = (CircleImageView) view.findViewById(R.id.messages_profile_image);

            messagePicture = (ImageView) view.findViewById(R.id.message_image_view);

            adapterPosition = getAdapterPosition();

        }
    }

    private String decrypt(String outputString, String password) throws Exception
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
        byte[] bytes = password.getBytes("UTF-8");

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
