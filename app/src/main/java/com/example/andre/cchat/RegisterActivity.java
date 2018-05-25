package com.example.andre.cchat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
// import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import de.frank_durr.ecdh_curve25519.ECDHCurve25519;

public class RegisterActivity extends AppCompatActivity {

    public static final String TAG = ECDHCurve25519.class.getName();

    static {
        try {
            System.loadLibrary("ecdhcurve25519");
            Log.i(TAG, "Loaded ecdhcurve25519 library.");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private FirebaseAuth mAuth;
    private DatabaseReference storeUserDefaultDataReference; // mengambil route database di firebase

    private Toolbar mToolbar;
    private ProgressDialog loadingBar;

    private EditText registerUserName;
    private EditText registerUserEmail;
    private EditText registerUserPassword;
    private EditText registerUserPrivateKey;
    private Button generatePrivateKeyButton;
    private Button buatAkunButton;

    String your_public_key_str;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();


        mToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Sign Up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        registerUserName = (EditText) findViewById(R.id.register_name);
        registerUserEmail = (EditText) findViewById(R.id.register_email);
        registerUserPassword = (EditText) findViewById(R.id.register_password);
        registerUserPrivateKey = (EditText) findViewById(R.id.register_private_key);
        generatePrivateKeyButton = (Button) findViewById(R.id.generate_key_button);
        buatAkunButton = (Button) findViewById(R.id.buat_akun_button);
        loadingBar = new ProgressDialog(this);

        generatePrivateKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SecureRandom random = new SecureRandom();
                byte[] your_private_key = ECDHCurve25519.generate_secret_key(random);
                Log.d("a private key hex", binarytoHexString(your_private_key));

                try {
                    String s = new String(your_private_key, "US-ASCII");
                    Log.d("kunci privatnya", s);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // Create Alice's public key.
                byte[] your_public_key = ECDHCurve25519.generate_public_key(your_private_key);
                Log.d("a public key hex", binarytoHexString(your_public_key));

                try {
                    String x = new String(your_public_key, "US-ASCII");
                    Log.d("kunci publiknya", x);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // masih bentuk HEXA
                String your_private_key_str = binarytoHexString(your_private_key);
                your_public_key_str = binarytoHexString(your_public_key);
                registerUserPrivateKey.setText(your_private_key_str);

                Toast.makeText(RegisterActivity.this, "Silahkan Simpan Kunci Private anda", Toast.LENGTH_SHORT).show();
            }
        });

        buatAkunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                final String name = registerUserName.getText().toString();
                final String email = registerUserEmail.getText().toString();
                final String pwd = registerUserPassword.getText().toString();
                final String public_key = your_public_key_str;

                AlertDialog.Builder alert = new AlertDialog.Builder(RegisterActivity.this);
                alert.setTitle("Konfirmasi");
                alert.setMessage("Apakah Anda sudah Menyimpan Kunci Private anda?");
                alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        DaftarkanAkun(name, email, pwd, public_key);
                    }
                });

                alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

                alert.show();

            }
        });

    }

    private void DaftarkanAkun(final String name, String email, String pwd, final String public_key)
    {
        // validasi mengecek apakaah field kosong atau tidak
        if(TextUtils.isEmpty(name))
        {
            Toast.makeText(RegisterActivity.this, "Masukkan nama anda.", Toast.LENGTH_LONG).show();
        }

        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(RegisterActivity.this, "Masukkan email anda.", Toast.LENGTH_LONG).show();
        }

        if(TextUtils.isEmpty(pwd))
        {
            Toast.makeText(RegisterActivity.this, "Masukkan password anda.", Toast.LENGTH_LONG).show();
        }

        if(TextUtils.isEmpty(public_key))
        {
            Toast.makeText(RegisterActivity.this, "Silahkan tekan tombol 'Hasilkan Kunci Privat Milikmu', dan salin kunci anda ", Toast.LENGTH_LONG).show();
        }

        else
        {
            // memunuclkan loadingbar
            loadingBar.setTitle("Akun baru sedang dibuat");
            loadingBar.setMessage("Silahkan Tunggu, ketika akun sedang dibuat");
            loadingBar.show();

            // script untuk memasukkan data  email dan password ke firebase
            mAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    // jika berhasil menyimpan maka, user akan dialihkan ke main
                    if(task.isSuccessful())
                    {
                        String device_token = FirebaseInstanceId.getInstance().getToken();

                        String current_user_id = mAuth.getCurrentUser().getUid();
                        // create reference and store the reference inside variable, reference to firebase database
                        storeUserDefaultDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child(current_user_id);

                        storeUserDefaultDataReference.child("user_name").setValue(name);
                        storeUserDefaultDataReference.child("user_name_lowercase").setValue(name.toLowerCase());
                        storeUserDefaultDataReference.child("user_public_key").setValue(public_key);
                        storeUserDefaultDataReference.child("user_status").setValue("Hello World, I am using CChat");
                        storeUserDefaultDataReference.child("user_image").setValue("default_profile");
                        storeUserDefaultDataReference.child("device_token").setValue(device_token);
                        storeUserDefaultDataReference.child("user_thumb_image").setValue("default_image")
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if(task.isSuccessful())
                                        {
                                            Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                            // validasi user tidak kembali setelah register activity
                                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(mainIntent);
                                            finish();
                                        }
                                    }
                                });
                    }
                    else
                    {
                        Toast.makeText(RegisterActivity.this, "Ada kesalahan, silahkan coba lagi", Toast.LENGTH_SHORT).show();
                    }

                    // dissmiss loading bar
                    loadingBar.dismiss();
                }
            });
        }
    }

    static private String binarytoHexString(byte[] binary)
    {
        StringBuilder sb = new StringBuilder(binary.length*2);

        // Go backwards (left to right in the string) since typically you print the low-order
        // bytes to the right.
        for (int i = binary.length-1; i >= 0; i--) {
            // High nibble first, i.e., to the left.
            // Note that bytes are signed in Java. However, "int x = abyte&0xff" will always
            // return an int value of x between 0 and 255.
            // "int v = binary[i]>>4" (without &0xff) does *not* work.
            int v = (binary[i]&0xff)>>4;
            char c;
            if (v < 10) {
                c = (char) ('0'+v);
            } else {
                c = (char) ('a'+v-10);
            }
            sb.append(c);
            // low nibble
            v = binary[i]&0x0f;
            if (v < 10) {
                c = (char) ('0'+v);
            } else {
                c = (char) ('a'+v-10);
            }
            sb.append(c);
        }

        return sb.toString();
    }
}
