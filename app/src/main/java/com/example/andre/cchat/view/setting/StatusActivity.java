package com.example.andre.cchat.view.setting;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private EditText statusInput;
    private ProgressDialog loadingBar;

    private DatabaseReference changeStatusRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        // reference to online user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        changeStatusRef = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.status_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Ubah Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button saveChangeButton = (Button) findViewById(R.id.status_save_change_button);
        statusInput = (EditText) findViewById(R.id.status_input);
        loadingBar = new ProgressDialog(this);

        String old_status = getIntent().getExtras().get("user_status").toString();
        statusInput.setText(old_status);

        saveChangeButton.setOnClickListener(view -> {
            String newStatus = statusInput.getText().toString();

            ChangeProfileStatus(newStatus);
        });
    }

    private void ChangeProfileStatus(String newStatus) {
        if (TextUtils.isEmpty(newStatus)) {
            Toast.makeText(StatusActivity.this,
                    "Silahkan Isi Status Anda", Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle("Mengubah Status Profil Anda...");
            loadingBar.setMessage("Harap Tunggu, ketika status profil anda sedang diubah");
            loadingBar.show();

            changeStatusRef.child("user_status").setValue(newStatus)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            loadingBar.dismiss();

                            Intent settingsIntent = new Intent(StatusActivity.this, SettingsActivity.class);
                            startActivity(settingsIntent);

                            Toast.makeText(StatusActivity.this,
                                    "Status Profil berhasil diubah...",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(StatusActivity.this,
                                    "Terjadi Kesalahan",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}
