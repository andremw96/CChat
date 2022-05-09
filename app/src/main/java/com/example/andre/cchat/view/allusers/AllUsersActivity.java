package com.example.andre.cchat.view.allusers;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.andre.cchat.R;
import com.example.andre.cchat.model.AllUsers;
import com.example.andre.cchat.view.allusers.adapter.AllUsersAdapter;
import com.example.andre.cchat.view.allusers.adapter.AllUsersViewHolder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class AllUsersActivity extends AppCompatActivity {

    private RecyclerView allUsersList;
    private DatabaseReference allUsersRef;
    private EditText searchInputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.all_users_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Semua User");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        allUsersList = (RecyclerView) findViewById(R.id.all_users_list);
        allUsersList.setHasFixedSize(true);
        allUsersList.setLayoutManager(new LinearLayoutManager(this));

        ImageButton searchButton = (ImageButton) findViewById(R.id.search_people_button);
        searchInputText = (EditText) findViewById(R.id.search_input_text);

        // ambil data dari firebase yg tabelnya users
        allUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        // load data offline
        allUsersRef.keepSynced(true);

        searchButton.setOnClickListener(view -> {
            String searchUserName = searchInputText.getText().toString().toLowerCase();

            if (TextUtils.isEmpty(searchUserName)) {
                Toast.makeText(AllUsersActivity.this, "Isikan nama teman yang dicari", Toast.LENGTH_SHORT).show();
            }

            findFriends(searchUserName);
        });

    }

    private void findFriends(String searchUserName) {
        Toast.makeText(this, "Sedang Mencari...", Toast.LENGTH_SHORT).show();

        Query searchFriends = allUsersRef.orderByChild("user_name_lowercase")
                .startAt(searchUserName).endAt(searchUserName + "\uf8ff");

        AllUsersAdapter firebaseRecyclerAdapter = new AllUsersAdapter(
                AllUsers.class,
                R.layout.all_users_display_layout,
                AllUsersViewHolder.class,
                searchFriends,
                this
        );

        allUsersList.setAdapter(firebaseRecyclerAdapter);
    }
}
