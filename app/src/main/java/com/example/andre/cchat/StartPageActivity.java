package com.example.andre.cchat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartPageActivity extends AppCompatActivity {

    private Button ButuhAkunButton;
    private Button PunyaAkunButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        // cast button / memanggil button ke script java
        ButuhAkunButton = (Button) findViewById(R.id.butuh_akun_button);
        PunyaAkunButton = (Button) findViewById(R.id.punya_akun_button);

        ButuhAkunButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent registerIntent = new Intent(StartPageActivity.this, RegisterActivity.class);
                startActivity(registerIntent);
            }
        });

        PunyaAkunButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent loginIntent = new Intent(StartPageActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });
    }
}
