package com.example.andre.cchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
// import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference storeUserDefaultDataReference; // mengambil route database di firebase

    private Toolbar mToolbar;
    private ProgressDialog loadingBar;

    private EditText registerUserName;
    private EditText registerUserEmail;
    private EditText registerUserPassword;
    private Button buatAkunButton;

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
        buatAkunButton = (Button) findViewById(R.id.buat_akun_button);
        loadingBar = new ProgressDialog(this);

        buatAkunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                final String name = registerUserName.getText().toString();
                String email = registerUserEmail.getText().toString();
                String pwd = registerUserPassword.getText().toString();

                DaftarkanAkun(name, email, pwd);
            }
        });
    }

    private void DaftarkanAkun(final String name, String email, String pwd)
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
}
