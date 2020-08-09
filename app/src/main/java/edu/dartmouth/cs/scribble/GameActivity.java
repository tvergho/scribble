package edu.dartmouth.cs.scribble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import edu.dartmouth.cs.scribble.adapters.GuessAdapter;
import edu.dartmouth.cs.scribble.fragments.EndTurnFragment;
import edu.dartmouth.cs.scribble.fragments.ResultsFragment;
import edu.dartmouth.cs.scribble.ml.SketchPredictor;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.Guess;
import edu.dartmouth.cs.scribble.models.LeaderboardItem;
import edu.dartmouth.cs.scribble.models.Player;
import edu.dartmouth.cs.scribble.views.DrawingView;

public class GameActivity extends AppCompatActivity {
    private DrawingView dv;
    private Button trashButton, editButton, eraserButton, leaderboardButton;
    private MaterialButton blackButton, redButton, orangeButton, yellowButton,
            greenButton, blueButton, purpleButton, brownButton;
    private TextView currentWordView, timerView, turnView;
    private ListView guessesView;
    private GuessAdapter adapter;
    private Button guessButton;
    private TextInputLayout guessTextLayout;
    private EditText guessText;
    private ConstraintLayout editPane;
    private FrameLayout drawLayout;
    private ConstraintLayout layout;
    private ArrayList<MaterialButton> buttons;
    private String selectedColor, gameId, playerName, curWord, hints;
    private int numHints = 0;
    private List<Guess> guesses;
    private Set<Long> guessIds;
    private FirebaseDatabase database;
    private DatabaseReference gameRef;
    private Handler timerHandler, aiHandler;
    private long secondsLeft, totalScore;
    private boolean turnStarted = false;
    private boolean timerStarted = false;
    private boolean dialogDisplayed = false;
    private boolean isEnding = false;
    private boolean isAIManager = false;
    private boolean aiStarted = false;
    private boolean aiCorrect = false;
    private long turnNumber = 1;
    private EndTurnFragment endTurnFragment;
    private Runnable timerRunnable;
    private SketchPredictor sketchPredictor;

    public static boolean isErasing = false;
    public static boolean isDrawing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        isDrawing = getIntent().getBooleanExtra(Constants.IS_DRAWING, false);
        database = FirebaseDatabase.getInstance();

        // Set up the drawing view and Firebase.
        gameId = getIntent().getStringExtra(Constants.GAME_ID);
        playerName = getIntent().getStringExtra(Constants.PLAYER_NAME);
        dv = findViewById(R.id.draw_view);
        dv.initializeDatabase(gameId);
        gameRef = database.getReference(gameId);
        gameRef.addValueEventListener(gameListener);

        // Sets up AI player.
        if (getIntent().hasExtra(Constants.AI_MANAGER) && getIntent().getBooleanExtra(Constants.AI_MANAGER, false)) {
            isAIManager = true;
            sketchPredictor = new SketchPredictor(this);
            aiHandler = new Handler(Looper.getMainLooper());
        }

