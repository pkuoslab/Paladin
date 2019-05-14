package com.sei.server;

import com.alibaba.fastjson.JSON;
import com.sei.agent.Device;
import com.sei.bean.View.ViewTree;
import com.sei.modules.test.ReplayTest;
import com.sei.server.component.Handler;
import com.sei.server.component.Scheduler;
import com.sei.util.*;
import com.sei.util.client.ClientAdaptor;
import com.sei.util.client.ClientAutomator;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.util.ServerRunner;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

import static com.sei.util.CommonUtil.DEFAULT_PORT;
import static com.sei.util.CommonUtil.log;
import com.sei.util.CmdUtil;

public class Control extends NanoHTTPD{
    public static HashMap<String, Handler> route_table = new HashMap<>();
    public static Scheduler scheduler;
    JSONObject config_json;
    //List<Device> devices = new ArrayList<>();
    Map<String, Device> devices = new HashMap<>();


    public static void main(String[] argv) {
        setListeningPort();
        Control server = new Control();
        server.set_route_table();
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
                    jo.put("activity", scheduler.graphAdjustor.getAllNodesTag().size());
                }catch (Exception e){
                    e.printStackTrace();
                }

                return newFixedLengthResponse(jo.toString());
            }
        });

        register("/replay", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                // parameter format : /replay?serial=xxx&nodes=xxx_xxx&xxx_xxx
                if (session.getQueryParameterString() == null){
                    return newFixedLengthResponse("format: replay?serial=xxx&nodes=xxx");
                }
                String query = session.getQueryParameterString().substring(7);
                List<String> route_list = Arrays.asList(query.split("&"));
                String serial = route_list.get(0);
                if (devices.containsKey(serial)){
                    return newFixedLengthResponse(serial + " still running");
                }

                Device d = null;
                try {
                    JSONArray device_config = config_json.getJSONArray("DEVICES");
                    for (int i = 0; i < device_config.length(); i++) {
                        JSONObject c = device_config.getJSONObject(i);
                        if (!c.getString("SERIAL").equals(serial))
                            continue;
                        String pkg = config_json.getString("PACKAGE");
                        String ip = "http://" + c.getString("IP");
                        String pass = "";
                        if (c.has("PASSWORD")) pass = c.getString("PASSWORD");
                        if (ip.contains("127.0.0.1"))
                            ShellUtils2.execCommand("adb -s " + serial + " forward tcp:" + c.getInt("PORT") + " tcp:6161");
                        d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, 0);
                        //d.setRoute_list(route_list);
                        if (!scheduler.bind(d))
                            return newFixedLengthResponse(serial + " still running?");
                        //ClientAdaptor.stopApp(d, pkg);
                        //d.start();
                        break;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    return newFixedLengthResponse("error");
                }

                if (route_list.get(1).substring(6).equals("all")) {
                    //strategy = new ModelReplay(graphManager);
                    //strategy.start();
                    ReplayTest test = new ReplayTest(d, scheduler);
                    test.start();
                }else{
                    route_list = route_list.subList(1, route_list.size());
                    int idx = route_list.get(0).indexOf("=");
                    route_list.set(0, route_list.get(0).substring(idx+1));
                    d.setRoute_list(route_list);
                    ClientAdaptor.stopApp(d, d.current_pkg);
                    d.start();
                }
                return newFixedLengthResponse("replay start");
            }
        });

        register("/save", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                log("save graph");
                scheduler.graphAdjustor.save();
                scheduler.save();
                return newFixedLengthResponse("save");
            }
        });

        register("/stop", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                // parameter format : /stop?serial=xxx
                if (session.getQueryParameterString() == null){
                    return newFixedLengthResponse("format: stop?serial=xxx");
                }
                String query = session.getQueryParameterString().substring(7);
                //log("stop device: " + query);
                if(devices.containsKey(query)){
                    devices.get(query).Exit = true;
                    devices.remove(query);
                }
                return newFixedLengthResponse("stop");
            }
        });

        register("/getXML", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                if (session.getQueryParameterString() == null){
                    return newFixedLengthResponse("format: getXML?serial=xxx");
                }
                String query = session.getQueryParameterString().substring(7);
                //log("stop device: " + query);
                if(!devices.containsKey(query)) {
                    return newFixedLengthResponse("unknown serial");
                }
                Device d = devices.get(query);
                String xml = ClientAutomator.getXML(d);
                //CommonUtil.log(xml);
                return newFixedLengthResponse(xml);
            }
        });

        register("/getTree", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                if (session.getQueryParameterString() == null){
                    return newFixedLengthResponse("format: getTree?serial=xxx");
                }
                String query = session.getQueryParameterString().substring(7);
                //log("stop device: " + query);
                if(!devices.containsKey(query)) {
                    return newFixedLengthResponse("unknown serial");
                }
                Device d = devices.get(query);
                ViewTree tree = ClientAutomator.getCurrentTree(d);
                //CommonUtil.log(xml);
                return newFixedLengthResponse(JSON.toJSONString(tree));
            }
        });

        register("/finish", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                // format: /finish?serial=xxx
                String query = session.getQueryParameterString().substring(7);
                for (String key: devices.keySet()){
                    Device d = devices.get(key);
                    if (d.serial.equals(query)){
                        if (d.Exit) return newFixedLengthResponse("yes");
                        else return newFixedLengthResponse("no");
                    }
                }

                return newFixedLengthResponse("unknown serial");
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
            config_json = new JSONObject(content);
            if (config_json.has("ADB_PATH")){
                log("ADB: " + config_json.getString("ADB_PATH"));
                CommonUtil.ADB_PATH = config_json.getString("ADB_PATH");
            }

            if (config_json.has("BACKEND")){
                String backEnd = config_json.getString("BACKEND");
                log("Backend: " + backEnd);

                if (backEnd.contains("UIAutomator")){
                    ClientAdaptor.type = 0;
                }else if (backEnd.contains("Xposed")){
                    ClientAdaptor.type = 1;
                }else{
                    log("unsupported backend: " + backEnd + " default UIAutomator");
                }
            }

            if (config_json.has("WEBVIEW")) {
                Boolean webview = config_json.getBoolean("WEBVIEW");
                if(webview) {
                    log("retrieve web content");
                    CommonUtil.WEBVIEW = true;
                    CmdUtil.startProcess();
                }
            }

            if (config_json.has("DEEPLINK")) {
                Boolean deeplink = config_json.getBoolean("DEEPLINK");
                if(deeplink) {
                    CommonUtil.DEEPLINK = true;
                }
            }

            String pkg = config_json.getString("PACKAGE");
            ConnectUtil.setUp(pkg);
            JSONArray device_config = config_json.getJSONArray("DEVICES");

            if (argv.length > 0) {
                scheduler = new Scheduler(argv[0], devices);
                if (argv[0].contains("-p")) return;
            }else
                scheduler = new Scheduler("", devices);

            if (config_json.has("SCREENSHOT")){
                CommonUtil.SCREENSHOT = config_json.getBoolean("SCREENSHOT");
            }

            for(int i=0; i < device_config.length(); i++){
                JSONObject c = device_config.getJSONObject(i);
                String serial = c.getString("SERIAL");
                if (!ClientUtil.connected(serial)){
                    log(serial + " not connected");
                    continue;
                }

                String ip = "http://" + c.getString("IP");

                String pass = "";
                if (c.has("PASSWORD")) pass = c.getString("PASSWORD");

                if (ip.contains("127.0.0.1")) {
                    //ShellUtils2.execCommand("adb -s " + serial + " forward tcp:" + c.getInt("PORT") + " tcp:6161");
                    ShellUtils2.execCommand("adb -s " + serial + " forward tcp:7008 tcp:6161");
                }

                Device d;
                if (argv.length >0 && argv[0].contains("-r")){
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.DFSGraph);
                } else if(argv.length >0 && argv[0].contains("-d")) {
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.DEBUG);
                } else if(argv.length >0 && argv[0].contains("-s")) {
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.SPIDER);
                } else if(argv.length >0 && argv[0].contains("-m")) {
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.MONKEY);
                    if(config_json.has("TARGET_ACTIVITY")) {
                        String target = config_json.getString("TARGET_ACTIVITY");
                        d.setTargetActivity(target);
                    }
                } else if(argv.length >0 && argv[0].contains("-b")) {
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.BEFORESPIDER);
                    if(config_json.has("TARGET_ACTIVITY")) {
                        String target = config_json.getString("TARGET_ACTIVITY");
                        d.setTargetActivity(target);
                    }
                } else if(argv.length >0 && argv[0].contains("-n")) {
                    CommonUtil.log("in spider mode!");
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.NEWSPIDER);
                    assert config_json.has("TARGET_ACTIVITY");
                    CommonUtil.SCREENSHOT = false;
                    CommonUtil.SPIDER = true;
                    String target = config_json.getString("TARGET_ACTIVITY");
                    d.setTargetActivity(target);
                    log("Target：" + target);
                } else {
                    d = new Device(ip, c.getInt("PORT"), serial, pkg, pass, Device.MODE.DFS);
                }

                scheduler.bind(d);
            }


            for (String key: devices.keySet()){
                devices.get(key).start();
            }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
