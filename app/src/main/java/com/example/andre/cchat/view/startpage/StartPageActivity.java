package com.example.andre.cchat.view.startpage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.example.andre.cchat.R;
import com.example.andre.cchat.view.login.LoginActivity;
import com.example.andre.cchat.view.register.RegisterActivity;

public class StartPageActivity extends AppCompatActivity {

    private Button butuhAkunButton;
    private Button punyaAkunButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        // cast button / memanggil button ke script java
        butuhAkunButton = (Button) findViewById(R.id.butuh_akun_button);
        punyaAkunButton = (Button) findViewById(R.id.punya_akun_button);

        butuhAkunButton.setOnClickListener(view -> {
            Intent registerIntent = new Intent(StartPageActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        });

        punyaAkunButton.setOnClickListener(view -> {
            Intent loginIntent = new Intent(StartPageActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        });
    }
}
