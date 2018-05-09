package com.sei.util;


import com.sei.agent.Device;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import jdk.nashorn.tools.Shell;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.net.ConnectException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sei.util.CommonUtil.log;

/**
 * Created by mike on 17-9-10.
 */

public class ClientUtil{

    public static void main(String[] argv){
        Device d = new Device("", 6161, "4d000ff73f0c6001", "com.tencent.mm", "monkeymonkey");
        System.out.println(getTopActivityName(d));
    }

    public static void record(String ins, int test_case){
//        if (ins.equals("start"))
//            ConnectUtil.sendInstruction("record", "start;" + test_case);
//        else if (ins.equals("stop"))
//            ConnectUtil.sendInstruction(ConnectUtil.launch_pkg, "record", "stop");
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
            case Action.action_list.MENU:
                action = "menu";
                trans = "";
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

    public static void handleAppNotRespond(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell uiautomator dump /sdcard/view.xml";
        ShellUtils2.execCommand(command);
        String xml = "view-" + d.serial + ".xml";
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  + " pull /sdcard/view.xml " + CommonUtil.DIR + xml);
        String content = CommonUtil.readFromFile(CommonUtil.DIR + xml);
        d.log("handle app not responding");
        List<Integer> coords = parse_coordinate(CommonUtil.DIR + xml);
        if (coords.size() == 2){
            command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input tap " + coords.get(0) + " " + coords.get(1);
            ShellUtils2.execCommand(command);
        }
        return;
    }

    public static List<Integer> parse_coordinate(String xml_path){
        String content = CommonUtil.readFromFile(xml_path);
        Document doc = Jsoup.parse(content, "", Parser.xmlParser());
        Elements elements = doc.getElementsByAttributeValue("resource-id", "android:id/aerr_close");
        List<Integer> coords = new ArrayList<>();
        for(Element element : elements){
            String vstr = element.attr("bounds");
            // [x1, y1][x2, y2]
            int x1 = Integer.parseInt(vstr.substring(1, vstr.indexOf(",")));
            int x2 = Integer.parseInt(vstr.substring(vstr.lastIndexOf("[")+1, vstr.lastIndexOf(",")));
            int y1 = Integer.parseInt(vstr.substring(vstr.indexOf(",")+1, vstr.indexOf("]")));
            int y2 = Integer.parseInt(vstr.substring(vstr.lastIndexOf(",")+1, vstr.lastIndexOf("]")));
            coords.add((x1 + x2)/2);
            coords.add((y1 + y2)/2);
            return coords;
        }
        return coords;
    }

    public static Boolean checkRespond(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell dumpsys window windows | grep 'Not Responding' | wc -l";
        ShellUtils2.CommandResult commandResult = ShellUtils2.execCommand(command);
        String dumpInfo = commandResult.successMsg;
        try {
            int num = Integer.parseInt(dumpInfo);
            if (num > 0) return false;
            else return true;
        }catch (Exception e){
            return true;
        }
    }

    public static String getTopActivityName(Device device){
        try {
            String command = CommonUtil.ADB_PATH + "adb -s " + device.serial + " shell dumpsys window windows | grep mCurrentFocus";
            ShellUtils2.CommandResult commandResult = ShellUtils2.execCommand(command);
            String dumpInfo = commandResult.successMsg;
            String result;
            int start = dumpInfo.lastIndexOf(".");
            int stop = dumpInfo.indexOf("}");
            if (start == -1 || stop == -1){
                CommonUtil.sleep(2000);
                commandResult = ShellUtils2.execCommand(command);
                dumpInfo = commandResult.successMsg;
                start = dumpInfo.lastIndexOf(".");
                stop = dumpInfo.indexOf("}");
            }

            if (dumpInfo.contains(ConnectUtil.launch_pkg))
                result = dumpInfo.substring(start+1, stop);
            else{
                command = CommonUtil.ADB_PATH + "adb -s " + device.serial + " shell dumpsys window windows | grep mFocusedApp";
                commandResult = ShellUtils2.execCommand(command);
                start = commandResult.successMsg.lastIndexOf(".");
                stop = commandResult.successMsg.lastIndexOf(" ");
                result = commandResult.successMsg.substring(start+1, stop);
            }

//            if (dumpInfo.contains(".")) {
//                int start = dumpInfo.lastIndexOf(".");
//                int stop = dumpInfo.lastIndexOf(" ");
//                result = dumpInfo.substring(start + 1, stop);
//            } else {
//                command = CommonUtil.ADB_PATH + "adb -s " + device.serial + " shell dumpsys window windows | grep mCurrentFocus";
//                dumpInfo = ShellUtils2.execCommand(command).successMsg;
//                int start = dumpInfo.lastIndexOf(".");
//                int stop = dumpInfo.indexOf("}");
//                result = dumpInfo.substring(start + 1, stop);
//            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
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

    public static void goBack(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input keyevent 4";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(1000);
    }
//    public static String getSerIntent(){
//        String intentStr = ConnectUtil.sendInstruction("getIntent", "");
//        return intentStr;
//    }

    public static void startApp(Device d, String pkg){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(8000);
        d.current_pkg = pkg;
    }

    public static void stopApp(Device d, String pkg){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell am force-stop " + pkg;
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(5000);
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

            List<ViewNode> nodes = tree.get_clickable_nodes();
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
            goBack(d);
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
}
