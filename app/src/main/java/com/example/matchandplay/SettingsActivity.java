package com.example.matchandplay;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsActivity extends AppCompatActivity {

    private Button btnBack, btnResetPassword, btnLogout, btnDeleteAccount;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btnBack);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        btnBack.setOnClickListener(v -> finish());

        // 1. Password Reset Logic
        btnResetPassword.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                mAuth.sendPasswordResetEmail(user.getEmail()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // 2. Standard Log Out Logic
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 3. Delete Account Logic (With a safety warning popup!)
        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account?")
                    .setMessage("Are you absolutely sure? This will wipe your profile and matches forever.")
                    .setPositiveButton("DELETE", (dialog, which) -> deleteUserAccount())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Erasing Data...");
            progressDialog.show();

            // First, delete the user's data from the Realtime Database
            DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("Users");
            db.child("player").child(uid).removeValue(); // Tries to delete if they are a player
            db.child("team").child(uid).removeValue();   // Tries to delete if they are a team

            // Then, delete their actual Authentication account
            user.delete().addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(SettingsActivity.this, "Account successfully deleted.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SettingsActivity.this, RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to delete: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}