package com.sei.modules.plugin.client;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Element;


public class MessageFactory {
    public static interface JS{
        int ALL_LOC = 1;
    }

    public static String buildRequest(int id, String method, JSONObject params){
        JSONObject jo = new JSONObject();
        jo.put("id", id);
        jo.put("method", method);
        if (params != null)
            jo.put("params", params);
        else
            jo.put("params", new JSONObject());

        return jo.toJSONString();
    }


    public static JSONObject buildResponse(String response){
        return JSONObject.parseObject(response);
    }

    public static JSONObject buildNode(Element node, String xpath){
        JSONObject jo = new JSONObject();
        jo.put("path", xpath);
        jo.put("tag", node.tagName());
        if (node.textNodes().size() == 0)
            jo.put("text", "");
        else
            jo.put("text", node.text());

        return jo;
    }

    public static String getJSTemplate(int id, String html_nodes){
        String js = "";

        if (id == JS.ALL_LOC){
            js = "var nodes = JSON.parse('" + html_nodes + "');" +
                    "var ret_Object = new Object();" +
                    "var ratio = window.devicePixelRatio;" +
                    "for(var node of nodes){" +
                    "xpath = node['path'];" +
                    "ret = document.evaluate(xpath, document, " +
                    "null, XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE, null);" +
                    "element = ret.snapshotItem(0);" +
                    "rect_list = element.getBoundingClientRect();" +
                    "rec = rect_list;" +
                    "node.x = ((rec['left'] + rec['right']) * ratio) / 2;" +
                    "node.y = ((rec['top'] + rec['bottom']) * ratio) / 2;" +
                    "node.h = rec['height'] * ratio;" +
                    "node.w = rec['width'] * ratio;" +
                    "}" +
                    "nodes";
        }

        return js;
    }
}
