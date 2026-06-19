package com.example.matchandplay;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolders> {

    private List<ChatObject> chatList;
    private Context context;
    private String myUserId;

    public ChatAdapter(List<ChatObject> matchesList, Context context) {
        this.chatList = matchesList;
        this.context = context;
        // Identify who the app is currently logged in as
        myUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ChatViewHolders onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        return new ChatViewHolders(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolders holder, int position) {
        holder.mMessage.setText(chatList.get(position).getMessage());

        // Dynamic UI formatting based on data ownership
        if (chatList.get(position).getCurrentUserId().equals(myUserId)) {
            // It's MY message -> Align Right, Pitch Green Background, White Text
            holder.mContainer.setGravity(Gravity.END);
            holder.mMessage.setBackgroundColor(Color.parseColor("#2E7D32"));
            holder.mMessage.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            // It's THEIR message -> Align Left, Light Grey Background, Black Text
            holder.mContainer.setGravity(Gravity.START);
            holder.mMessage.setBackgroundColor(Color.parseColor("#E0E0E0"));
            holder.mMessage.setTextColor(Color.parseColor("#000000"));
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public class ChatViewHolders extends RecyclerView.ViewHolder {
        public TextView mMessage;
        public LinearLayout mContainer;

        public ChatViewHolders(View itemView) {
            super(itemView);
            mMessage = itemView.findViewById(R.id.message);
            mContainer = itemView.findViewById(R.id.container);
        }
    }
}