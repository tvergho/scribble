package edu.dartmouth.cs.scribble.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import edu.dartmouth.cs.scribble.R;
import edu.dartmouth.cs.scribble.models.LeaderboardItem;
import edu.dartmouth.cs.scribble.models.Player;

public class LeaderboardAdapter extends ArrayAdapter<LeaderboardItem> {
    private Context context;
    private List<LeaderboardItem> players;

    public LeaderboardAdapter(@NonNull Context context, @NonNull List<LeaderboardItem> players) {
        super(context, 0);

        this.context = context;
        this.players = players;
    }

    @Override
    public int getCount() {
        return players.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LeaderboardItem player = players.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.leaderboard_item, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.name);
        TextView scoreView = convertView.findViewById(R.id.score);
        nameView.setText(player.getPlayerName());
        Log.d("leaders", player.getPlayerName());
        scoreView.setText("+" + player.getScore());

        return convertView;
    }
}
