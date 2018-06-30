package com.sei.util;


import com.sei.agent.Device;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import com.sei.util.client.ClientAdaptor;
import jdk.nashorn.tools.Shell;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sei.util.CommonUtil.ADB_PATH;
import static com.sei.util.CommonUtil.log;

/**
 * Created by mike on 17-9-10.
 */

public class ClientUtil{

    public static void main(String[] argv){
        if(connected("192.168.59.101:5555")){
            log("connected!");
        }else{
            log("not connected");
        }
    }

    public static void record(String ins, int test_case){
//        if (ins.equals("start"))
//            ConnectUtil.sendInstruction("record", "start;" + test_case);
//        else if (ins.equals("stop"))
//            ConnectUtil.sendInstruction(ConnectUtil.launch_pkg, "record", "stop");
    }

    public static void test(){
        System.out.println("this is reflection test");
    }

    public static int execute_action(Device d, int code, ViewTree tree, String path){
        String trans = URLEncoder.encode(path);
        String action = "";
        switch (code){
            case Action.action_list.CLICK:
                action = "click";
                break;
            case Action.action_list.ENTERTEXT:
                action = "enterText";
                trans = path;
                break;
            case Action.action_list.MENU:
                action = "menu";
                trans = "";
                break;
            case Action.action_list.BACK:
                action = "back";
                trans = "";
                break;
        }
        String s =  ConnectUtil.sendInstruction(d, "execute_action", action + ";" + trans);
        return checkStatus(d, s);
    }


    public static void click(Device d, int x, int y){
        ConnectUtil.sendInstruction(d, "random_click", x + ";" + y);
    }

    public static ViewTree getCurrentTree(Device d){
        ConnectUtil.sendInstruction(d, "refreshUI", "");
        String s = ConnectUtil.sendInstruction(d, "getCurrentTree", "");
        String treeStr;
        ViewTree tree;
        String pkg = getForeground(d);
        while (!s.contains("Success")){
            d.log("Client fail to write, pkg: " + pkg + ", current: " + d.current_pkg);
            if (!checkRespond(d)) handleAppNotRespond(d);

            CommonUtil.sleep(2000);
            if (!pkg.equals(d.current_pkg) && pkg.contains(ConnectUtil.prefix)) {
                d.current_pkg = pkg;
            }

            ConnectUtil.sendInstruction(d, "refreshUI", "");
            s = ConnectUtil.sendInstruction(d, "getCurrentTree", "");
            if (s.contains("Success"))
                break;
            else {
                d.log("Client fail to write, pkg: " + pkg + ", current: " + d.current_pkg);
                d.log("get tree: " + s);
                return null;
            }
        }

        String tree_file = "tree-" + d.serial + ".json";
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  + " pull sdcard/tree.json " + CommonUtil.DIR + tree_file);
        treeStr = CommonUtil.readFromFile(CommonUtil.DIR + tree_file);

        tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        tree.setActivityName(getTopActivityName(d));
        return tree;
    }

    public static ViewTree getTree(Device d){
        String treeStr = ConnectUtil.sendInstruction(d, "getTree", "");
        ViewTree tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        return tree;
    }

    public static String getForeground(Device device){
        return ClientAdaptor.getForeground(device);
    }

    public static void handleAppNotRespond(Device d){
        ClientAdaptor.handleAppNotRespond(d);
    }

    public static List<Integer> parse_coordinate(String xml_path){
        return ClientAdaptor.parse_coordinate(xml_path);
    }

    public static Boolean checkRespond(Device d){
        return ClientAdaptor.checkRespond(d);
    }

    public static String getTopActivityName(Device device){
        return ClientAdaptor.getTopActivityName(device);
    }

    public static int checkStatus(Device d, String status){
        int response = Device.UI.OUT;
        if (status.contains("Same"))
            response = Device.UI.SAME;
        else if (status.contains("New"))
            response = Device.UI.NEW;
        else{
            d.log("status: " + status.replace("\n", "/"));
            String pkg = getForeground(d);
            d.log("process: " + pkg);
            if (pkg.contains(ConnectUtil.prefix)) {
                d.current_pkg = pkg.replace("\n", "");
                if (getCurrentTree(d) != null) {
                    response = Device.UI.PIDCHANGE;
                }else {
                    response = Device.UI.OUT;
                }
            }else if (pkg.contains("packageinstaller")){
                if(!checkPermission(d)){
                    pkg = getForeground(d);
                    if (pkg.contains(ConnectUtil.prefix) && getCurrentTree(d) != null)
                        return Device.UI.NEW;
                }else
                    return Device.UI.OUT;

            }else{
//                goBack(d);
//                pkg = getForeground(d);
//                if (!pkg.contains(ConnectUtil.prefix)){
//                    goBack(d);
//                    pkg = getForeground(d);
//                }
//                if (pkg.contains(ConnectUtil.prefix) && getCurrentTree(d) != null)
//                    return Device.UI.NEW;
//                else
//                    return Device.UI.OUT;
                return Device.UI.OUT;
            }
        }
        return response;
    }

    public static Boolean checkPermission(Device d){
        Boolean checked = false;

        String f = getForeground(d);
        CommonUtil.sleep(2000);
        while (f.contains("packageinstaller")){
            d.current_pkg = "com.android.packageinstaller";
            ConnectUtil.sendInstruction(d, "refreshUI", "");
            ViewTree tree = getTree(d);
            if (tree == null || tree.root == null) break;

            List<ViewNode> nodes = tree.fetch_clickable_nodes();
            for(ViewNode node : nodes){
                if (node.getViewTag().contains("Button") &&
                        (node.getViewText().contains("Allow") || node.getViewText().contains("允许"))) {
                    int x = node.getX() + node.getWidth() / 2;
                    int y = node.getY() + node.getHeight() / 2;
                    d.log("Allow permission");
                    checked = true;
                    click(d, x, y);
                    break;
                }
            }
            CommonUtil.sleep(1000);
        }

        f = getForeground(d);
        if (!checked && f.contains("packageinstaller")){
            ClientAdaptor.goBack(d);
        }

        ConnectUtil.force_stop("com.android.packageinstaller");
        d.current_pkg = ConnectUtil.launch_pkg;
        return checked;
    }

    public static Boolean login(Device d, ViewTree tree){
        Boolean SUCCESS = false;
        List<ViewNode> nodes = tree.fetch_clickable_nodes();
        for(ViewNode node : nodes){
            if (node.getViewTag().contains("EditText")){
                int x = node.getX() + node.getWidth() / 2;
                int y = node.getY() + node.getHeight() / 2;
                click(d, x, y);
                break;
            }
        }

        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  +" shell input text " + d.password);
        CommonUtil.sleep(1000);
        for (ViewNode node : nodes){
            String text = node.getViewText();
            if (text != null && (text.contains("登录") || text.contains("Log"))){
                log("log in");
                int x = node.getX() + node.getWidth() / 2;
                int y = node.getY() + node.getHeight() / 2;
                click(d, x, y);
                break;
            }
        }

        String login_activity = tree.getActivityName();
        CommonUtil.sleep(5000);
        String current_activity = getTopActivityName(d);
        if (!current_activity.contains(login_activity)){
            d.log("current activity: " + current_activity);
            log("log in successfully");
            return true;
        }else{
            CommonUtil.sleep(5000);
            current_activity = getTopActivityName(d);
            if (!current_activity.equals(login_activity)) {
                d.log("current activity: " + current_activity);
                log("log in successfully");
                return true;
            }else {
                log("log fail");
                return false;
            }
        }
    }

    public static Boolean connected(String serial){
        ShellUtils2.CommandResult result = ShellUtils2.execCommand(ADB_PATH + "adb devices");
        String[] lines = result.successMsg.split("\n");
        if (lines.length <= 1) return false;

        for(int i=1; i < lines.length; i++){
            String[] cp = lines[i].split("\t");
            if (cp[0].equals(serial)) return true;
        }

        return false;
    }
}
