package com.example.matchandplay;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements CardStackListener {

    private CardStackView cardStackView;
    private CardAdapter adapter;
    private CardStackLayoutManager manager;
    private List<User> userList = new ArrayList<>();

    // This Set will hold the IDs of everyone we already swiped
    private Set<String> swipedUserIds = new HashSet<>();

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUId;

    private String userSex;
    private String oppositeSex;

    // Variables for the Matchmaking Algorithm
    private String myLocation = "";
    private String myPositionOrNeeds = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            finish();
            return;
        }
        currentUId = mAuth.getCurrentUser().getUid();

        // --- NEW: Top Bar Settings Button ---
        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Top Bar Matches Button
        Button btnMatches = findViewById(R.id.btnMatches);
        btnMatches.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MatchesActivity.class);
            startActivity(intent);
        });

        // Empty State: Update Stats Button
        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        btnEditProfile.setOnClickListener(v -> {
            if (userSex != null && userSex.equals("player")) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, TeamProfileActivity.class));
            }
        });

        // --- THE RADAR PULSE ANIMATION ---
        ImageView radarPulse = findViewById(R.id.radarPulse);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(radarPulse, "scaleX", 1f, 2.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(radarPulse, "scaleY", 1f, 2.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(radarPulse, "alpha", 0.5f, 0f);

        // Make it loop forever
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        // Set speed (1.5 seconds per pulse)
        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        alpha.setDuration(1500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.start();
        // --------------------------------------

        // Setup Card Stack
        cardStackView = findViewById(R.id.card_stack_view);
        manager = new CardStackLayoutManager(this, this);
        adapter = new CardAdapter(this, userList);
        cardStackView.setLayoutManager(manager);
        cardStackView.setAdapter(adapter);
        cardStackView.setItemAnimator(new DefaultItemAnimator());

        // Step 1: Find out who we are and get our location/needs
        checkUserType();
    }

    private void checkUserType() {
        DatabaseReference usersDb = mDatabase;
        usersDb.child("player").child(currentUId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    // --- GATEKEEPER CHECK: Did they finish their Player profile? ---
                    if (!snapshot.hasChild("position") || !snapshot.hasChild("country")) {
                        Toast.makeText(MainActivity.this, "Please complete your Player Profile", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                        finish();
                        return;
                    }

                    userSex = "player";
                    oppositeSex = "team";
                    // Save my data to compare later for the algorithm
                    if(snapshot.child("country").getValue() != null) myLocation = snapshot.child("country").getValue().toString();
                    if(snapshot.child("position").getValue() != null) myPositionOrNeeds = snapshot.child("position").getValue().toString();

                    getSwipedHistory();
                } else {
                    // If not a player, check if they are a team
                    usersDb.child("team").child(currentUId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot teamSnap) {
                            if (teamSnap.exists()) {

                                // --- GATEKEEPER CHECK: Did they finish their Team profile? ---
                                if (!teamSnap.hasChild("lookingFor") || !teamSnap.hasChild("country")) {
                                    Toast.makeText(MainActivity.this, "Please complete your Team Stats", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(MainActivity.this, TeamProfileActivity.class));
                                    finish();
                                    return;
                                }

                                userSex = "team";
                                oppositeSex = "player";
                                // Save my data to compare later for the algorithm
                                if(teamSnap.child("country").getValue() != null) myLocation = teamSnap.child("country").getValue().toString();
                                if(teamSnap.child("lookingFor").getValue() != null) myPositionOrNeeds = teamSnap.child("lookingFor").getValue().toString();

                                getSwipedHistory();

                            } else {
                                // THE ULTIMATE FALLBACK:
                                startActivity(new Intent(MainActivity.this, ChooseTypeActivity.class));
                                finish();
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void getSwipedHistory() {
        // Look in "connections/yeps" and "connections/nopes"
        DatabaseReference yepsDb = mDatabase.child(userSex).child(currentUId).child("connections").child("yeps");
        yepsDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot user : snapshot.getChildren()){
                        swipedUserIds.add(user.getKey());
                    }
                }

                // Now fetch Nopes
                DatabaseReference nopesDb = mDatabase.child(userSex).child(currentUId).child("connections").child("nopes");
                nopesDb.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            for(DataSnapshot user : snapshot.getChildren()){
                                swipedUserIds.add(user.getKey());
                            }
                        }
                        // Step 3: NOW we are ready to load the cards
                        getOppositeUsers();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void getOppositeUsers() {
        TextView txtNoUsers = findViewById(R.id.txtNoUsers);

        mDatabase.child(oppositeSex).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String s) {
                if (snapshot.exists() && !snapshot.getKey().equals(currentUId) && !swipedUserIds.contains(snapshot.getKey())) {
                    try {
                        String name = snapshot.child("name").getValue() != null ? snapshot.child("name").getValue().toString() : "Unknown";
                        String profileImageUrl = snapshot.child("profileImageUrl").getValue() != null ? snapshot.child("profileImageUrl").getValue().toString() : "default";
                        String line1 = "No data";
                        String line2 = "No data";

                        // --- THE SMART MATCHMAKING ALGORITHM ---
                        int score = 0;
                        String otherLocation = snapshot.child("country").getValue() != null ? snapshot.child("country").getValue().toString() : "";
                        String otherPositionOrNeeds = "";

                        if (oppositeSex.equals("player")) {
                            if (snapshot.child("position").getValue() != null) {
                                otherPositionOrNeeds = snapshot.child("position").getValue().toString();
                                line1 = otherPositionOrNeeds;
                            }
                            if (snapshot.child("age").getValue() != null) line2 = snapshot.child("age").getValue().toString() + " years";
                        } else {
                            if (snapshot.child("lookingFor").getValue() != null) {
                                otherPositionOrNeeds = snapshot.child("lookingFor").getValue().toString();
                                line1 = "Needs: " + otherPositionOrNeeds;
                            }
                            if (snapshot.child("country").getValue() != null) line2 = snapshot.child("country").getValue().toString();
                        }

                        // Rule 1: Location Match (40 Points)
                        if (!myLocation.isEmpty() && !otherLocation.isEmpty() && myLocation.equalsIgnoreCase(otherLocation)) {
                            score += 40;
                        }

                        // Rule 2: Position Match (60 Points)
                        if (!myPositionOrNeeds.isEmpty() && !otherPositionOrNeeds.isEmpty()) {
                            String myNeedLower = myPositionOrNeeds.toLowerCase();
                            String otherNeedLower = otherPositionOrNeeds.toLowerCase();

                            if (myNeedLower.contains(otherNeedLower) || otherNeedLower.contains(myNeedLower)) {
                                score += 60;
                            }
                        }

                        // Append the score to the name string first
                        String displayName = name + " (" + score + "% Match)";

                        // Create the User ONCE with the updated name
                        User user = new User(snapshot.getKey(), displayName, line2, line1, profileImageUrl);
                        user.setMatchScore(score);

                        // Add to the list
                        userList.add(user);

                        // --- SORT THE DECK (Highest Score First) ---
                        Collections.sort(userList, (u1, u2) -> Integer.compare(u2.getMatchScore(), u1.getMatchScore()));

                        adapter.notifyDataSetChanged();

                        // Hide the searching text because we found at least one card
                        txtNoUsers.setVisibility(View.GONE);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String s) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- THE MATCH POPUP METHOD ---
    private void showMatchDialog(String matchId) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_match);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btnChatNow = dialog.findViewById(R.id.btnChatNow);
        Button btnKeepSwiping = dialog.findViewById(R.id.btnKeepSwiping);

        btnChatNow.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("matchId", matchId);
            startActivity(intent);
        });

        btnKeepSwiping.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- SWIPE LOGIC ---
    @Override
    public void onCardSwiped(Direction direction) {
        if (manager.getTopPosition() > userList.size()) return;

        User swipedUser = userList.get(manager.getTopPosition() - 1);
        String otherUid = swipedUser.getUid();

        swipedUserIds.add(otherUid);

        if (direction == Direction.Right) {
            // --- SAVE AS YEP (Like) ---
            mDatabase.child(userSex).child(currentUId).child("connections").child("yeps").child(otherUid).setValue(true);

            // Check for Match
            mDatabase.child(oppositeSex).child(otherUid).child("connections").child("yeps").child(currentUId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String key = FirebaseDatabase.getInstance().getReference().child("Chat").push().getKey();

                        mDatabase.child(userSex).child(currentUId).child("connections").child("matches").child(otherUid).child("ChatId").setValue(key);
                        mDatabase.child(oppositeSex).child(otherUid).child("connections").child("matches").child(currentUId).child("ChatId").setValue(key);

                        showMatchDialog(otherUid);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });

        } else if (direction == Direction.Left) {
            // --- SAVE AS NOPE (Dislike) ---
            mDatabase.child(userSex).child(currentUId).child("connections").child("nopes").child(otherUid).setValue(true);
        }
    }

    @Override public void onCardDragging(Direction direction, float ratio) {}
    @Override public void onCardRewound() {}
    @Override public void onCardCanceled() {}
    @Override public void onCardAppeared(View view, int position) {}
    @Override public void onCardDisappeared(View view, int position) {}
}