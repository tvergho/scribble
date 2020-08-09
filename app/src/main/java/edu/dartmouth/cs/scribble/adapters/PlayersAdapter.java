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
import edu.dartmouth.cs.scribble.models.Player;

public class PlayersAdapter extends ArrayAdapter<Player> {
    private Context context;
    private List<Player> players;

    public PlayersAdapter(@NonNull Context context, @NonNull List<Player> players) {
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
        Player player = players.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.player_item, parent, false);
        }

        if (player.isAI()) {
            ImageView iconView = convertView.findViewById(R.id.icon);
            iconView.setImageDrawable(context.getDrawable(R.drawable.ic_robot_solid));
        }

        TextView nameView = convertView.findViewById(R.id.name);
        nameView.setText(player.getPlayerName());

        return convertView;
    }
}
