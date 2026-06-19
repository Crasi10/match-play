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

public class ProfileActivity extends AppCompatActivity {

    private EditText mPosition, mAge, mCountry, mCareer, mFoot;
    private Button mSave, mBack;
    private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    private String userId;

    private Uri resultUri; // Holds the image before uploading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mPosition = findViewById(R.id.inputPosition);
        mAge = findViewById(R.id.inputAge);
        mCountry = findViewById(R.id.inputCountry);
        mCareer = findViewById(R.id.inputCareer);
        mFoot = findViewById(R.id.inputFoot);
        mSave = findViewById(R.id.btnSave);
        mBack = findViewById(R.id.btnBack);
        mProfileImage = findViewById(R.id.profileImage);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("player").child(userId);

        // Load existing data if they are editing their profile
        loadExistingData();

        // 1. Open Gallery when clicking the picture
        mProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        // 2. The SAVE Button Logic
        mSave.setOnClickListener(v -> saveUserInformation());

        // 3. The Cancel Button
        mBack.setOnClickListener(v -> {
            finish(); // Just closes the screen
        });
    }

    private void saveUserInformation() {
        String position = mPosition.getText().toString().trim();
        String age = mAge.getText().toString().trim();
        String country = mCountry.getText().toString().trim();
        String career = mCareer.getText().toString().trim();
        String foot = mFoot.getText().toString().trim();

        if (position.isEmpty() || age.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Please fill in Position, Age, and Country at minimum", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a loading spinner so they know it's working
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Profile...");
        progressDialog.show();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("position", position);
        userInfo.put("age", age);
        userInfo.put("country", country);
        userInfo.put("career", career);
        userInfo.put("foot", foot);
        userInfo.put("profileSetup", true); // CRITICAL: Tells the app they finished setup!

        // If they selected a new picture, upload it to Firebase Storage first
        if (resultUri != null) {
            StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profileImages").child(userId);
            filepath.putFile(resultUri).addOnSuccessListener(taskSnapshot -> filepath.getDownloadUrl().addOnSuccessListener(uri -> {
                userInfo.put("profileImageUrl", uri.toString());
                mUserDatabase.updateChildren(userInfo); // Save everything to database
                progressDialog.dismiss();
                goToMainApp();
            }));
        } else {
            // No new picture, just save the text data
            mUserDatabase.updateChildren(userInfo);
            progressDialog.dismiss();
            goToMainApp();
        }
    }

    private void goToMainApp() {
        Toast.makeText(ProfileActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Catches the image from the phone's gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            resultUri = data.getData();
            mProfileImage.setImageURI(resultUri); // Show it on screen instantly
        }
    }

    private void loadExistingData() {
        mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("position").getValue() != null) mPosition.setText(snapshot.child("position").getValue().toString());
                    if (snapshot.child("age").getValue() != null) mAge.setText(snapshot.child("age").getValue().toString());
                    if (snapshot.child("country").getValue() != null) mCountry.setText(snapshot.child("country").getValue().toString());
                    if (snapshot.child("career").getValue() != null) mCareer.setText(snapshot.child("career").getValue().toString());
                    if (snapshot.child("foot").getValue() != null) mFoot.setText(snapshot.child("foot").getValue().toString());

                    if (snapshot.child("profileImageUrl").getValue() != null) {
                        String imageUrl = snapshot.child("profileImageUrl").getValue().toString();
                        if (!imageUrl.equals("default") && !isDestroyed()) {
                            Glide.with(ProfileActivity.this).load(imageUrl).into(mProfileImage);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}