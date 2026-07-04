package com.example.matchandplay;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MatchesActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mMatchesAdapter;
    private RecyclerView.LayoutManager mMatchesLayoutManager;
    private List<User> resultsMatches = new ArrayList<>();

    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches);

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize UI components but DO NOT fetch data yet
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setHasFixedSize(true);
        mMatchesLayoutManager = new LinearLayoutManager(MatchesActivity.this);
        mRecyclerView.setLayoutManager(mMatchesLayoutManager);

        mMatchesAdapter = new MatchesAdapter(resultsMatches, MatchesActivity.this);
        mRecyclerView.setAdapter(mMatchesAdapter);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    // --- LIFECYCLE OPTIMIZATION ---
    @Override
    protected void onStart() {
        super.onStart();
        // Clear the list to prevent duplicates and crashes when returning to this screen
        resultsMatches.clear();
        mMatchesAdapter.notifyDataSetChanged();

        // Fetch data now that the screen is safely visible
        checkUserTypeAndFetchMatches();
    }

    private void checkUserTypeAndFetchMatches() {
        DatabaseReference userDb = FirebaseDatabase.getInstance().getReference().child("Users");

        // First, check if the user is a Player
        userDb.child("player").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // I am a Player, so search for my matches
                    fetchMatchIds("player", "team");
                } else {
                    // I am not a player, check if I am a Team
                    userDb.child("team").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // I am a Team, so search for my matches
                                fetchMatchIds("team", "player");
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchMatchIds(String myType, String matchType) {
        DatabaseReference matchDb = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(myType).child(currentUserID).child("connections").child("matches");

        matchDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot match : snapshot.getChildren()) {
                        FetchMatchInformation(match.getKey(), matchType);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void FetchMatchInformation(String key, String type) {
        DatabaseReference userDb = FirebaseDatabase.getInstance().getReference().child("Users").child(type).child(key);
        userDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userId = snapshot.getKey();
                    String name = "Unknown";
                    String profileImageUrl = "default"; // FIXED: Prevents adapter crash from null URLs

                    if (snapshot.child("name").getValue() != null) {
                        name = snapshot.child("name").getValue().toString();
                    }
                    if (snapshot.child("profileImageUrl").getValue() != null) {
                        profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                    }

                    // Create the user object safely
                    User obj = new User(userId, name, "", "", profileImageUrl);

                    // Add to list and notify the adapter efficiently
                    resultsMatches.add(obj);
                    mMatchesAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}