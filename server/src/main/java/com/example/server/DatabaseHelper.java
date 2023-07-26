package com.example.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import edu.hitsz.database.Score;

public class DatabaseHelper {
    private static final String url = "jdbc:sqlite:database.db";
//    public static void main(String[] args) {
//        createMedium();
//    } //已执行
    private static void createDataBase() {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }
    private static void createUser() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS USER" +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " PASSWORD        TEXT     NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Table User create successfully");
    }
    private static void createMedium() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS MEDIUM " +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " SCORE        INTEGER     NOT NULL, " +
                    " TIME           TEXT      NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Table MEDIUM create successfully");
    }

    /**
     * 创建新账户
     * @param userName
     * @param password
     */
    public static void createNewAccount(String userName, String password) {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "INSERT INTO USER (NAME,PASSWORD) " +
                    "VALUES ('"+
                    userName + "','" +
                    password + "');";

            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Account create successfully");
    }

    /**
     * 查询账户
     * @param userName 账户用户名
     * @return 存在此账户则返回密码，否则返回not exist
     */
    public static String queryAccount(String userName) {
        String password = null;
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "SELECT * FROM USER WHERE NAME=" +
                    "'"+ userName +"';";

            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                password = rs.getString("password");
            } else {
                password = "not exist!";
            }
            stmt.close();
            c.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        return password;
    }

    /**
     * 插入新的分数记录
     * @param username
     * @param score
     * @param gameType MEDIUM/EASY/HARD
     */
    public static void addScore(String username, int score, String gameType) {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        String time = dateTime.format(formatter);
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully to add");

            stmt = c.createStatement();
            if (gameType.contains("EASY")) {
                gameType = "easy";
            } else if (gameType.contains("MEDIUM")) {
                gameType = "medium";
            } else if (gameType.contains("HARD")) {
                gameType = "hard";
            }
            String sql = "INSERT INTO "+gameType+" (NAME,SCORE,TIME) " +
                    "VALUES ('"+
                    username + "','" +
                    score + "','" +
                    time + "');";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }

    /**
     * 删除一条得分记录
     * @param id
     * @param gameType 表名
     */
    public static void deleteScore(String id, String gameType) {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "DELETE FROM "+gameType +
                    " WHERE ID=" + id;
            System.out.println(sql);

            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }

    public static ArrayList<Score> getRankList(String gameType) {
        ArrayList<Score> rankList = new ArrayList<>();
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(url);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM "+gameType+" ORDER BY SCORE DESC;");
            while (rs.next()) {
                Score score = new Score(rs.getInt("ID"),
                        rs.getString("NAME"),
                        rs.getInt("SCORE"),
                        rs.getString("TIME"));
                rankList.add(score);
            }
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        return rankList;
    }
}
