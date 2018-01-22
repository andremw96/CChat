package com.example.andre.cchat;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
// import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private FirebaseAuth mAuth;

    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsPagerAdapter myTabsPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instansi firebase authentication
        mAuth = FirebaseAuth.getInstance();

        // tab2 untuk mainactivity
        myViewPager = (ViewPager) findViewById(R.id.main_tabs_pager);
        myTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsPagerAdapter);
        myTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);


        mToolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("CChat");
    }

    @Override
    protected void onStart() {
        super.onStart();

        // mengambil user yg sedang login
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // jika user blm login
        if ( currentUser == null )
        {
            LogoutUser();
        }
    }

    private void LogoutUser()
    {
        Intent startPageIntent = new Intent(MainActivity.this, StartPageActivity.class);
        // ketika user pencet back button, maka dia tidak bisa ke main activity lagi
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // memanggil menu
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);

        if(item.getItemId() == R.id.main_logout_button)
        {
            mAuth.signOut(); // logout dari firebase database

            LogoutUser(); // kembali ke main page
        }

        if(item.getItemId() == R.id.main_pengaturan_akun_button)
        {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        return true;
    }
}
