package com.sei.util;

import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import static com.sei.util.CommonUtil.log;

/**
 * Created by vector on 16/5/10.
 */
public class ViewUtil {
    public static String generate_path(ViewNode node){
        ArrayList<ViewNode> list = new ArrayList<>();
        do {
            list.add(node);
            node = node.getParent();
        }while(node != null);
        int size = list.size();
        log(Integer.toString(size));
        String xpath = "";
        for (int i = size-2; i >= 0; --i) {
            String[] spl = list.get(i).getViewTag().split("\\.");
            xpath += ('/' + spl[spl.length-1]);

        }
        return xpath.substring(1);
    }

    public static String generate_xpath(ViewNode node){
        ArrayList<ViewNode> list = new ArrayList<>();
        do {
            list.add(node);
            node = node.getParent();
        }while(node != null);
        int size = list.size();
        String xpath = "";
        for (int i = 0; i < size; ++i){
            log(list.get(i).getViewTag());
        }
        for (int i = size-2; i >= 0; --i) {
            String[] spl = list.get(i).getViewTag().split("\\.");
            xpath += ('/' + spl[spl.length-1]);

        }
        return xpath.substring(1);
    }



    public static List<ViewNode>  getViewByXpath(ViewNode root, String xpath){
        class tmp{
            public ViewNode v;
            public int d;
            tmp(ViewNode v, int d){
                this.v = v; this.d = d;
            }
        }
        if (xpath.contains("@")){
            ArrayList<ViewNode> list  = new ArrayList<>();
            ArrayList<ViewNode> stack = new ArrayList<>();
            stack.add(root);
            while(!stack.isEmpty()){
                ViewNode node = stack.remove(0);
                if (node.xpath != null && node.xpath.equals(xpath)){
                    list.add(node);
                }
                stack.addAll(node.getChildren());
            }
            return list;
        }
        List<ViewNode> list = new ArrayList<>();
        ArrayDeque<tmp> queue=new ArrayDeque<>();
        String[] pathNodes = xpath.split("/");
        queue.push(new tmp(root, 1));
        while(!queue.isEmpty()){
            tmp t = queue.pop();
            ViewNode v = t.v;
            if (t.d == pathNodes.length)
                list.add(v);
            else{
                for (ViewNode child : v.getChildren()){
//                    Log.i("liuyi", "children: " + SerializeUtil.toBase64(child));
                    if (child.getViewTag().contains(pathNodes[t.d]))
                        queue.add(new tmp(child, t.d+1));
                }
            }
        }
        return list;
    }

    public static ViewNode getViewByPath(ViewNode node, String path){
        String xpath = path.substring(0, path.indexOf("#"));
        List<ViewNode> vl = getViewByXpath(node, xpath);
        int i = Integer.parseInt(path.substring(path.indexOf("#") + 1));
        if (vl.size() < i+1) {
            log("xpath: " + path + " can not locate node");
            return null;
        }
        ViewNode vn = vl.get(i);
        return vn;
    }

    public static String getLast(String name){
        if (!name.contains("."))
            return name;
        String[] words = name.split("\\.");
        return  words[words.length-1];
    }
}
