package com.sei.server;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.modules.DepthFirstTraversal;
import com.sei.modules.ModelReplay;
import com.sei.modules.Strategy;
import com.sei.modules.plugin.WebviewService;
import com.sei.modules.testStrategy;
import com.sei.server.component.Handler;
import com.sei.server.component.Scheduler;
import com.sei.util.*;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.util.ServerRunner;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.naming.ldap.ControlFactory;
import java.io.File;
import java.util.*;

import static com.sei.util.CommonUtil.DEFAULT_PORT;
import static com.sei.util.CommonUtil.log;

public class Control extends NanoHTTPD{
    GraphManager graphManager = new GraphManager();
    Strategy strategy;
    public static HashMap<String, Handler> route_table = new HashMap<>();
    public static Boolean finished = false;
    public static Scheduler scheduler;


    public static void main(String[] argv) {
        setListeningPort();
        Control server = new Control();
        server.set_route_table();
        scheduler = new Scheduler();
        server.configure(argv);
        System.out.println("listening on: " + DEFAULT_PORT);
        // WebviewService webviewService = new WebviewService();
        // webviewService.start();
        ServerRunner.run(Control.class);
    }

    public Control(){
        super(CommonUtil.DEFAULT_PORT);
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


        register("/list", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                JSONObject jo = new JSONObject();
                try {
                    jo.put("nodes", scheduler.graphAdjustor.getAllNodesTag());
                    jo.put("activity", scheduler.graphAdjustor.appGraph.getActivities().size());
                }catch (Exception e){
                    e.printStackTrace();
                }
                return newFixedLengthResponse(jo.toString());
            }
        });

//        register("/replay", new Handler() {
//            @Override
//            public Response onRequest(IHTTPSession session) {
//                // parameter format : /replay?nodes=xxx_xxx&xxx_xxx
//                String query = session.getQueryParameterString().substring(6);
//                //log(query.toString());
//                if (query.equals("all")) {
//                    strategy = new ModelReplay(graphManager);
//                    strategy.start();
//                }else{
//                    List<String> route_list = Arrays.asList(query.split("&"));
//                    strategy = new ModelReplay(graphManager, route_list);
//                    strategy.start();
//                }
//                return newFixedLengthResponse("replay start");
//            }
//        });

        register("/save", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                log("save graph");
                scheduler.graphAdjustor.save();
                scheduler.save();
                return newFixedLengthResponse("save");
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
            //String dir = "/home/mike/togithub/droidwalker/droidwalker/out/artifacts/droidwalker_jar/";
            String dir = "./";
            File config = new File(dir + "config.json");
            if (!config.exists()) return;
            String content = CommonUtil.readFromFile(dir + "config.json");
            JSONObject config_json = new JSONObject(content);
            CommonUtil.DEFAULT_PORT = config_json.getInt("DEFAULT_PORT");
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void configure(String[] argv){
        String dir = "./";
        File config = new File(dir + "config.json");
        if (!config.exists()) return;
        try {
            String content = CommonUtil.readFromFile(dir + "config.json");
            JSONObject config_json = new JSONObject(content);
            if (config_json.has("ADB_PATH")){
                log("ADB: " + config_json.getString("ADB_PATH"));
                CommonUtil.ADB_PATH = config_json.getString("ADB_PATH");
            }
            String pkg = config_json.getString("PACKAGE");
            ConnectUtil.setUp(pkg);
            JSONArray device_config = config_json.getJSONArray("DEVICES");
            List<Device> devices = new ArrayList<>();

            for(int i=0; i < device_config.length(); i++){
                JSONObject c = device_config.getJSONObject(i);
                String ip = "http://" + c.getString("IP");
                String serial = c.getString("SERIAL");
                String pass = "";
                if (c.has("PASSWORD")) pass = c.getString("PASSWORD");

                if (ip.contains("127.0.0.1"))
                    ShellUtils2.execCommand("adb -s " + serial + " forward tcp:" + c.getInt("PORT") + " tcp:6161");
                Device d = new Device(ip + ":" + c.getInt("PORT"), c.getInt("PORT"), serial, pkg, pass);
                devices.add(d);
                scheduler.bind(d);
            }

            for(Device d: devices)
                d.start();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
