package edu.hitsz.database;

import java.io.Serializable;

public class Score implements Serializable {
    private int id;
    private String name;
    private int score;
    private String time;
    public Score(int id, String name, int score, String time){
        this.id = id;
        this.name = name;
        this.score = score;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public String getTime() {
        return time;
    }

    public int getId() {
        return id;
    }
}
