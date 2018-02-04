package com.sei.modules.plugin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neovisionaries.ws.client.WebSocketException;
import com.sei.modules.plugin.client.BlockWebSocket;
import com.sei.modules.plugin.client.MessageFactory;
import com.sei.server.component.Handler;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.SerializeUtil;
import com.sei.util.ShellUtils2;
import fi.iki.elonen.NanoHTTPD;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

import static com.sei.util.CommonUtil.log;

public class WebviewHelper extends NanoHTTPD{
    public HashMap<String, Handler> route_table = new HashMap<>();
    public BlockWebSocket client;

    public int port = 5800;

    public static void main(String[] argv){

    }

    public WebviewHelper(){
        super(5800);
        setRoute_table();
        client = new BlockWebSocket();
    }

    public void register(String route, Handler handler){
        route_table.put(route, handler);
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

    public void setRoute_table(){
        register("/", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                return newFixedLengthResponse("WebView Helper!!!");
            }
        });

        register("/query", new Handler() {
            @Override
            public Response onRequest(IHTTPSession session) {
                // /query?info={"x":123, "y": 456, "pid" : 1080}
                String query = session.getQueryParameterString();
                JSONObject jo = JSONObject.parseObject(query.substring(5));
                //log("query: " + jo.toJSONString());
                String nodes = getHTMLNodes(jo);
                return newFixedLengthResponse(nodes);
            }
        });
    }

    private String getHTMLNodes(JSONObject info){
        String wsUrl = getWebSocketURL(info);
        if (wsUrl.equals("")) return "[]";

        JSONArray html_nodes = new JSONArray();
        Document document = getHTMLPage(wsUrl);

        if (document == null) return "[]";

        Queue<Element> nodes = new LinkedList<>();
        Queue<String> xpaths = new LinkedList<>();

        nodes.add(document.body());
        xpaths.add("/html/body");
        Map<String, Integer> tags = new HashMap<>();

        while(!nodes.isEmpty()){
            Element node = nodes.poll();
            String xpath = xpaths.poll();

            List<Element> children = node.children();
            tags.clear();

            for (Element child : children){
                nodes.add(child);

                String name = child.tagName();
                if(!tags.containsKey(name))
                    tags.put(name, 0);

                int num = tags.get(name) + 1;
                tags.put(name, num);
                xpaths.add(xpath + "/" + name + "[" + num + "]");
            }

            if (children.size() == 0) {
                JSONObject html_node = MessageFactory.buildNode(node, xpath);
                html_nodes.add(html_node);
            }
        }


        String html_nodes_annotated;

        try {
            html_nodes_annotated = annotateHTMLNodes(html_nodes.toString()).toJSONString();
        } catch (Exception e){
            html_nodes_annotated = "[]";
        }

        client.close();
        //log(html_nodes_annotated);
        return html_nodes_annotated;
    }

    private Document getHTMLPage(String wsUrl){
        try {
            //log("websocket url: " + wsUrl);
            client.connect(wsUrl);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

        Document document = null;

        try {
            String req = MessageFactory.buildRequest(0, "DOM.getDocument", null);
            JSONObject ret = MessageFactory.buildResponse(client.send(req));

            JSONObject root = ret.getJSONObject("result").getJSONObject("root");
            JSONObject params = new JSONObject();
            params.put("nodeId", root.getIntValue("nodeId"));

            req = MessageFactory.buildRequest(0, "DOM.getOuterHTML", params);
            ret = MessageFactory.buildResponse(client.send(req));

            String html_string = ret.getJSONObject("result").getString("outerHTML");
            document = Jsoup.parse(html_string);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        return document;
    }


    private String getWebSocketURL(JSONObject info){
        int x = info.getInteger("x");
        int y = info.getInteger("y");
        int pid = info.getInteger("pid");

        String url = "";
        ShellUtils2.execCommand("adb forward tcp:9222 localabstract:webview_devtools_remote_" + pid);
        try {
            String webview_info = ConnectUtil.sendHttpGet("http://localhost:9222/json");
            //log(webview_info);
            JSONArray ja = JSON.parseArray(webview_info);
            for (Object e : ja) {
                JSONObject jo = (JSONObject) e;
                String des_string = jo.getString("description");
                JSONObject des = JSONObject.parseObject(des_string);
                if (des.getBoolean("attached") && des.getBoolean("visible") &&
                    des.getInteger("screenX") == x && des.getInteger("screenY") == y){
                        url = jo.getString("webSocketDebuggerUrl");
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return url;
    }

    public JSONArray annotateHTMLNodes(String html_nodes) throws InterruptedException{
        JSONArray annotated_html_nodes = getNodesWithPosition(html_nodes);
        return annotated_html_nodes;
    }

    public JSONArray getNodesWithPosition(String html_nodes) throws InterruptedException{
        JSONObject params = new JSONObject();
        params.put("expression", MessageFactory.getJSTemplate(MessageFactory.JS.ALL_LOC, html_nodes));
        params.put("includeCommandLineAPI", true);
        params.put("returnByValue", true);
        String req = MessageFactory.buildRequest(0, "Runtime.evaluate", params);
        JSONObject ret = MessageFactory.buildResponse(client.send(req));
        return ret.getJSONObject("result").getJSONObject("result").getJSONArray("value");
    }
}
