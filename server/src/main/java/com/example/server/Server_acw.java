package com.example.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Handler;

import javax.xml.crypto.Data;

import edu.hitsz.database.Score;

public class Server_acw {
    private static final String TAG = "Server";
    public static final int PORT = 11451;
    private final int matchWaitTime = 10000;
    //username-socket
    private final HashMap<String, Socket> uList = new HashMap<>();
    //以下四个表都只用于对战
    //typeList-相当于待匹配的玩家列表
    private final HashMap<String, String> typeList = new HashMap<>();
    //matchList-已经匹配成功的玩家对
    private final HashMap<String, String> matchList = new HashMap<>();
    private final HashMap<String, Integer> scoreList = new HashMap<>();
    private final HashMap<String, Integer> hpList = new HashMap<>();
    private ServerSocket server = null;


    public static void main(String[] args) { new Server_acw();}

    public Server_acw(){
        try {
            server = new ServerSocket(PORT);
            System.out.println("---Server opening---");
            while (true) {
                Socket client = server.accept();
                System.out.println("New client socket connect:" + client);
                //连接建立后，在子线程中运行后续逻辑，主线程继续循环，等待下一个链接

                Thread t = new Thread(new Service(client));
                //启动子线程处理这一连接的后续逻辑
                t.start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对每个连接成功的客户端进行服务线程实例化
     */
    class Service implements Runnable {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String rivalName;
        public Service(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), "UTF-8")), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String line;
                while((line = reader.readLine()) != null) {
                    //不断等待数据并读取
                    //读到新的信息，就根据信息进行回应
                    final String[] words = line.split(" ");

                    if (words[0].contains("SignUp")) {
                        //注册
                        signUp(words[1], words[2]);
                    }
                    if (words[0].contains("SignIn")) {
                        //登录
                        signIn(words[1], words[2]);
                    }
                    if (words[0].contains("single")) {
                        writer.println("Game Admission");
                        System.out.println("Game Admission");
                    }
                    if (words[0].contains("double")) {
                        synchronized (typeList) {
                            typeList.put(words[2], words[1]);
                        }

                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                while (!interrupted()) {
                                    boolean flag = match(words[1], words[2]);
                                    if (flag) {
                                        synchronized (typeList) {
                                            //用户名作为key,移除此用户
                                            typeList.remove(words[2]);
                                        }
                                        synchronized (scoreList) {
                                            scoreList.put(words[2], 0);
                                        }
                                        synchronized (hpList) {
                                            hpList.put(words[2], 0);
                                        }

                                        writer.println("Game Admission");
                                        System.out.println("Game Admission");
                                        break;
                                    }
                                }
                            }
                        };
                        t.start();
                        Thread.sleep(matchWaitTime);
                        if (t.isAlive()) {
                            t.interrupt();
                            synchronized (typeList) {
                                typeList.remove(words[2]);
                            }
                            writer.println("Match failed!");
                            System.out.println("Match failed!");
                        }

                    }
                    if (words[0].contains("UPDATE:")) {
                        int rivalScore = 0;
                        int rivalHp = 0;
                        synchronized (matchList) {
                            rivalName = matchList.get(words[1]);
                        }
                        try {
                            synchronized (scoreList) {
                                scoreList.replace(words[1], Integer.parseInt(words[2]));
                                if (rivalName != null) {
                                    rivalScore = scoreList.get(rivalName);
                                }
                            }
                            synchronized (hpList) {
                                hpList.replace(words[1], Integer.parseInt(words[3]));
                                if (rivalName != null) {
                                    rivalHp = hpList.get(rivalName);
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        writer.println("UPDATE: "+rivalName+" "+rivalScore+" "+rivalHp);
                    }
                    if (words[0].contains("MEDIUM") || words[0].contains("HARD") || words[0].contains("EASY")) {
                        //记录得分操作
                        if (words[2].equals("single")) {
                            //直接录入数据库
                            DatabaseHelper.addScore(words[1], Integer.parseInt(words[3]), words[0]);
                            writer.println("Rank Admission");
                            System.out.println("Rank Admission");
                        } else if (words[2].equals("double")) {
                            //录入得分，写回对战结果
                            DatabaseHelper.addScore(words[1], Integer.parseInt(words[3]), words[0]);
                            //TODO-检查对手是否阵亡
                            while (true) {
                                synchronized (hpList) {
                                    if (hpList.get(rivalName) <= 0) {
                                        hpList.remove(rivalName);
                                        break;
                                    }
                                }
                            }
                            //当双方皆阵亡，进入排行榜
                            writer.println("Rank Admission");
                            System.out.println("Rank Admission");
                            //本局游戏结束的后处理
                            synchronized (matchList) {
                                matchList.remove(rivalName);
                            }
                            synchronized (scoreList) {
                                scoreList.remove(rivalName);
                            }
                        }
                    }
                    if (words[0].contains("GetRankList")) {
                        ArrayList<Score> rankList = DatabaseHelper.getRankList(words[1]);
                        for (Score score : rankList) {
                            writer.println("Score: "+
                                    score.getId() + " " +
                                    score.getName() + " " +
                                    score.getScore() + " " +
                                    score.getTime());
                        }
                        System.out.println("write rankList");
                    }
                    if (words[0].contains("Delete")) {
                        DatabaseHelper.deleteScore(words[2],words[1]);
                        ArrayList<Score> rankList = DatabaseHelper.getRankList(words[1]);
                        for (Score score : rankList) {
                            writer.println("Score: "+
                                    score.getId() + " " +
                                    score.getName() + " " +
                                    score.getScore() + " " +
                                    score.getTime());
                        }
                    }


                }
                writer.println("");//输入流关闭后，还可以通过输出流发送
                socket.shutdownOutput();//发送完毕，关闭输出流，服务端
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void signIn(String name, String password) {
            String re;
            re = DatabaseHelper.queryAccount(name);
            if (re.equals(password)) {
                writer.println("Sign in successfully!");
                //将登陆成功的用户加入列表
                synchronized (uList) {
                    uList.put(name, socket);
                }
                System.out.println("Sign in successfully!");
            } else if (re.equals("not exist!")) {
                writer.println("Account doesn't exist!");
                System.out.println("Account doesn't exist!");
            } else {
                writer.println("Wrong password!");
                System.out.println("Wrong password!");
            }
        }

        private void signUp(String name, String password) {
            String re = DatabaseHelper.queryAccount(name);
            if (re.equals("not exist!")) {
                DatabaseHelper.createNewAccount(name, password);
                writer.println("Sign up successfully!");
                //将注册成功的用户加入列表
                uList.put(name, socket);
                System.out.println("Sign up successfully!");
            } else {
                writer.println("Account already existed!");
                System.out.println("Account already existed!");
            }
        }

        /**
         * 为client寻找对手
         * @param gameType single/medium/hard
         * @return 存在难度匹配的对手返回true，否则返回false
         */
        private boolean match(String gameType, String username) {
            synchronized (matchList) {
                //对方也匹配到自己
                for (Map.Entry<String, String> entry : matchList.entrySet()) {
                    if (entry.getKey().equals(username)) {
                        this.rivalName = entry.getValue();
                        matchList.put(rivalName, username);
                        return true;
                    }
                }
                //自己匹配到对方
                synchronized (typeList) {
                    for (Map.Entry<String, String> entry : typeList.entrySet()) {
                        if (entry.getValue().equals(gameType) && !entry.getKey().equals(username)) {
                            //若选择难度相同且非同一用户
                            this.rivalName = entry.getKey();
                            matchList.put(rivalName, username);

                            break;
                        }
                    }
                }

            }

            return false;
        }
    }




}