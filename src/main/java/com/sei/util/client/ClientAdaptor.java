package com.sei.util.client;

import com.sei.agent.Device;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.ShellUtils2;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.swing.text.View;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientAdaptor {
    public interface TYPE{
        int UIAUTOMATOR = 0;
        int XPOSED = 1;
    }
    public static int type = 0;
    static Class<?>[] clzz = new Class[]{ClientAutomator.class, ClientUtil.class};

    public static ViewTree getCurrentTree(Device d) throws Exception{
        Method m = clzz[type].getMethod("getCurrentTree", Device.class);
        return (ViewTree) m.invoke(null, d);
    }

    public static int execute_action(Device d, int code, ViewTree tree, String path) throws Exception{
        Class<?>[] parameters = new Class[]{Device.class, int.class, ViewTree.class, String.class};
        Method m = clzz[type].getMethod("execute_action", parameters);
        return (int) m.invoke(null, d, code, tree, path);
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
            return result;
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
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
        CommonUtil.sleep(5000);
    }

    public static Boolean checkPermission(Device d) throws Exception{
        Method m = clzz[type].getMethod("checkPermission", Device.class);
        return (Boolean) m.invoke(null, d);
    }

    public static void goBack(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input keyevent 4";
        ShellUtils2.execCommand(command);
        CommonUtil.sleep(1000);
    }

    public static Boolean login(Device d, ViewTree tree) throws Exception{
        Method m = clzz[type].getMethod("login", Device.class, ViewTree.class);
        return (Boolean) m.invoke(null, d, tree);
    }

    public static void scrollUp(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input swipe 300 300 500 1000";
        ShellUtils2.execCommand(command);
    }

    public static void scrollDown(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input swipe 500 1000 300 300";
        ShellUtils2.execCommand(command);
    }

    public static void click(Device d, int x, int y){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input tap " + x + " " + y;
        ShellUtils2.execCommand(command);
    }

    public static void clickMenu(Device d){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input keyevent 82";
        ShellUtils2.execCommand(command);
    }

    public static void enterText(Device d, String text){
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell input text " + text;
        ShellUtils2.execCommand(command);
    }
}
