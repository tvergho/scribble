package edu.dartmouth.cs.scribble;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.Player;

public class EntryActivity extends AppCompatActivity {
    private Button startButton;
    private EditText gameCode;
    private EditText playerName;
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(openNextActivity);

        gameCode = findViewById(R.id.game_code);
        playerName = findViewById(R.id.player_name);

        // Initialize database.
        database = FirebaseDatabase.getInstance();
    }

    View.OnClickListener openNextActivity = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String gameID = gameCode.getText().toString();
            final String pName = playerName.getText().toString();
            myRef = database.getReference(gameID);

            if (verifyInput()) {
                // Check if the name is already taken.
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child(Constants.PLAYERS).child(pName).exists()) { // Display error.
                            playerName.setError("Name already taken.");
                            playerName.requestFocus();
                        }
                        else { // Start new activity.
                            Intent intent = new Intent();
                            if (!dataSnapshot.child(Constants.HAS_STARTED).exists()) { // Game hasn't started yet.
                                intent = new Intent(EntryActivity.this, PlayersActivity.class);
                            }
                            else {
                                intent = new Intent(EntryActivity.this, GameActivity.class);
                            }

                            intent.putExtra(Constants.GAME_ID, gameID);
                            intent.putExtra(Constants.PLAYER_NAME, pName);

                            Player player = new Player(pName, false);
                            myRef.child(Constants.PLAYERS).child(pName).setValue(player);

                            startActivity(intent);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

            }
        }
    };

    // Verify whether the user input is valid before starting the game.
    private boolean verifyInput() {
        String gameID = gameCode.getText().toString();
        String pName = playerName.getText().toString();
        boolean isValid = true;

        if (gameID.isEmpty()) {
            gameCode.setError("Required.");
            gameCode.requestFocus();
            isValid = false;
        }
        if (pName.isEmpty()) {
            playerName.setError("Required.");
            playerName.requestFocus();
            isValid = false;
        }

        return isValid;
    }
}
