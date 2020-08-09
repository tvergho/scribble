package edu.dartmouth.cs.scribble.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import edu.dartmouth.cs.scribble.GameActivity;
import edu.dartmouth.cs.scribble.R;
import edu.dartmouth.cs.scribble.adapters.LeaderboardAdapter;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.LeaderboardItem;

public class EndTurnFragment extends DialogFragment {
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
                List<LeaderboardItem> leaders = new ArrayList<>();

                for (DataSnapshot child : dataSnapshot.child(Constants.TURN_SCORE).getChildren()) {
                    if (!child.getKey().equals(Constants.MAX_TURN_SCORE)) {
                        LeaderboardItem player = new LeaderboardItem(child.getKey(), (long) child.getValue());
                        leaders.add(player);
                    }
                }
                Collections.sort(leaders);
                setAdapter(leaders);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        builder.setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.next_round, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.hasChild(Constants.GAME_ENDED) && (boolean)dataSnapshot.child(Constants.GAME_ENDED).getValue()) {
                                    ((GameActivity)getActivity()).endGame();
                                }
                                else if (!(boolean)dataSnapshot.child(Constants.TURN_STARTED).getValue()){
                                    gameRef.child(Constants.NEXT_TURN).setValue(true);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                })
                .setView(v);
        return builder.create();
    }

    private void setAdapter(List<LeaderboardItem> leaders) {
        if (getActivity() != null) {
            LeaderboardAdapter adapter = new LeaderboardAdapter(getActivity(), leaders);
            list.setAdapter(adapter);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        /*
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        String gameId = bundle.getString(Constants.GAME_ID);
        final DatabaseReference gameRef = database.getReference(gameId);

        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(Constants.GAME_ENDED) && (boolean)dataSnapshot.child(Constants.GAME_ENDED).getValue()) {
                    ((GameActivity)getActivity()).endGame();
                }
                else if (!(boolean)dataSnapshot.child(Constants.TURN_STARTED).getValue()){
                    gameRef.child(Constants.NEXT_TURN).setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });*/
    }
}
