package com.sei.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by vector on 16/6/27.
 */
public class CommonUtil {
    public static String HOST = "http://127.0.0.1:6161";
    public static String SERVER = "http://172.20.66.202:5600";
    public static int DEFAULT_PORT = 5700;
    public static int SLEEPTIME = 1000;
    public static double SIMILARITY = 0.9;
    public static String DIR = "";
    public static String ADB_PATH = "/home/mike/Android/Sdk/platform-tools/";
    public static int SCREEN_X = 0;
    public static String PASSWORD = "monkeymonkey";
    public static String SERIAL = "";
    public static Random random = new Random(266); //trail : 259


    public static void main(String[] argv){
        for(int i=0; i < 15; i++)
            System.out.println(random.nextDouble());
    }

    public static void sleep(int milliseconds){
        try{
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public static int shuffle(List<Integer> foots, int tot){
        int ran = (int)(random.nextDouble() * tot);
        if (foots.size() >= tot)
            return -1;
        while (foots.contains(ran)){
            ran = (int)(random.nextDouble() * tot);
        }
//        log("shuffle " + (ran + 1) + " / " + tot);
        return ran;
    }

    public static String readFromFile(String path){
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while(line != null){
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            String ret = sb.toString();
            return ret;
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static void log(String info) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String S = new SimpleDateFormat("MM-dd HH:mm:ss").format(timestamp);
        System.out.println(S + "\t" + info);
    }
}
