package com.sei.server;

import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.modules.DepthFirstTraversal;
import com.sei.modules.ModelReplay;
import com.sei.modules.Strategy;
import com.sei.modules.testStrategy;
import com.sei.server.component.Handler;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.util.ServerRunner;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

import static com.sei.util.CommonUtil.DEFAULT_PORT;
import static com.sei.util.CommonUtil.log;

public class Control extends NanoHTTPD{
    GraphManager graphManager = new GraphManager();
    Strategy strategy;
    public static HashMap<String, Handler> route_table = new HashMap<>();


    public static void main(String[] argv) {
        //setListeningPort();
        Control server = new Control();
        server.set_route_table();
        server.configure();
        //System.out.println("listening on: " + DEFAULT_PORT);
        //ServerRunner.run(Control.class);

    }

    public Control(){
        super(DEFAULT_PORT);
    }

    public void register(String route, Handler handler){
        route_table.put(route, handler);
    }
    public void set_route_table(){
        register("/", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                return newFixedLengthResponse("hello!!!");
            }
        });

        register("/set", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                Map<String, List<String>> query = session.getParameters();
                String packageName = query.get("package").get(0);
                ClientUtil.startApp(packageName);
                return newFixedLengthResponse("set package: " + packageName);
            }
        });

        register("/start", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                Map<String, List<String>> query = session.getParameters();
                startTestApplication(query.get("strategy").get(0));
                return newFixedLengthResponse("start success");
            }
        });

        register("/stop", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                stopTestApplication();
                return newFixedLengthResponse("stop success");
            }
        });

        register("/list", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                JSONObject jo = new JSONObject();
                try {
                    jo.put("nodes", graphManager.getAllNodesTag());
                    jo.put("activity", graphManager.getActivitySize());
                }catch (Exception e){
                    e.printStackTrace();
                }
                return newFixedLengthResponse(jo.toString());
            }
        });

        register("/replay", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                // parameter format : /replay?nodes=xxx_xxx&xxx_xxx
                String query = session.getQueryParameterString().substring(6);
                //log(query.toString());
                if (query.equals("all")) {
                    strategy = new ModelReplay(graphManager);
                    strategy.start();
                }else{
                    List<String> route_list = Arrays.asList(query.split("&"));
                    strategy = new ModelReplay(graphManager, route_list);
                    strategy.start();
                }
                return newFixedLengthResponse("replay start");
            }
        });
    }

    @Override
    public Response serve(IHTTPSession session){
        String url = session.getUri();
        Handler handler = route_table.get(url);
        if (handler == null)
            return newFixedLengthResponse("unknown command");
        else {
            return handler.onRequest(session);
        }
    }

    void startTestApplication(String s) {
        log("start test ");
        CommonUtil.sleep(2000);
        try {
            if (s.equals("DFS")){
                log("strategy: DFS");
                CommonUtil.random = new Random(250);
                graphManager = new GraphManager();
                strategy = new DepthFirstTraversal(graphManager);
                strategy.start();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stopTestApplication() {

        log("stop test");
        try {
            strategy.EXIT = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void setListeningPort(){
        try {
            String dir = "/home/mike/togithub/droidwalker/droidwalker/out/artifacts/droidwalker_jar/";
            File config = new File(dir + "config.json");
            if (!config.exists()) return;
            String content = CommonUtil.readFromFile(dir + "config.json");
            JSONObject config_json = new JSONObject(content);
            CommonUtil.DEFAULT_PORT = config_json.getInt("DEFAULT_PORT");
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void configure(){
        String dir = "./";
        File config = new File(dir + "config.json");
        if (!config.exists()) return;
        try {
            String content = CommonUtil.readFromFile(dir + "config.json");
            JSONObject config_json = new JSONObject(content);

            JSONObject device = (JSONObject) config_json.get("DEVICE");
            CommonUtil.HOST = "http://" + device.getString("IP") + ":6161";
            JSONObject app = (JSONObject) config_json.get("APP");
            CommonUtil.ADB_PATH = config_json.getString("ADB_PATH");
            if (device.has("SCREEN_WIDTH"))
                CommonUtil.SCREEN_X = device.getInt("SCREEN_WIDTH");

            ClientUtil.startApp(app.getString("PACKAGE"));

            if (app.getString("STRATEGY").equals("DFS")){
                log("strategy: " + app.getString("STRATEGY"));
                strategy = new DepthFirstTraversal(graphManager);
                strategy.start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
