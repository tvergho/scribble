package edu.dartmouth.cs.scribble.models;

public class Guess implements Comparable<Guess> {
    private String playerName;
    private String guess;
    private int guessType;
    private long time;

    public Guess() {
        playerName = "";
        guess = "";
        guessType = Constants.GUESS_TYPE_INCORRECT;
        time = 0;
    }

    public Guess(String playerName, String guess, String curWord, long time) {
        this.playerName = playerName;
        this.guess = guess;
        this.time = time;

        if (curWord.equals(guess.trim().toLowerCase())) {
            guessType = Constants.GUESS_TYPE_CORRECT;
        }
        else {
            guessType = Constants.GUESS_TYPE_INCORRECT;
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getGuess() {
        return guess;
    }

    public void setGuess(String guess) {
        this.guess = guess;
    }

    public int getGuessType() {
        return guessType;
    }

    public void setGuessType(int guessType) {
        this.guessType = guessType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public int compareTo(Guess o) {
        return (int)(this.getTime() - o.getTime());
    }
}
