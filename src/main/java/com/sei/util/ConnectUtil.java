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
    public static OkHttpClient uploadClient = null;
    public static String launch_pkg = "";
    public static String current_pkg = "";
    public static String response = "";
    public static String prefix = "";
    public static Boolean is_upload = false;

    public static void setUp(String pkgName){
        current_pkg = pkgName;
        launch_pkg = pkgName;
        int s = current_pkg.lastIndexOf(".");
        prefix = current_pkg.substring(0, s);
        log("prefix: " + prefix);
        if (is_upload) {
            // 遗留设计，向展示图的服务器上传新节点信息
            // 应在新版本中使用统一 RESTful接口
            uploadClient = new OkHttpClient();
        }
    }

    public static String sendInstruction(String method, String args){
        String pkg = current_pkg;
        return sendInstruction(pkg, method, args);
    }

    public static String sendInstruction(String pkg, String method, String args){
        Map<String, String> map = new HashMap<>();
        map.put("pkgName", pkg);
        map.put("method", method);
        map.put("arg", args);
        String request = CommonUtil.HOST  + "/CMDManager?getMessage=" + SerializeUtil.toBase64(map);
//        log("request: " + request);
        return sendHttpGet(request);
    }


    public static String sendOrderBeforeReadFile(String method, String args){
        String status = sendInstruction(method, args);
        String retStr = "";
        //log("status: " + status);
        if (status.contains("Success")) {
            int index = CommonUtil.SERIAL.indexOf(" ");
            String tree_file;
            if (index != -1)
                tree_file = "tree-" + CommonUtil.SERIAL.substring(index+1) + ".json";
            else
                tree_file = "tree.json";

            ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " pull sdcard/tree.json " + CommonUtil.DIR + tree_file);
            retStr = CommonUtil.readFromFile(CommonUtil.DIR + tree_file);
        }else {
            log("Client fail to write");
        }
        return retStr;
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
            connection.setReadTimeout(10 * 1000);
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

    public static void asyncPostJson(String json, String url){
        final MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        uploadClient.newCall(request).enqueue(customCallback(url));
    }

    public static void asyncPostForm(RequestBody formbody, String url){
        Request request = new Request.Builder()
                .url(url)
                .post(formbody)
                .build();
        uploadClient.newCall(request).enqueue(customCallback(url));
    }

    public static Callback customCallback(String url){
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log("POST: " + url + " FAIL!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log("POST " + url + " SUCCESS!");
            }
        };
        return callback;
    }

//    public static void upload(ViewTree tree, AppGraph appGraph){
//        try {
//            RequestBody requestbody = new FormBody.Builder()
//                    .add("config", SerializeUtil.toBase64(appGraph))
//                    .add("current", SerializeUtil.toBase64(tree))
//                    .build();
//            asyncPostForm(requestbody, CommonUtil.SERVER + "/upload");
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }

    public static void upload(String treeStr){
        //
    }

    public static String killApp(String pkg){
        Map<String, String> map = new HashMap<>();
        String name = "CMDManager";
        String method = "killApp";
        map.put("pkgName", name);
        map.put("method", method);
        map.put("arg", pkg);
        String request = CommonUtil.HOST  + "/CMDManager?getMessage=" + SerializeUtil.toBase64(map);
        log("request: " + request);
        return sendHttpGet(request);
    }

    public static void force_stop(String pkg){
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell am force-stop " + pkg);
    }
}
