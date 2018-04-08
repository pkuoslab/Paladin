package com.sei.util;


import com.sei.agent.Device;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;

import java.net.ConnectException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sei.modules.Strategy.checkPermission;
import static com.sei.util.CommonUtil.log;

/**
 * Created by mike on 17-9-10.
 */

public class ClientUtil{

    public interface Status{
        int NEW = 0;
        int PIDCHANGE = -1;
        int OUT = -3;
        int SAME = 2;
    }

    public static void main(String[] argv){
        //log("foreground app: " + getForegroundPkg());
        return;
    }

    public static void record(String ins, int test_case){
        if (ins.equals("start"))
            ConnectUtil.sendInstruction("record", "start;" + test_case);
        else if (ins.equals("stop"))
            ConnectUtil.sendInstruction(ConnectUtil.launch_pkg, "record", "stop");
    }

    public static String execute_action(int action, String path, Boolean refresh){
        String trans = URLEncoder.encode(path);
        String status = "";
        switch (action){
            case Action.action_list.CLICK:
                status = ConnectUtil.sendInstruction("execute_action", "click;" + trans + ";true");
                break;
        }
        return status;
    }

    public static String execute_action(int action, String path){
        String trans = URLEncoder.encode(path);
        String status = "";
        switch (action){
            case Action.action_list.CLICK:
                status = ConnectUtil.sendInstruction("execute_action", "click;" + trans);
                break;
            case Action.action_list.LONGCLICK:
                status = ConnectUtil.sendInstruction("execute_action", "longClick;" + trans);
                break;
            case Action.action_list.ENTERTEXT:
                status = ConnectUtil.sendInstruction("execute_action", "enterText;" + path);
        }
        return status;
    }

    public static String execute_action(int action){
        String status = "";
        switch (action){
            case Action.action_list.BACK:
                status = ConnectUtil.sendInstruction("execute_action", "back;");
                break;
            case Action.action_list.SCROLLLEFT:
                status = ConnectUtil.sendInstruction("execute_action", "scrollLeft;");
                break;
            case Action.action_list.SCROLLRIGHT:
                status = ConnectUtil.sendInstruction("execute_action", "scrollRight;");
                break;
            case Action.action_list.MENU:
                status = ConnectUtil.sendInstruction("execute_action", "menu;");
        }
        return status;
    }

