package edu.dartmouth.cs.scribble;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.dartmouth.cs.scribble.adapters.PlayersAdapter;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.Player;

public class PlayersActivity extends AppCompatActivity {
    private String gameID;
    private List<Player> players = new ArrayList<>();
    private ListView playerListView;
    private PlayersAdapter adapter;
    private DatabaseReference myRef;
    private FirebaseDatabase database;
    private Button startButton, aiButton;
    private TextView gameCodeView;
    private Player aiPlayer;
    private Player thisPlayer;
    private boolean isStarting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players);

        // Set up the list of players.
        playerListView = findViewById(R.id.leaderboard_list);
        adapter = new PlayersAdapter(this, players);
        playerListView.setAdapter(adapter);

        // Set up buttons.
        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(openGameActivity);

        aiButton = findViewById(R.id.ai_button);
        aiButton.setOnClickListener(newAI);

        gameCodeView = findViewById(R.id.game_code);

        // Get the game ID and set up Firebase.
        if (getIntent().hasExtra(Constants.GAME_ID)) {
            gameID = getIntent().getStringExtra(Constants.GAME_ID);
            gameCodeView.setText("Game Code: " + gameID);
        }
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference(gameID); // Sets the current reference to our game.
        myRef.addValueEventListener(databaseListener);
    }

    // Database event listener.
    ValueEventListener databaseListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            players.clear();
            enableAIButton();

            // Re-populates the player list with the new children.
            for (DataSnapshot child : dataSnapshot.child(Constants.PLAYERS).getChildren()) {
                Player player = child.getValue(Player.class);
                players.add(player);

                if (player.isAI()) { // Only allow one AI.
                    disableAIButton();
                }
                if (player.getPlayerName().equals(getIntent().getStringExtra(Constants.PLAYER_NAME))) {
                    thisPlayer = player;
                }
            }

            // Don't start the game with only one player.
            if (players.size() <= 1) {
                disableMainButton();
            }
            else {
                enableMainButton();
            }

            adapter.notifyDataSetChanged();

            // Start the game if one player has started it.
            if (dataSnapshot.child(Constants.HAS_STARTED).getValue() != null &&
                    (boolean)dataSnapshot.child(Constants.HAS_STARTED).getValue()) {
                startGameActivity();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w("Error", databaseError.toException());
        }
    };

    View.OnClickListener openGameActivity = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Randomly designate a drawer (not the AI).
            Random r = new Random();
            if (players.size() > 1) {
                int randIndex = r.nextInt(players.size());
                while (players.get(randIndex).getPlayerName().equals(Constants.AI_NAME)) {
                    randIndex = r.nextInt(players.size());
                }
                Player randomPlayer = players.get(randIndex);
                myRef.child(Constants.PLAYERS).child(randomPlayer.getPlayerName()).child(Constants.IS_DRAWING).setValue(true);
                myRef.child(Constants.ARTIST_NAME).setValue(randomPlayer.getPlayerName());
                myRef.child(Constants.ALREADY_ARTIST).child(randomPlayer.getPlayerName()).setValue(true);
                myRef.child(Constants.PRES_TURNS).setValue(0);

                // Tells Firebase that a game is starting.
                myRef.child(Constants.HAS_STARTED).setValue(true);
                myRef.child(Constants.TURN).setValue(1);

                // Starts the game activity.
                startGameActivity();
            }
        }
    };

    View.OnClickListener newAI = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Creates a new AI character.
            Player player = new Player(Constants.AI_NAME, true);
            myRef.child(Constants.PLAYERS).child(Constants.AI_NAME).setValue(player);
            aiPlayer = player; // Make this device responsible for the AI player.

            // Disable the button.
            disableAIButton();
        }
    };

    // Starts a new game activity.
    private void startGameActivity() {
        // Load whether the player is the current artist and start the activity.
        myRef.child(Constants.PLAYERS).child(thisPlayer.getPlayerName()).child(Constants.IS_DRAWING).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                isStarting = true; // Means player won't be destroyed in onDestroy.

                Intent intent = new Intent(PlayersActivity.this, GameActivity.class);
                intent.putExtra(Constants.GAME_ID, gameID);
                intent.putExtra(Constants.PLAYER_NAME, thisPlayer.getPlayerName());

                // Have the AI manager set up Firebase for the AI player.
                if (aiPlayer != null) {
                    intent.putExtra(Constants.AI_MANAGER, true);
                    myRef.child(Constants.ALREADY_ARTIST).child(Constants.AI_NAME).setValue(true);
                }

                if ((boolean)dataSnapshot.getValue()) {
                    intent.putExtra(Constants.IS_DRAWING, true);
                }
                else {
                    intent.putExtra(Constants.IS_DRAWING, false);
                }

                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // Button state helpers.
    private void disableAIButton() {
        aiButton.setEnabled(false);
        aiButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.AI_DISABLED)));
    }

    private void enableAIButton() {
        aiButton.setEnabled(true);
        aiButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.AI_ENABLED)));
    }

    private void disableMainButton() {
        startButton.setEnabled(false);
        startButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.MAIN_DISABLED)));
    }

    private void enableMainButton() {
        startButton.setEnabled(true);
        startButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.MAIN_ENABLED)));
    }

    @Override
    protected void onDestroy() {
        myRef.removeEventListener(databaseListener);
        if (!isStarting) {
            if (thisPlayer != null) myRef.child(Constants.PLAYERS).child(thisPlayer.getPlayerName()).removeValue();
            if (aiPlayer != null) myRef.child(Constants.PLAYERS).child(aiPlayer.getPlayerName()).removeValue();
        }
        super.onDestroy();
    }
}
