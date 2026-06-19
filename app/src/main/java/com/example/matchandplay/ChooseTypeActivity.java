package com.example.matchandplay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class ChooseTypeActivity extends AppCompatActivity {

    private Button mPlayer, mTeam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_type);

        mPlayer = findViewById(R.id.btnPlayer);
        mTeam = findViewById(R.id.btnTeam);

        mPlayer.setOnClickListener(v -> saveChoiceAndRedirect("player"));
        mTeam.setOnClickListener(v -> saveChoiceAndRedirect("team"));
    }

    private void saveChoiceAndRedirect(String type) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("Users").child(type).child(userId);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("type", type);
        userInfo.put("profileSetup", false); // They still need to do the profile

        db.updateChildren(userInfo);

        if (type.equals("player")) {
            Intent intent = new Intent(ChooseTypeActivity.this, ProfileActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(ChooseTypeActivity.this, TeamProfileActivity.class);
            startActivity(intent);
        }
        finish();
    }
}