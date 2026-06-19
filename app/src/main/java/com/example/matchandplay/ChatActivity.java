package com.example.matchandplay;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mChatAdapter;
    private RecyclerView.LayoutManager mChatLayoutManager;
    private EditText mSendEditText;
    private Button mSendButton, mBack;
    private TextView mName;

    private String currentUserID, matchId, chatId;
    private DatabaseReference mDatabaseChat, mDatabaseUser;

    private List<ChatObject> resultsChat = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Make sure this matches your XML file name

        // 1. Get the Match ID passed from the previous screen
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            matchId = extras.getString("matchId");
        }

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 2. Setup UI Elements
        mSendEditText = findViewById(R.id.inputMessage);
        mSendButton = findViewById(R.id.btnSend);
        mBack = findViewById(R.id.btnBack); // FIND THE BUTTON

        mName = findViewById(R.id.txtChatName);
        mName.setText("Chat");

        // 3. FORCE THE BACK BUTTON TO WORK
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // This closes the chat and goes back to Matches
            }
        });

        // 4. Setup RecyclerView
        mRecyclerView = findViewById(R.id.recyclerViewChat);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setHasFixedSize(false);
        mChatLayoutManager = new LinearLayoutManager(ChatActivity.this);
        mRecyclerView.setLayoutManager(mChatLayoutManager);

        // MODIFICATION: Added "ChatActivity.this" to match the ChatAdapter constructor
        mChatAdapter = new ChatAdapter(resultsChat, ChatActivity.this);
        mRecyclerView.setAdapter(mChatAdapter);

        mSendButton.setOnClickListener(v -> sendMessage());

        // 5. Start loading messages
        checkUserType();
    }

    private void sendMessage() {
        String sendMessageText = mSendEditText.getText().toString();
        if(!sendMessageText.isEmpty() && chatId != null){
            DatabaseReference newMessageDb = mDatabaseChat.push();
            Map<String, Object> map = new HashMap<>();

            // MODIFICATION: Changed keys to match ChatObject exactly
            map.put("currentUserId", currentUserID);
            map.put("message", sendMessageText);

            newMessageDb.setValue(map);
            mSendEditText.setText(null);
        }
    }

    private void checkUserType() {
        // Try finding the match in the "player" folder first
        DatabaseReference pDb = FirebaseDatabase.getInstance().getReference().child("Users").child("player").child(currentUserID).child("connections").child("matches").child(matchId);
        pDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("ChatId").getValue() != null) {
                    chatId = snapshot.child("ChatId").getValue().toString();
                    mDatabaseChat = FirebaseDatabase.getInstance().getReference().child("Chat").child(chatId);
                    getChatMessages();
                } else {
                    // Try "team" folder
                    DatabaseReference tDb = FirebaseDatabase.getInstance().getReference().child("Users").child("team").child(currentUserID).child("connections").child("matches").child(matchId);
                    tDb.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && snapshot.child("ChatId").getValue() != null) {
                                chatId = snapshot.child("ChatId").getValue().toString();
                                mDatabaseChat = FirebaseDatabase.getInstance().getReference().child("Chat").child(chatId);
                                getChatMessages();
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void getChatMessages() {
        mDatabaseChat.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String s) {
                if(snapshot.exists()){
                    String messageText = "";
                    String senderId = "";

                    // MODIFICATION: Retrieving the exact keys we saved ("message" and "currentUserId")
                    if(snapshot.child("message").getValue() != null) {
                        messageText = snapshot.child("message").getValue().toString();
                    }
                    if(snapshot.child("currentUserId").getValue() != null) {
                        senderId = snapshot.child("currentUserId").getValue().toString();
                    }

                    if(!messageText.isEmpty() && !senderId.isEmpty()){
                        // MODIFICATION: Creating ChatObject with the correct variables
                        resultsChat.add(new ChatObject(messageText, senderId));
                        mChatAdapter.notifyDataSetChanged();

                        // Keep the chat scrolled to the bottom
                        mRecyclerView.scrollToPosition(resultsChat.size() - 1);
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}