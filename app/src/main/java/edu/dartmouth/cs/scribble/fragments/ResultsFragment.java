package edu.dartmouth.cs.scribble.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.dartmouth.cs.scribble.R;
import edu.dartmouth.cs.scribble.adapters.LeaderboardAdapter;
import edu.dartmouth.cs.scribble.adapters.ResultsAdapter;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.LeaderboardItem;
import edu.dartmouth.cs.scribble.models.Player;

public class ResultsFragment extends DialogFragment {
    ListView list;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View v = getActivity().getLayoutInflater().inflate(R.layout.leaderboard_fragment, null);
        list = v.findViewById(R.id.leaderboard_list);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        String gameId = bundle.getString(Constants.GAME_ID);
        final DatabaseReference gameRef = database.getReference(gameId);

        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Player> players = new ArrayList<>();

                for (DataSnapshot child : dataSnapshot.child(Constants.PLAYERS).getChildren()) {
                    Player player = child.getValue(Player.class);
                    players.add(player);
                }
                Collections.sort(players);
                setAdapter(players);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        builder.setTitle(R.string.leaderboard_title)
                .setView(v)
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        return builder.create();
    }

    private void setAdapter(List<Player> players) {
        if (getActivity() != null) {
            ResultsAdapter adapter = new ResultsAdapter(getActivity(), players);
            list.setAdapter(adapter);
        }
    }
}
