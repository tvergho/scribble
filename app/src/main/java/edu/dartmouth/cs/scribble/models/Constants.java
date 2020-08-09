package edu.dartmouth.cs.scribble.models;

import android.graphics.Point;

public class Constants {
    // Machine learning
    public static final String MODEL_NAME = "ScribbleModelV2";
    public static final int NUM_CLASSES = 15;
    public static final String[] ML_WORDS = new String[]{
            "apple", "bowtie", "candle", "door", "envelope",
            "fish", "guitar", "ice cream", "lightning", "moon",
            "mountain", "star", "tent", "toothbrush", "wristwatch"
    };
    public static final String[] PRESENTATION = new String[]{
            "toothbrush", "lightning", "star", "wristwatch", "moon", "door",
            "ice cream", "bowtie", "apple", "tent", "envelope", "candle",
            "mountain", "fish", "guitar"
    };

    // Colors
    public static final String BLACK = "#000000";
    public static final String BLUE = "#0000FF";
    public static final String RED = "#FF0000";
    public static final String ORANGE = "#FFA800";
    public static final String YELLOW = "#FEFE04";
    public static final String GREEN = "#00FF00";
    public static final String PURPLE = "#CC00FF";
    public static final String BROWN = "#964B00";
    public static final String SELECTED = "#FF891C";
    public static final String AI_DISABLED = "#7ECAFF";
    public static final String AI_ENABLED = "#219CF1";
    public static final String MAIN_DISABLED = "#80FFE81C";
    public static final String MAIN_ENABLED = "#FFE81C";

    // Intents
    public static final String GAME_ID = "game-id";
    public static final String PLAYER_NAME = "player-name";
    public static final String AI_MANAGER = "ai-manager";
    public static final String BROADCAST_GUESS = "broadcast-guess";
    public static final String GUESS_KEY = "guess";

    // Firebase paths
    public static final String PLAYERS = "players";
    public static final String HAS_STARTED = "hasStarted";
    public static final String CANVAS = "canvas";
    public static final String IS_DRAWING = "drawing";
    public static final String LIST_OF_WORDS = "_words";
    public static final String CURRENT_WORD = "curWord";
    public static final String ALREADY_DRAWN = "alreadyDrawn";
    public static final String ALREADY_ARTIST = "alreadyArtist";
    public static final String TURN_STARTED = "turnStarted";
    public static final String GUESSES = "guesses";
    public static final String TURN_SCORE = "turnScore";
    public static final String MAX_TURN_SCORE = "maxTurnScore";
    public static final String ARTIST_NAME = "artistName";
    public static final String CURRENT_TIME = "currentTime";
    public static final String NEXT_TURN = "nextTurn";
    public static final String TURN = "turn";
    public static final String GAME_ENDED = "gameEnded";
    public static final String SIGNED_OUT = "signedOut";
    public static final String PRES_MODE = "_presMode";
    public static final String PRES_TURNS = "presTurns";

    // Guesses
    public static final int GUESS_TYPE_INCORRECT = 0;
    public static final int GUESS_TYPE_CORRECT = 1;

    // Other
    public static final String AI_NAME = "AI Player";
    public static final long GAME_LENGTH = 60;
    public static final int MAX_TURNS = 3;
}
