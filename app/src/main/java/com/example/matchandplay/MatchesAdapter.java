package com.example.matchandplay;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {

    private List<User> matchesList;
    private Context context;

    public MatchesAdapter(List<User> matchesList, Context context) {
        this.matchesList = matchesList;
        this.context = context;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_matches, parent, false);
        return new MatchViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        User match = matchesList.get(position);

        holder.mMatchName.setText(match.getName());

        if (!match.getProfileImageUrl().equals("default")) {
            Glide.with(context).load(match.getProfileImageUrl()).into(holder.mMatchImage);
        }

        // When you click a person in the list, open the Chat!
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            // We pass the other person's ID so the Chat screen knows who we are talking to
            intent.putExtra("matchId", match.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return matchesList.size();
    }

    // This links to your item_matches.xml UI elements
    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        public TextView mMatchName;
        public ImageView mMatchImage;

        public MatchViewHolder(View itemView) {
            super(itemView);
            mMatchName = itemView.findViewById(R.id.MatchName);
            mMatchImage = itemView.findViewById(R.id.MatchImage);
        }
    }
}