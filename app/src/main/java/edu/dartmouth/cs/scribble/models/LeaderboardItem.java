package edu.dartmouth.cs.scribble.models;

public class LeaderboardItem implements Comparable<LeaderboardItem> {
    private String playerName;
    private long score;

    public LeaderboardItem() {
        playerName = "";
        score = 0;
    }

    public LeaderboardItem(String playerName, long score) {
        this.playerName = playerName;
        this.score = score;
    }


    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    @Override
    public int compareTo(LeaderboardItem o) {
        return (int)(o.getScore() - this.getScore());
    }
}
