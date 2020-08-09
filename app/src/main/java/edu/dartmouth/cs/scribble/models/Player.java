package edu.dartmouth.cs.scribble.models;

public class Player implements Comparable<Player> {
    private String playerName;
    private boolean isAI;
    private int score;
    private boolean isDrawing;

    public Player() {
        playerName = "";
        isAI = false;
        score = 0;
        isDrawing = false;
    }

    public Player(String playerName, boolean isAI) {
        this.playerName = playerName;
        this.isAI = isAI;
        score = 0;
        isDrawing = false;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public boolean isAI() {
        return isAI;
    }

    public void setAI(boolean AI) {
        isAI = AI;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isDrawing() {
        return isDrawing;
    }

    public void setDrawing(boolean drawing) {
        isDrawing = drawing;
    }

    @Override
    public int compareTo(Player o) {
        return (int)(o.getScore() - this.getScore());
    }
}
