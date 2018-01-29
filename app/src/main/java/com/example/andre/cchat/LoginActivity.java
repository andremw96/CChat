package com.example.andre.cchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
// import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private FirebaseAuth mAuth;

    private Button loginButton;
    private EditText loginEmail;
    private EditText loginPwd;
    private ProgressDialog loadingBar;

    private DatabaseReference usersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");

        mToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Sign In");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loginButton = (Button) findViewById(R.id.login_button);
        loginEmail = (EditText) findViewById(R.id.login_email);
        loginPwd = (EditText) findViewById(R.id.login_pwd);
        loadingBar = new ProgressDialog(this);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                String email = loginEmail.getText().toString();
                String pwd = loginPwd.getText().toString();

                loginUserAccount(email, pwd);
            }
        });
    }

    private void loginUserAccount(String email, String pwd)
    {
        if (TextUtils.isEmpty(email))
        {
            Toast.makeText(LoginActivity.this, "Masukkan email anda",
                    Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(pwd))
        {
            Toast.makeText(LoginActivity.this, "Masukkan password anda",
                    Toast.LENGTH_SHORT).show();
        }

        else
        {
            loadingBar.setTitle("Login Akun");
            loadingBar.setMessage("Silahkan tunggu, ketika kita melakukan verifikasi akun anda");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, pwd).
                    addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if(task.isSuccessful())
                    {
                        String online_user_id = mAuth.getCurrentUser().getUid();
                        String device_token = FirebaseInstanceId.getInstance().getToken();

                        usersReference.child(online_user_id).child("device_token").setValue(device_token)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(mainIntent);
                                        finish();
                                    }
                                });
                    }
                    else
                    {
                        Toast.makeText(LoginActivity.this, "Email dan password tidak dikenali, silahkan Cek kembali email atau password anda",
                                Toast.LENGTH_SHORT).show();
                    }

                    loadingBar.dismiss();
                }
            });
        }
    }
}
