package com.example.matchandplay;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class TeamProfileActivity extends AppCompatActivity {

    private EditText mLeague, mFounded, mCountry, mStadium, mBudget, mLookingFor, mColors, mRival, mHonors;
    private Button mSave, mBack;
    private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    private String userId;

    private Uri resultUri; // Holds the image before uploading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_profile);

        // Map all the IDs from your activity_team_profile.xml
        mLeague = findViewById(R.id.inputPosition);
        mFounded = findViewById(R.id.inputAge);
        mCountry = findViewById(R.id.inputTeamCountry);
        mStadium = findViewById(R.id.inputFoot);
        mBudget = findViewById(R.id.inputBudget);
        mLookingFor = findViewById(R.id.inputLookingFor);
        mColors = findViewById(R.id.inputColors);
        mRival = findViewById(R.id.inputRival);
        mHonors = findViewById(R.id.inputHonors);

        mSave = findViewById(R.id.btnSave);
        mBack = findViewById(R.id.btnBack);
        mProfileImage = findViewById(R.id.profileImage);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("team").child(userId);

        loadExistingData();

        // 1. Open Gallery when clicking the picture
        mProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        mSave.setOnClickListener(v -> saveTeamInformation());

        mBack.setOnClickListener(v -> finish());
    }

    private void saveTeamInformation() {
        String league = mLeague.getText().toString().trim();
        String founded = mFounded.getText().toString().trim();
        String country = mCountry.getText().toString().trim();
        String stadium = mStadium.getText().toString().trim();
        String budget = mBudget.getText().toString().trim();
        String lookingFor = mLookingFor.getText().toString().trim();
        String colors = mColors.getText().toString().trim();
        String rival = mRival.getText().toString().trim();
        String honors = mHonors.getText().toString().trim();

        if (league.isEmpty() || country.isEmpty() || lookingFor.isEmpty()) {
            Toast.makeText(this, "Please fill in League, Country, and Looking For at minimum", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Team Stats...");
        progressDialog.show();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("position", league);
        userInfo.put("age", founded);
        userInfo.put("country", country);
        userInfo.put("foot", stadium);
        userInfo.put("budget", budget);
        userInfo.put("lookingFor", lookingFor);
        userInfo.put("colors", colors);
        userInfo.put("rival", rival);
        userInfo.put("honors", honors);
        userInfo.put("profileSetup", true);

        // --- FIREBASE STORAGE UPLOAD LOGIC ---
        if (resultUri != null) {
            StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profileImages").child(userId);
            filepath.putFile(resultUri).addOnSuccessListener(taskSnapshot -> filepath.getDownloadUrl().addOnSuccessListener(uri -> {
                userInfo.put("profileImageUrl", uri.toString());

                // Save to Database AFTER image uploads successfully
                mUserDatabase.updateChildren(userInfo).addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if(task.isSuccessful()){
                        goToMainApp();
                    } else {
                        Toast.makeText(TeamProfileActivity.this, "DB Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            })).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(TeamProfileActivity.this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        } else {
            // If they didn't pick a new image, just save the text
            mUserDatabase.updateChildren(userInfo).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    goToMainApp();
                } else {
                    Toast.makeText(TeamProfileActivity.this, "DB Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void goToMainApp() {
        Toast.makeText(TeamProfileActivity.this, "Club Info Saved!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(TeamProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            resultUri = data.getData();
            mProfileImage.setImageURI(resultUri); // Shows image instantly on screen
        }
    }

    private void loadExistingData() {
        mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("position").getValue() != null) mLeague.setText(snapshot.child("position").getValue().toString());
                    if (snapshot.child("age").getValue() != null) mFounded.setText(snapshot.child("age").getValue().toString());
                    if (snapshot.child("country").getValue() != null) mCountry.setText(snapshot.child("country").getValue().toString());
                    if (snapshot.child("foot").getValue() != null) mStadium.setText(snapshot.child("foot").getValue().toString());
                    if (snapshot.child("budget").getValue() != null) mBudget.setText(snapshot.child("budget").getValue().toString());
                    if (snapshot.child("lookingFor").getValue() != null) mLookingFor.setText(snapshot.child("lookingFor").getValue().toString());
                    if (snapshot.child("colors").getValue() != null) mColors.setText(snapshot.child("colors").getValue().toString());
                    if (snapshot.child("rival").getValue() != null) mRival.setText(snapshot.child("rival").getValue().toString());
                    if (snapshot.child("honors").getValue() != null) mHonors.setText(snapshot.child("honors").getValue().toString());

                    if (snapshot.child("profileImageUrl").getValue() != null) {
                        String imageUrl = snapshot.child("profileImageUrl").getValue().toString();
                        if (!imageUrl.equals("default") && !isDestroyed()) {
                            // Added skipMemoryCache so Glide is forced to load the newest image!
                            Glide.with(TeamProfileActivity.this)
                                    .load(imageUrl)
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .into(mProfileImage);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}