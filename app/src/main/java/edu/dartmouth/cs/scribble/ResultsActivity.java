package edu.dartmouth.cs.scribble;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.dartmouth.cs.scribble.adapters.PlayersAdapter;
import edu.dartmouth.cs.scribble.adapters.ResultsAdapter;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.Player;

public class ResultsActivity extends AppCompatActivity {
    private String gameID;
    private List<Player> players = new ArrayList<>();
    private ListView playerListView;
    private ResultsAdapter adapter;
    private DatabaseReference myRef;
    private FirebaseDatabase database;
    private Button backButton;
    private String thisPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Set up the list of players and views.
        playerListView = findViewById(R.id.leaderboard_list);
        adapter = new ResultsAdapter(this, players);
        playerListView.setAdapter(adapter);
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(mainButtonOnClick);

        // Get the game ID and set up Firebase.
        if (getIntent().hasExtra(Constants.GAME_ID)) {
            gameID = getIntent().getStringExtra(Constants.GAME_ID);
        }
        if (getIntent().hasExtra(Constants.PLAYER_NAME)) {
            thisPlayer = getIntent().getStringExtra(Constants.PLAYER_NAME);
        }
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference(gameID); // Sets the current reference to our game.
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.child(Constants.PLAYERS).getChildren()) {
                    // Get all the players and their scores.
                    Player player = child.getValue(Player.class);
                    players.add(player);
                }
                Collections.sort(players);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Error", databaseError.toException());
            }
        });
    }

    View.OnClickListener mainButtonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(ResultsActivity.this, EntryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    };

    @Override
    protected void onDestroy() {
        myRef.child(Constants.PLAYERS).child(thisPlayer).child(Constants.SIGNED_OUT).setValue(true);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean allSignedOut = true; // Delete the game if all players have left to free up the game ID.
                for (DataSnapshot child : dataSnapshot.child(Constants.PLAYERS).getChildren()) {
                    if (!child.hasChild(Constants.SIGNED_OUT) || !(boolean)child.child(Constants.SIGNED_OUT).getValue()) {
                        allSignedOut = false;
                    }
                }
                if (allSignedOut) {
                    myRef.removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        super.onDestroy();
    }
}