    public static int execute_action(Device d, int code, String path){
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
        }
        String s =  ConnectUtil.sendInstruction(d, "execute_action", action + ";" + trans);
        return checkStatus(d, s);
    }

    public static int execute_action(Device d, int code){
        String action = "";
        switch (code){
            case Action.action_list.BACK:
                action = "back";
                break;
            case Action.action_list.MENU:
                action = "menu";
                break;
        }
        String s =  ConnectUtil.sendInstruction(d, "execute_action", action + ";");
        return checkStatus(d, s);
    }

    public static void random_click(int times){
        for(int i=0; i < times; i++){
            int x = (int) (Math.random() * 768);
            int y = ((int) (Math.random() * 200)) + 800;
            ConnectUtil.sendInstruction("random_click", x + ";" + y);
        }
    }

    public static void click(int x, int y){
        ConnectUtil.sendInstruction("random_click", x + ";" + y);
    }

    public static void click(Device d, int x, int y){
        ConnectUtil.sendInstruction(d, "random_click", x + ";" + y);
    }

    public static ViewTree getCurrentTree(){
        String treeStr = ConnectUtil.sendOrderBeforeReadFile("getCurrentTree", "");
        ViewTree tree = null;
        if (treeStr != "")
            tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        else{
            treeStr = ConnectUtil.sendOrderBeforeReadFile("getCurrentTree", "");
            if (treeStr != "")
                tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        }
        return tree;
    }

    public static ViewTree getCurrentTree(Device d){
        ConnectUtil.sendInstruction(d, "refreshUI", "");
        String s = ConnectUtil.sendInstruction(d, "getCurrentTree", "");
        String treeStr;
        ViewTree tree;

        while (!s.contains("Success")){
            d.log("Client fail to write");
            CommonUtil.sleep(2000);
            ConnectUtil.sendInstruction(d, "refreshUI", "");
            s = ConnectUtil.sendInstruction(d, "getCurrentTree", "");
            if (s.contains("Success"))
                break;
            else {
                d.log("Client fail to write");
                return null;
            }
        }

        String tree_file = "tree-" + d.serial + ".json";
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  + " pull sdcard/tree.json " + CommonUtil.DIR + tree_file);
        treeStr = CommonUtil.readFromFile(CommonUtil.DIR + tree_file);

        tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        return tree;
    }

    public static ViewTree getTree(){
        String treeStr = ConnectUtil.sendInstruction("getTree", "");
        ViewTree tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        return tree;
    }

    public static ViewTree getTree(Device d){
        String treeStr = ConnectUtil.sendInstruction(d, "getTree", "");
        ViewTree tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        return tree;
    }

    public static void refreshUI(){
        ConnectUtil.sendInstruction("refreshUI", "");
    }

    public static void setScreenSize(int x){
        if (x != 0)
            ConnectUtil.sendInstruction("set_screen", String.valueOf(x));
        else
            ConnectUtil.sendInstruction("set_screen", "");
    }

    public static String getCurrentActivity(){
        String act = ConnectUtil.sendInstruction("getCurrentActivity", "");
        return act;
    }

    public static String getCurrentActivity(Device d){
        String act = ConnectUtil.sendInstruction(d,"getCurrentActivity", "");
        return act;
    }


    public static String getForeground(){
        String command = CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell dumpsys activity activities";
        ShellUtils2.CommandResult commandResult = ShellUtils2.execCommand(command);
        String dumpInfo = commandResult.successMsg;
        //log("dumpinfo: " + dumpInfo);
        Pattern p = Pattern.compile("ProcessRecord\\{.*\\}");
        Matcher m = p.matcher(dumpInfo);
        String result = "";
        if (m.find()){
            result = m.group(0);
            int start = result.indexOf(":");
            int stop = result.indexOf("/");
            result = result.substring(start + 1, stop);
        }

        return result;

    }

    public static String getForeground(Device device){
        String command = CommonUtil.ADB_PATH + "adb -s " + device.serial + " shell dumpsys activity activities";
        ShellUtils2.CommandResult commandResult = ShellUtils2.execCommand(command);
        String dumpInfo = commandResult.successMsg;
        Pattern p = Pattern.compile("ProcessRecord\\{.*\\}");
        Matcher m = p.matcher(dumpInfo);
        String result = "";
        if (m.find()){
            result = m.group(0);
            int start = result.indexOf(":");
            int stop = result.indexOf("/");
            result = result.substring(start + 1, stop);
        }

        return result;
    }

    public static int checkStatus(Device d, String status){
        int response = Device.UI.OUT;
        if (status.contains("Same"))
            response = Device.UI.SAME;
        else if (status.contains("New"))
            response = Device.UI.NEW;
        else{
            d.log("status: " + status.replace("\n", "/"));
            String pkg = ClientUtil.getForeground(d);
            d.log("process: " + pkg);
            if (pkg.contains(ConnectUtil.prefix)) {
                d.current_pkg = pkg.replace("\n", "");
                if (getCurrentTree(d) != null) {
                    response = Device.UI.PIDCHANGE;
                }else
                    response = Device.UI.OUT;
            }else if (pkg.contains("packageinstaller"))
                checkPermission(d);
        }
        return response;
    }

    public static int checkStatus(String status){
        status = status.replace("\n", "");
        if (status.contains("Same")){
            return Status.SAME;
        }else if(status.contains("New")){
            return Status.NEW;
        }else if(status.contains("PidChange")){
//            需要确认当前的Foreground package
            CommonUtil.sleep(1000);
            String pkgName = getForeground();
            pkgName = pkgName.replace("\n", "");
            if (pkgName.contains(ConnectUtil.prefix)) {
                ConnectUtil.current_pkg = pkgName;
                ClientUtil.refreshUI();
                ViewTree tree = ClientUtil.getCurrentTree();
                if (tree == null) {
                    return Status.OUT;
                }
            }else if(pkgName.contains("launcher")) {
                log("return to launcher, need recover");
                return Status.OUT;
            }else if (pkgName.contains("packageinstaller")){
                checkPermission();
                return Status.OUT;
            }else{
                log("Process changed: " + status.replace("\n", "") + " prefix: " + ConnectUtil.prefix + " pkgname: " + pkgName);
                ShellUtils2.execCommand("adb -s " + CommonUtil.SERIAL + " shell input keyevent 4");
                ClientUtil.refreshUI();
                ViewTree tree = ClientUtil.getCurrentTree();
                if (tree == null)
                    return Status.OUT;
                else
                    return Status.SAME;
//                String s = ConnectUtil.sendInstruction("execute_action", "back;");
//                if (!s.contains("Stop")) {
//                    ClientUtil.refreshUI();
//                    ViewTree tree = ClientUtil.getCurrentTree();
//                    if (tree == null)
//                        return Status.OUT;
//                    else
//                        return Status.SAME;
//                }else
//                    return Status.OUT;
            }
            return Status.PIDCHANGE;
        }else {
            log("error: " + status);
            return Status.OUT;
        }
    }

    public static String getSerIntent(){
        String intentStr = ConnectUtil.sendInstruction("getIntent", "");
        return intentStr;
    }

    public static void startApp(String pkg){
        String command = CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(8000);
        ConnectUtil.setUp(pkg);

    }

    public static void startApp(Device d, String pkg){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(8000);
        d.current_pkg = pkg;
    }

    public static void stopApp(Device d, String pkg){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell am force-stop " + pkg;
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(1000);
    }

    public static void startPkg(String pkg){
        String command = CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(2000);
    }

    public static Boolean checkPermission(){
        Boolean checked = false;

        String f = ClientUtil.getForeground();
        while (f.contains("packageinstaller")){
            checked = true;
            ConnectUtil.current_pkg = "com.android.packageinstaller";
            ClientUtil.refreshUI();
            ViewTree tree = ClientUtil.getTree();
            if (tree == null || tree.root == null) break;

            List<ViewNode> nodes = tree.get_clickable_nodes();

            for(ViewNode node : nodes){
                if (node.getViewTag().contains("Button") && node.getViewText().contains("Allow")) {
                    int x = node.getX() + node.getWidth() / 2;
                    int y = node.getY() + node.getHeight() / 2;
                    ClientUtil.click(x, y);
                    break;
                }
            }
            f = ClientUtil.getForeground();
            CommonUtil.sleep(500);
        }

        ConnectUtil.force_stop("com.android.packageinstaller");
        ConnectUtil.current_pkg = ConnectUtil.launch_pkg;
        return checked;
    }

    public static Boolean checkPermission(Device d){
        Boolean checked = false;

        String f = getForeground(d);
        while (f.contains("packageinstaller")){
            d.log("Allow permission");
            checked = true;
            d.current_pkg = "com.android.packageinstaller";
            ConnectUtil.sendInstruction(d, "refreshUI", "");
            ViewTree tree = getTree(d);
            if (tree == null || tree.root == null) break;

            List<ViewNode> nodes = tree.get_clickable_nodes();

            for(ViewNode node : nodes){
                if (node.getViewTag().contains("Button") && node.getViewText().contains("Allow")) {
                    int x = node.getX() + node.getWidth() / 2;
                    int y = node.getY() + node.getHeight() / 2;
                    click(d, x, y);
                    break;
                }
            }
            f = getForeground(d);
            CommonUtil.sleep(1000);
        }

        ConnectUtil.force_stop("com.android.packageinstaller");
        d.current_pkg = ConnectUtil.launch_pkg;
        return checked;
    }

    public static Boolean login(Device d, ViewTree tree){
        Boolean SUCCESS = false;
        List<ViewNode> nodes = tree.get_clickable_nodes();
        for(ViewNode node : nodes){
            if (node.getViewTag().contains("EditText")){
                int x = node.getX() + node.getWidth() / 2;
                int y = node.getY() + node.getHeight() / 2;
                click(d, x, y);
                break;
            }
        }

        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  +" shell input text " + d.password);
        for (ViewNode node : nodes){
            String text = node.getViewText();
            if (text != null && (text.contains("登录") || text.contains("Log In"))){
                log("log in");
                int x = node.getX() + node.getWidth() / 2;
                int y = node.getY() + node.getHeight() / 2;
                click(d, x, y);
                break;
            }
        }

        String login_activity = tree.getActivityName();
        CommonUtil.sleep(5000);
        String current_activity = getCurrentActivity(d);
        if (!current_activity.contains(login_activity)){
            log("log in successfully");
            return true;
        }else{
            CommonUtil.sleep(5000);
            current_activity = getCurrentActivity(d);
            if (!current_activity.equals(login_activity)) {
                log("log in successfully");
                return true;
            }else {
                log("log fail");
                return false;
            }
        }
    }
}
