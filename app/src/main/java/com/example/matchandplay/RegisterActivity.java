package com.example.matchandplay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private Button mRegister, mBack;
    private EditText mEmail, mPassword, mName;
    private RadioGroup mRadioGroup;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Connect Java variables to the new XML IDs
        mRegister = findViewById(R.id.btnRegister);
        mBack = findViewById(R.id.btnBack);
        mEmail = findViewById(R.id.inputEmail);
        mPassword = findViewById(R.id.inputPassword);
        mName = findViewById(R.id.inputName);
        mRadioGroup = findViewById(R.id.radioGroup);

        mRegister.setOnClickListener(v -> {
            int selectId = mRadioGroup.getCheckedRadioButtonId();

            // Check if a radio button is actually selected
            if (selectId == -1) {
                Toast.makeText(RegisterActivity.this, "Please select Player or Team", Toast.LENGTH_SHORT).show();
                return;
            }

            final RadioButton radioButton = findViewById(selectId);

            // Trim removes accidental blank spaces the user might have typed
            final String email = mEmail.getText().toString().trim();
            final String password = mPassword.getText().toString().trim();
            final String name = mName.getText().toString().trim();

            // --- 1. Client-Side Checks ---
            if (name.isEmpty()) {
                mName.setError("Name is required");
                mName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                mEmail.setError("Email is required");
                mEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                mPassword.setError("Password is required");
                mPassword.requestFocus();
                return;
            }

            if (password.length() < 8) {
                mPassword.setError("Password must be at least 8 characters");
                mPassword.requestFocus();
                return;
            }

            // Requires at least one letter and one number
            String passwordPattern = "^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$";
            if (!password.matches(passwordPattern)) {
                mPassword.setError("Must contain at least 1 letter and 1 number");
                mPassword.requestFocus();
                return;
            }

            // --- 2. Firebase Server Checks ---
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, task -> {
                if (!task.isSuccessful()) {
                    // Find out EXACTLY what went wrong
                    try {
                        throw task.getException();
                    } catch (com.google.firebase.auth.FirebaseAuthUserCollisionException e) {
                        mEmail.setError("This email is already registered. Try logging in.");
                        mEmail.requestFocus();
                    } catch (com.google.firebase.auth.FirebaseAuthWeakPasswordException e) {
                        mPassword.setError("This password is too weak");
                        mPassword.requestFocus();
                    } catch (com.google.firebase.auth.FirebaseAuthInvalidCredentialsException e) {
                        mEmail.setError("This email format is invalid");
                        mEmail.requestFocus();
                    } catch (Exception e) {
                        // Fallback for network errors
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    String userId = mAuth.getCurrentUser().getUid();

                    // Determine if user is a "player" or "team" based on radio button text
                    String userType = "player";
                    if (radioButton.getText().toString().equalsIgnoreCase("Team")) {
                        userType = "team";
                    }

                    // Save initial info to Database
                    mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userType).child(userId);
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("name", name);
                    userInfo.put("type", userType);
                    userInfo.put("profileSetup", false); // Mark that they haven't finished profile yet
                    mDatabase.updateChildren(userInfo);

                    Toast.makeText(RegisterActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();

                    // Redirect to the correct Profile Setup page
                    if (userType.equals("player")) {
                        Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(RegisterActivity.this, TeamProfileActivity.class);
                        startActivity(intent);
                    }
                    finish();
                }
            });
        });

        mBack.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}