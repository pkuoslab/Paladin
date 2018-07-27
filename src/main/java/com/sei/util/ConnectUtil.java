package com.sei.util;

import com.alibaba.fastjson.JSONObject;
import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.AppGraph;
import com.sei.bean.View.ViewTree;
import okhttp3.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static com.sei.util.CommonUtil.log;


/**
 * Created by mike on 17-9-6.
 */

public class ConnectUtil {
    public static String launch_pkg = "";
    public static String current_pkg = "";
    public static String prefix = "";

    public static void setUp(String pkgName){
        current_pkg = pkgName;
        launch_pkg = pkgName;
        int s = current_pkg.lastIndexOf(".");
        prefix = current_pkg.substring(0, s);
        log("prefix: " + prefix);
    }

    public static String sendInstruction(Device d, String method, String args){
        Map<String, String> map = new HashMap<>();
        map.put("pkgName", d.current_pkg);
        map.put("method", method);
        map.put("arg", args);
        String rq = d.ip + ":" + d.port +  "/CMDManager?getMessage=" + SerializeUtil.toBase64(map);
        return sendHttpGet(rq);
    }

    public static String sendHttpGet(String str){
        try{
            String request = str;
            URL url = new URL(request);
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(20 * 1000);
            InputStream input = connection.getInputStream();
            Scanner sc = new Scanner(input);

            StringBuilder sb = new StringBuilder();
            for(; sc.hasNextLine();){
                sb.append(sc.nextLine()).append("\n");
            }
            sc.close();
            return sb.toString();

        }catch (Exception e){
            //e.printStackTrace();
            return e.getMessage();
        }
    }

    public static String postJson(String request, JSONObject jo) throws Exception{
        URL url = new URL(request);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(jo.toString());
        wr.flush();

        int HttpResult = con.getResponseCode();
        if (HttpResult == HttpURLConnection.HTTP_OK) {
            InputStreamReader is = new InputStreamReader((InputStream) con.getContent());
            BufferedReader bf = new BufferedReader(is);
            StringBuilder sb = new StringBuilder();
            String line;
            do{
                line = bf.readLine();
                if (line != null)
                    sb.append(line + "\n");
            }while(line != null);
            return sb.toString();
        }else{
            log(con.getResponseMessage());
            return "fail";
        }
    }

    public static void force_stop(String pkg){
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell am force-stop " + pkg);
    }
}
