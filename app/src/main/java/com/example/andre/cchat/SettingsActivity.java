package com.example.andre.cchat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView settingsDisplayProfileImage;
    private TextView settingsDisplayName;
    private TextView settingsDisplayStatus;
    private Button settingsUbahFotoProfilButton;
    private Button settingsUbahStatusButton;

    private DatabaseReference getUserDataReference;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        String online_user_id = mAuth.getCurrentUser().getUid();
        getUserDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child(online_user_id);

        settingsDisplayProfileImage = (CircleImageView) findViewById(R.id.settings_profile_image);
        settingsDisplayName = (TextView) findViewById(R.id.settings_username);
        settingsDisplayStatus = (TextView) findViewById(R.id.settings_user_status);
        settingsUbahFotoProfilButton = (Button) findViewById(R.id.settings_ubah_foto_profil_button);
        settingsUbahStatusButton = (Button) findViewById(R.id.settings_ubah_status_button);

        getUserDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // datasnapshot itu objek yg pnya reference, sesuai getuserdatareferance
                String name = dataSnapshot.child("user_name").getValue().toString();
                String status = dataSnapshot.child("user_status").getValue().toString();
                String image = dataSnapshot.child("user_image").getValue().toString();
                String thumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                settingsDisplayName.setText(name);
                settingsDisplayStatus.setText(status);
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }
}
