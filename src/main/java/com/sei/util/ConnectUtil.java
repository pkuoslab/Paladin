package com.sei.util;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.AppGraph;
import com.sei.bean.View.ViewTree;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
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
        String rq = d.ip + "/CMDManager?getMessage=" + SerializeUtil.toBase64(map);
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

    public static void force_stop(String pkg){
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell am force-stop " + pkg);
    }
}
