package com.sei.util;


import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            ConnectUtil.sendInstruction("record", "stop");
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

    public static ViewTree queryUIChanged(ViewTree currentTree){
        String treeStr = ConnectUtil.sendInstruction("getCurrentTree", "");
        ViewTree new_tree = (ViewTree) SerializeUtil.toObject(treeStr, ViewTree.class);
        if (new_tree.getTreeStructureHash() != currentTree.getTreeStructureHash()){
            log("new tree!");
            return new_tree;
        }else{
            return null;
        }
    }

    public static String getForeground(String info){
        String command = CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell dumpsys activity activities";
        ShellUtils2.CommandResult commandResult = ShellUtils2.execCommand(command);
        String dumpInfo = commandResult.successMsg;
        //log("dumpinfo: " + dumpInfo);
        Pattern p = Pattern.compile("ProcessRecord\\{.*\\}");
        Matcher m = p.matcher(dumpInfo);
        String result = "";
        if (m.find()){
            result = m.group(0);
            if (info.equals("PKG")) {
                int start = result.indexOf(":");
                int stop = result.indexOf("/");
                result = result.substring(start + 1, stop);
            }else if (info.equals("PID")){
                int start = result.indexOf(" ");
                int stop = result.indexOf(":");
                result = result.substring(start + 1, stop);
            }
        }

        return result;

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
            String pkgName = getForeground("PKG");
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
            }else{
                log("Process changed: " + status.replace("\n", "") + " prefix: " + ConnectUtil.prefix + " pkgname: " + pkgName);
                String s = ConnectUtil.sendInstruction("execute_action", "back;");
                if (!s.contains("Stop")) {
                    ClientUtil.refreshUI();
                    ViewTree tree = ClientUtil.getCurrentTree();
                    if (tree == null)
                        return Status.OUT;
                    else
                        return Status.SAME;
                }else
                    return Status.OUT;
            }
            return Status.PIDCHANGE;
        }else {
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

    public static void startPkg(String pkg){
        String command = CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(2000);
    }
}