        database.getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Return to main screen if the game ID is invalid.
                if (!dataSnapshot.hasChild(gameId)) {
                    Intent intent = new Intent(GameActivity.this, EntryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                // Sync an incoming player with the current time.
                else if (dataSnapshot.child(gameId).hasChild(Constants.TURN_STARTED) &&
                        (boolean)dataSnapshot.child(gameId).child(Constants.TURN_STARTED).getValue()) { // Turn has started.
                    if (dataSnapshot.child(gameId).hasChild(Constants.CURRENT_TIME)) {
                        secondsLeft = (long)dataSnapshot.child(gameId).child(Constants.CURRENT_TIME).getValue();
                    }
                }
                Log.d("seconds left", String.valueOf(secondsLeft));
                if (secondsLeft == 0 || secondsLeft > 57) {
                    // Initialize the turn scores.
                    for (DataSnapshot child : dataSnapshot.child(gameId).child(Constants.PLAYERS).getChildren()) {
                        String indivName = child.getValue(Player.class).getPlayerName();
                        gameRef.child(Constants.TURN_SCORE).child(indivName).setValue(0);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // Initialize the views.
        blackButton = findViewById(R.id.black);
        blueButton = findViewById(R.id.blue);
        redButton = findViewById(R.id.red);
        orangeButton = findViewById(R.id.orange);
        yellowButton = findViewById(R.id.yellow);
        greenButton = findViewById(R.id.green);
        blueButton = findViewById(R.id.blue);
        purpleButton = findViewById(R.id.purple);
        brownButton = findViewById(R.id.brown);
        trashButton = findViewById(R.id.trash);
        editButton = findViewById(R.id.edit);
        eraserButton = findViewById(R.id.eraser);
        guessTextLayout = findViewById(R.id.guess_layout);
        guessButton = findViewById(R.id.guess_button);
        editPane = findViewById(R.id.edit_pane);
        drawLayout = findViewById(R.id.draw_layout);
        layout = findViewById(R.id.game_layout);
        currentWordView = findViewById(R.id.current_word);
        timerView = findViewById(R.id.timer);
        guessText = findViewById(R.id.guess);
        turnView = findViewById(R.id.turn_number);
        leaderboardButton = findViewById(R.id.leaderboard_button);

        buttons = new ArrayList<>(Arrays.asList(blackButton, redButton, orangeButton, yellowButton,
                greenButton, blueButton, purpleButton, brownButton));
        leaderboardButton.setOnClickListener(onTurnClick);
        guesses = new ArrayList<>();
        guessIds = new HashSet<>();

        // Set up the guessing view.
        guessesView = findViewById(R.id.guesses_list);
        adapter = new GuessAdapter(this, guesses);
        guessesView.setAdapter(adapter);

        timerHandler = new Handler(Looper.getMainLooper());

        if (isDrawing) {
            setUpForDrawing();
            // secondsLeft = 0;
            startTurn(); // The artist is responsible for starting everyone's turn.
        }
        else {
            setUpForGuessing();
        }
    }

    // AI functionality
    @Override
    protected void onResume() {
        if (isAIManager) LocalBroadcastManager.getInstance(this).registerReceiver(aiReceiver, new IntentFilter(Constants.BROADCAST_GUESS));
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (isAIManager) LocalBroadcastManager.getInstance(this).unregisterReceiver(aiReceiver);
        super.onPause();
    }

    // Called every few seconds for the AI to make a prediction.
    Runnable aiRunnable = new Runnable() {
        @Override
        public void run() {
            if (turnStarted && !aiCorrect) {
                Log.d("airunnable", "gameactivity");
                if (sketchPredictor.isInitialized()) dv.makePrediction(sketchPredictor);
                aiHandler.postDelayed(this, 4000);
            }
        }
    };

    // Submits a guess as the AI player.
    BroadcastReceiver aiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("airunnable", "aireceiver");
            Date date = new Date();
            long curTime = date.getTime();
            Guess guess = new Guess(Constants.AI_NAME, intent.getStringExtra(Constants.GUESS_KEY), curWord, curTime);
            gameRef.child(Constants.GUESSES).child(String.valueOf(curTime)).setValue(guess);

            // On a correct guess, end that turn for the user.
            if (guess.getGuessType() == Constants.GUESS_TYPE_CORRECT) {
                aiCorrect = true;
                score(true);
            }
        }
    };

    View.OnClickListener onTurnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ResultsFragment resultsFragment = new ResultsFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.GAME_ID, gameId);
            resultsFragment.setArguments(bundle);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(resultsFragment, "results");
            ft.commitAllowingStateLoss();
        }
    };

    View.OnClickListener onTrashClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dv.clearCanvas();
        }
    };

    View.OnClickListener onEditEraseClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.edit) {
                setIsNotErasing();
            }
            else {
                setIsErasing();
            }
        }
    };

    View.OnClickListener onGuessClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String guessString = guessText.getText().toString();
            if (guessString.trim().length() > 0) { // Don't do anything for an empty string.
                // Post a new Guess to Firebase.
                Date date = new Date();
                long curTime = date.getTime();
                Guess guess = new Guess(playerName, guessString, curWord, curTime);
                gameRef.child(Constants.GUESSES).child(String.valueOf(curTime)).setValue(guess);

                // On a correct guess, end that turn for the user.
                if (guess.getGuessType() == Constants.GUESS_TYPE_CORRECT) {
                    guessButton.setEnabled(false);
                    score(false);
                }
            }
            guessText.setText("");
        }
    };

    ValueEventListener gameListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            // End the turn if all players have answered correctly and we're not setting up for the next turn.
            Log.d("early", dataSnapshot.hasChild(Constants.TURN_SCORE) + " " + turnStarted + " " +
                    (!dataSnapshot.hasChild(Constants.NEXT_TURN) || !(boolean)dataSnapshot.child(Constants.NEXT_TURN).getValue()));
            if (dataSnapshot.hasChild(Constants.TURN_SCORE) && turnStarted &&
                    (!dataSnapshot.hasChild(Constants.NEXT_TURN) || !(boolean)dataSnapshot.child(Constants.NEXT_TURN).getValue())) {
                boolean isUnanswered = false;
                // Cycles through all of the current turn scores to see if any are still 0.
                for (DataSnapshot child : dataSnapshot.child(Constants.TURN_SCORE).getChildren()) {
                    if ((long)child.getValue() == 0) isUnanswered = true;
                    Log.d("early", child.getKey() + " " + child.getValue());
                }
                if (!isUnanswered) {
                    gameRef.child(Constants.TURN_STARTED).setValue(false);
                    endTurn();
                    return;
                }
            }

            // Handle adding guesses to the list, in the order they were made.
            if (dataSnapshot.hasChild(Constants.GUESSES) && dataSnapshot.child(Constants.GUESSES).getChildrenCount() > guesses.size()) {
                List<Guess> newGuesses = new ArrayList<>();
                Log.d("new guess", String.valueOf(guesses.size()));

                // To make sorting more efficient, only the new guesses are sorted and added to an already sorted list.
                for (DataSnapshot child : dataSnapshot.child(Constants.GUESSES).getChildren()) {
                    Guess guess = child.getValue(Guess.class);
                    if (!guessIds.contains(guess.getTime())) {
                        Log.d("new guess", guess.getGuess());
                        newGuesses.add(guess);
                        guessIds.add(guess.getTime());
                    }
                }

                Collections.sort(newGuesses);
                guesses.addAll(newGuesses);
                adapter.notifyDataSetChanged();
            }

            // Set up next turn.
            if (dataSnapshot.hasChild(Constants.NEXT_TURN) && (boolean)dataSnapshot.child(Constants.NEXT_TURN).getValue() && !turnStarted) {
                setUpNextTurn();
            }

            // Sync the current turn number and set up the view for the next turn.
            if (turnNumber != (long)dataSnapshot.child(Constants.TURN).getValue()) {
                turnNumber = (long)dataSnapshot.child(Constants.TURN).getValue();
                turnView.setText(String.valueOf(turnNumber));
            }

            // Handle starting a new turn.
            if (!turnStarted && dataSnapshot.hasChild(Constants.TURN_STARTED) && (boolean)dataSnapshot.child(Constants.TURN_STARTED).getValue()) {
                turnStarted = true;
                if (endTurnFragment != null) {
                    endTurnFragment.dismiss();
                    dialogDisplayed = false;
                }

                if (!dataSnapshot.child(Constants.PLAYERS).child(playerName).getValue(Player.class).isDrawing()) {
                    setUpForGuessing();
                    isDrawing = false;
                }

                else if (dataSnapshot.child(Constants.PLAYERS).child(playerName).getValue(Player.class).isDrawing()) {
                    setUpForDrawing();
                    isDrawing = true;
                }

                secondsLeft = 0;
                startTurn();
            }

            // Set the current word and hints.
            if (dataSnapshot.hasChild(Constants.CURRENT_WORD) && !dataSnapshot.child(Constants.CURRENT_WORD).getValue().equals(curWord) && turnStarted) {
                Log.d("method", "curword" + isDrawing + dataSnapshot.child(Constants.PLAYERS).child(playerName).getValue(Player.class).isDrawing());
                // Set up the hint string.
                if (!isDrawing && !dataSnapshot.child(Constants.PLAYERS).child(playerName).getValue(Player.class).isDrawing()) {

                    curWord = (String) dataSnapshot.child(Constants.CURRENT_WORD).getValue();
                    Log.d("method", "curword" + curWord);
                    hints = "";
                    for (char c : curWord.toCharArray()) {
                        if (c == ' ') hints += ' ';
                        else hints += '_';
                    }
                    currentWordView.setText(hints);
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w("Database error", databaseError.toException());
        }
    };

    // Update all the Firebase variables for the next turn.
    private void setUpNextTurn() {
        Log.d("method", "setUpNextTurn()");
        if (isDrawing) { // Only last turn's artist is responsible for setting up for the next turn.
            gameRef.child(Constants.NEXT_TURN).setValue(false);
            gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // New turn.
                    if (dataSnapshot.child(Constants.ALREADY_ARTIST).getChildrenCount() >= dataSnapshot.child(Constants.PLAYERS).getChildrenCount()) {
                        gameRef.child(Constants.TURN).setValue(turnNumber + 1);

                        // Only select a new artist if the game isn't over.
                        if (turnNumber + 1 <= Constants.MAX_TURNS) {
                            Iterator<DataSnapshot> players = dataSnapshot.child(Constants.PLAYERS).getChildren().iterator();
                            Player player = players.next().getValue(Player.class);

                            // Don't let AI be the artist.
                            if (player.getPlayerName().equals(Constants.AI_NAME)) {
                                player = players.next().getValue(Player.class);
                            }

                            gameRef.child(Constants.PLAYERS).child(player.getPlayerName()).child(Constants.IS_DRAWING).setValue(true);
                            gameRef.child(Constants.ARTIST_NAME).setValue(player.getPlayerName());
                            gameRef.child(Constants.ALREADY_ARTIST).child(player.getPlayerName()).setValue(true);

                            while (players.hasNext()) {
                                player = players.next().getValue(Player.class);
                                gameRef.child(Constants.PLAYERS).child(player.getPlayerName()).child(Constants.IS_DRAWING).setValue(false);
                                if (!player.getPlayerName().equals(Constants.AI_NAME)) {
                                    gameRef.child(Constants.ALREADY_ARTIST).child(player.getPlayerName()).removeValue();
                                }
                            }
                        }
                        else { // Designate the game as ended.
                            gameRef.child(Constants.GAME_ENDED).setValue(true);
                            endGame();
                            return;
                        }
                    }

                    else { // Designate new artist.
                        Iterator<DataSnapshot> players = dataSnapshot.child(Constants.PLAYERS).getChildren().iterator();
                        Player player = players.next().getValue(Player.class);
                        while (dataSnapshot.child(Constants.ALREADY_ARTIST).hasChild(player.getPlayerName())) {
                            player = players.next().getValue(Player.class);
                        }
                        gameRef.child(Constants.PLAYERS).child(player.getPlayerName()).child(Constants.IS_DRAWING).setValue(true);
                        gameRef.child(Constants.ARTIST_NAME).setValue(player.getPlayerName());
                        gameRef.child(Constants.ALREADY_ARTIST).child(player.getPlayerName()).setValue(true);

                        for (DataSnapshot child : dataSnapshot.child(Constants.PLAYERS).getChildren()) {
                            if (!child.getValue(Player.class).getPlayerName().equals(player.getPlayerName())) {
                                gameRef.child(Constants.PLAYERS).child(child.getValue(Player.class).getPlayerName()).child(Constants.IS_DRAWING).setValue(false);
                            }
                        }
                    }

                    // Reset the turn's scores.
                    for (DataSnapshot child : dataSnapshot.child(Constants.TURN_SCORE).getChildren()) {
                        gameRef.child(Constants.TURN_SCORE).child(child.getKey()).setValue(0);
                    }
                    gameRef.child(Constants.TURN_STARTED).setValue(true);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    // Open ResultsActivity to end the game.
    public void endGame() {
        gameRef.removeEventListener(gameListener);
        timerHandler.removeCallbacks(timerRunnable);
        if (aiRunnable != null && aiHandler != null) aiHandler.removeCallbacks(aiRunnable);

        isEnding = true;
        Intent intent = new Intent(GameActivity.this, ResultsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.GAME_ID, gameId);
        intent.putExtra(Constants.PLAYER_NAME, playerName);
        startActivity(intent);
        finish();
    }

    // Starts the timer.
    private void startTurn() {
        secondsLeft = Constants.GAME_LENGTH;
        turnStarted = true;
        Log.d("method", "startTurn()");

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Tell Firebase to start a new turn.
                if (isDrawing && secondsLeft == Constants.GAME_LENGTH) {
                    gameRef.child(Constants.TURN_STARTED).setValue(true);
                }

                // Converts seconds to mm:ss.
                int min = (int)Math.floor((secondsLeft / 60.0) % 60);
                int sec = (int)secondsLeft - (min * 60);
                if (sec < 10 && sec >= 0 && min >= 0 && secondsLeft >= 0) {
                    timerView.setText(min + ":" + "0" + sec);
                }
                else if (sec >= 10 && secondsLeft >= 0) {
                    timerView.setText(min + ":" + sec);
                }

                secondsLeft--;

                // The Firebase time is synced to the artist's time.
                if (isDrawing) {
                    gameRef.child(Constants.CURRENT_TIME).setValue(secondsLeft);
                }

                if (secondsLeft >= 0) {
                    timerHandler.postDelayed(this, 1000);
                    timerStarted = true;
                    addHint();
                }
                else {
                    timerHandler.removeCallbacks(this);
                    timerStarted = false;
                    if (isDrawing) {
                        // End the turn for everyone.
                        gameRef.child(Constants.TURN_STARTED).setValue(false);
                    }
                    endTurn();
                }
            }
        };

        if (!timerStarted) {
            timerHandler.post(timerRunnable);
            timerStarted = true;
        }
        if (isAIManager && !aiStarted) {
            aiHandler.post(aiRunnable);
            aiStarted = true;
        }
    }

    // Called to calculate the current score, and add it to the turn's score and leaderboard.
    private void score(boolean isAI) {
        String pName = "";
        if (isAI) pName = Constants.AI_NAME;
        else pName = playerName;

        final long turnScore = secondsLeft * 10;
        totalScore += turnScore;
        gameRef.child(Constants.PLAYERS).child(pName).child("score").setValue(totalScore);
        gameRef.child(Constants.TURN_SCORE).child(pName).setValue(turnScore);

        // Update the artist's score if this is the max score.
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child(Constants.TURN_SCORE).hasChild(Constants.MAX_TURN_SCORE) ||
                        (long)dataSnapshot.child(Constants.TURN_SCORE).child(Constants.MAX_TURN_SCORE).getValue() < turnScore) {

                    // Get a reference to the artist and their current score.
                    String artistName = (String)dataSnapshot.child(Constants.ARTIST_NAME).getValue();
                    Player artist = dataSnapshot.child(Constants.PLAYERS).child(artistName).getValue(Player.class);
                    gameRef.child(Constants.PLAYERS).child(artistName).child("score").setValue(artist.getScore() + turnScore - 10);
                    gameRef.child(Constants.TURN_SCORE).child(artistName).setValue(turnScore - 10);
                    gameRef.child(Constants.TURN_SCORE).child(Constants.MAX_TURN_SCORE).setValue(turnScore);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // Called to end the turn.
    private void endTurn() {
        Log.d("method", "endTurn()");
        gameRef.child(Constants.TURN_STARTED).setValue(false, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                turnStarted = false; // Keeps this state synced to the database, prevents async issues.
            }
        });
        currentWordView.setText(curWord.toUpperCase());
        timerHandler.removeCallbacks(timerRunnable);
        timerStarted = false;

        if (isAIManager) {
            aiHandler.removeCallbacks(aiRunnable);
            aiStarted = false;
            aiCorrect = false;
            sketchPredictor.clearAlreadyGuessed();
        }

        if (!dialogDisplayed) {
            dialogDisplayed = true;
            endTurnFragment = new EndTurnFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.GAME_ID, gameId);
            endTurnFragment.setArguments(bundle);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(endTurnFragment, "end_turn");
            ft.commitAllowingStateLoss();
        }
    }

    // Calculates and adds a hint if needed.
    private void addHint() {
        if (hints != null && !isDrawing) {
            if (numHints <= hints.length() / 2) { // Don't give too many hints!
                // Give out hints in even intervals.
                if (secondsLeft % Math.floor((Constants.GAME_LENGTH / (hints.length() / 2 + 1))) == 0) {
                    Random r = new Random();
                    int randIndex = r.nextInt(hints.length());
                    hints = hints.substring(0, randIndex) + curWord.charAt(randIndex) + hints.substring(randIndex + 1);
                    numHints++;
                }
            }

            currentWordView.setText(hints.toUpperCase());
        }
    }

    // Initialize the view for drawing mode.
    private void setUpForDrawing() {
        // Undo the layout changes from guessing mode.
        guessButton.setVisibility(View.INVISIBLE);
        guessTextLayout.setVisibility(View.INVISIBLE);
        editPane.setVisibility(View.VISIBLE);
        if (dv != null) {
            Log.d("receiving", "removed");
            dv.startDraw();
            dv.clearCanvas();
        }

        // Set the current word.
        final DatabaseReference wordsRef = database.getReference();
        wordsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String word = "";

                // Set the word randomly.
                if (dataSnapshot.hasChild(Constants.PRES_MODE) && !(boolean)dataSnapshot.child(Constants.PRES_MODE).getValue()) {
                    Random r = new Random();
                    int randIndex = r.nextInt((int) dataSnapshot.child(Constants.LIST_OF_WORDS).getChildrenCount());
                    word = (String) dataSnapshot.child(Constants.LIST_OF_WORDS).child(String.valueOf(randIndex)).getValue();

                    // If null or already picked (and there are remaining words to choose from), re-select the word.
                    if (dataSnapshot.child(gameId).child(Constants.ALREADY_DRAWN).getChildrenCount() < dataSnapshot.child(Constants.LIST_OF_WORDS).getChildrenCount()) {
                        while (word == null || dataSnapshot.child(gameId).child(Constants.ALREADY_DRAWN).hasChild(word)) {
                            int rand = r.nextInt((int) dataSnapshot.child(Constants.LIST_OF_WORDS).getChildrenCount());
                            word = (String) dataSnapshot.child(Constants.LIST_OF_WORDS).child(String.valueOf(rand)).getValue();
                        }
                    }
                }
                // Set the word according to a list.
                else {
                    long presTurns = (long)dataSnapshot.child(gameId).child(Constants.PRES_TURNS).getValue();
                    word = Constants.PRESENTATION[(int)presTurns];
                    gameRef.child(Constants.PRES_TURNS).setValue(presTurns + 1);
                }

                currentWordView.setText(word.toUpperCase());
                curWord = word;
                gameRef.child(Constants.CURRENT_WORD).setValue(curWord);
                gameRef.child(Constants.ALREADY_DRAWN).child(curWord).setValue(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // Set up the on click listeners.
        for (MaterialButton b : buttons) {
            b.setOnClickListener(onColorClick);
        }
        trashButton.setOnClickListener(onTrashClick);
        editButton.setOnClickListener(onEditEraseClick);
        eraserButton.setOnClickListener(onEditEraseClick);

        // Set the color and mark the corresponding button.
        markButton(blackButton);
    }

    // Initialize the layout for guessing mode.
    private void setUpForGuessing() {
        guessButton.setVisibility(View.VISIBLE);
        guessTextLayout.setVisibility(View.VISIBLE);
        editPane.setVisibility(View.INVISIBLE);
        guessButton.setOnClickListener(onGuessClick);
        guessButton.setEnabled(true);

        if (dv != null) {
            dv.startGuess();
            dv.clearCanvas();
        }
    }

    // Sets the mode and button colors to edit mode.
    private void setIsNotErasing() {
        editButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.SELECTED)));
        eraserButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.BLACK)));
        isErasing = false;
    }

    // Sets everything for erasing mode.
    private void setIsErasing() {
        eraserButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.SELECTED)));
        editButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(Constants.BLACK)));
        isErasing = true;
    }

    View.OnClickListener onColorClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            markButton((MaterialButton)v);
            setIsNotErasing();
        }
    };

    // Marks the currently selected color.
    private void markButton(MaterialButton selectedButton) {
        // Changes the color of the corresponding button.
        for (MaterialButton b : buttons) {
            if (b != selectedButton) {
                b.setStrokeColorResource(R.color.colorWhite);
            }
            else {
                b.setStrokeColorResource(R.color.colorSelected);
            }
        }

        // Updates the paint color.
        switch (selectedButton.getId()) {
            case R.id.black:
                selectedColor = Constants.BLACK;
                break;
            case R.id.blue:
                selectedColor = Constants.BLUE;
                break;
            case R.id.red:
                selectedColor = Constants.RED;
                break;
            case R.id.orange:
                selectedColor = Constants.ORANGE;
                break;
            case R.id.yellow:
                selectedColor = Constants.YELLOW;
                break;
            case R.id.green:
                selectedColor = Constants.GREEN;
                break;
            case R.id.purple:
                selectedColor = Constants.PURPLE;
                break;
            case R.id.brown:
                selectedColor = Constants.BROWN;
                break;
            default:
                break;
        }

        // Sets the color on the DrawingView.
        dv.setColor(Color.parseColor(selectedColor));
    }

    @Override
    protected void onDestroy() {
        gameRef.removeEventListener(gameListener);
        timerHandler.removeCallbacks(timerRunnable);
        if (aiRunnable != null && aiHandler != null) aiHandler.removeCallbacks(aiRunnable);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        if (!isEnding) database.getReference(gameId).child(Constants.PLAYERS).child(playerName).removeValue();
        if (isAIManager) LocalBroadcastManager.getInstance(this).unregisterReceiver(aiReceiver);

        // Delete game from the database if everyone's left.
        database.getReference(gameId).child(Constants.PLAYERS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0 || (dataSnapshot.getChildrenCount() == 1 && dataSnapshot.hasChild(Constants.AI_NAME))) {
                    database.getReference(gameId).removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        super.onDestroy();
    }
}
