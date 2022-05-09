package com.example.andre.cchat.view.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.andre.cchat.R;
import com.example.andre.cchat.view.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

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

        Toolbar mToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Sign In");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginEmail = (EditText) findViewById(R.id.login_email);
        loginPwd = (EditText) findViewById(R.id.login_pwd);
        loadingBar = new ProgressDialog(this);

        loginButton.setOnClickListener(view -> {
            String email = loginEmail.getText().toString();
            String pwd = loginPwd.getText().toString();

            loginUserAccount(email, pwd);
        });
    }

    private void loginUserAccount(String email, String pwd) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "Masukkan email anda",
                    Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(LoginActivity.this, "Masukkan password anda",
                    Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle("Login Akun");
            loadingBar.setMessage("Silahkan tunggu, ketika kita melakukan verifikasi akun anda");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, pwd).
                    addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String online_user_id = mAuth.getCurrentUser().getUid();
                            String device_token = FirebaseInstanceId.getInstance().getToken();

                            usersReference.child(online_user_id).child("device_token").setValue(device_token)
                                    .addOnSuccessListener(aVoid -> {
                                        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(mainIntent);
                                        finish();
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, "Email dan password tidak dikenali, silahkan Cek kembali email atau password anda",
                                    Toast.LENGTH_SHORT).show();
                        }

                        loadingBar.dismiss();
                    });
        }
    }
}
