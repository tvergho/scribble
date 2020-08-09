package edu.dartmouth.cs.scribble.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import edu.dartmouth.cs.scribble.R;
import edu.dartmouth.cs.scribble.models.Player;

public class ResultsAdapter extends ArrayAdapter<Player> {
    private Context context;
    private List<Player> players;

    public ResultsAdapter(@NonNull Context context, @NonNull List<Player> players) {
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
            convertView = LayoutInflater.from(context).inflate(R.layout.results_item, parent, false);
        }

        TextView placeView = convertView.findViewById(R.id.place);
        TextView nameView = convertView.findViewById(R.id.name);
        TextView scoreView = convertView.findViewById(R.id.score);

        int place = position + 1;

        placeView.setText(place + ".");
        nameView.setText(player.getPlayerName() + ":");
        scoreView.setText(String.valueOf(player.getScore()));

        return convertView;
    }
}
