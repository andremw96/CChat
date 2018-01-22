package com.example.andre.cchat;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView settingsDisplayProfileImage;
    private TextView settingsDisplayName;
    private TextView settingsDisplayStatus;
    private Button settingsUbahFotoProfilButton;
    private Button settingsUbahStatusButton;

    private final static int gallerypick = 1;

    private StorageReference storeProfileImageStorageRef;
    private DatabaseReference getUserDataReference;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        String online_user_id = mAuth.getCurrentUser().getUid();
        getUserDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child(online_user_id);
        storeProfileImageStorageRef = FirebaseStorage.getInstance().getReference().child("Profile_Images");


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
                Picasso.with(SettingsActivity.this).load(image).into(settingsDisplayProfileImage);

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });

        settingsUbahFotoProfilButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // membuka aplikasi galeri HP
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, gallerypick);
            }
        });

        settingsUbahStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String old_status = settingsDisplayStatus.getText().toString();

                Intent statusIntent = new Intent(SettingsActivity.this, StatusActivity.class);
                statusIntent.putExtra("user_status", old_status);
                startActivity(statusIntent);
            }
        });
    }

    // cropping foto profil
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == gallerypick && resultCode == RESULT_OK && data != null)
        {
            Uri ImageUri = data.getData();
            CropImage.activity(ImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {
                Uri resultUri = result.getUri();

                String userID = mAuth.getCurrentUser().getUid();
                StorageReference filePath = storeProfileImageStorageRef.child(userID + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful())
                        {
                            Toast.makeText(SettingsActivity.this,
                                    "Foto Profil berhasil telah tersimpan",
                                     Toast.LENGTH_LONG).show();

                            // get url from firebase storage for image
                            String downloadUrl = task.getResult().getDownloadUrl().toString();
                            getUserDataReference.child("user_image").setValue(downloadUrl).
                                    addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            Toast.makeText(SettingsActivity.this,
                                                    "Foto Profil berhasil diunggah", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }
                        else
                        {
                            Toast.makeText(SettingsActivity.this,
                                    "Terdapat kesalahan dalam menyimpan foto profil, silahkan coba lagi",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
            }
        }
    }
}
