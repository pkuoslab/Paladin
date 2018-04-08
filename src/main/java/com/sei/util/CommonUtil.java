package com.sei.util;

import com.sei.agent.Device;

import java.io.BufferedReader;
import java.io.File;
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
    //public static String SERVER = "http://172.20.66.202:5600";
    public static int DEFAULT_PORT = 5700;
    public static int SLEEPTIME = 1000;
    public static double SIMILARITY = 0.9;
    public static String DIR = "";
    public static String ADB_PATH = "/home/mike/Android/Sdk/platform-tools/";
    public static int SCREEN_X = 0;
    public static String PASSWORD = "monkeymonkey";
    public static Boolean SCREENSHORT = true;
    public static String SERIAL = "";
    public static Random random = new Random(1011); //trail : 259


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

    public static void getSnapshot(String name){
        File dir = new File(ConnectUtil.launch_pkg);
        if (!dir.exists())
            dir.mkdir();

        String picname = name + ".png";
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL  +" shell screencap -p sdcard/" + picname);
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL  +" pull sdcard/" + picname + " " + ConnectUtil.launch_pkg + "/");
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL  +" shell rm sdcard/" + picname);
    }

    public static void log(String info) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String S = new SimpleDateFormat("MM-dd HH:mm:ss").format(timestamp);
        System.out.println(S + "\t" + info);
    }

    public static void start_paladin(Device d){
        ClientUtil.stopApp(d, "ias.deepsearch.com.helper");
        ClientUtil.stopApp(d, ConnectUtil.launch_pkg);
        CommonUtil.sleep(2000);
        ClientUtil.startApp(d, "ias.deepsearch.com.helper");
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input keyevent KEYCODE_HOME");
        CommonUtil.sleep(2000);
        ClientUtil.startApp(d, ConnectUtil.launch_pkg);
        if (d.ip.contains("127.0.0.1"))
            ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial + " forward tcp:" + d.port + " tcp:6161");
    }
}
