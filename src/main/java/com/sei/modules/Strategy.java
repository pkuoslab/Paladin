package com.sei.modules;

import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.Collection.Stack.GraphManagerWithStack;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import com.sei.util.*;

import java.util.List;

import static com.sei.util.CommonUtil.log;

public class Strategy extends Thread {
    String name;
    String description;
    ViewTree currentTree;
    public volatile boolean EXIT = false;
    public volatile boolean RUNNING = false;

    public Boolean refresh(){
        ClientUtil.refreshUI();
        for (int i = 0; i < 10; i++) {
            ClientUtil.refreshUI();
            currentTree = ClientUtil.getCurrentTree();
            if (currentTree != null) {
                return true;
            }
            CommonUtil.sleep(1500);
        }

        if (currentTree == null) {
            log("can not get tree, restart DroidWalker");
            start_droidwalker();
        }

        for (int i = 0; i < 10; i++) {
            ClientUtil.refreshUI();
            currentTree = ClientUtil.getCurrentTree();
            if (currentTree != null) {
                return true;
            }
            CommonUtil.sleep(1500);
        }

        if (currentTree == null){
            log("can not get tree, give up");
            return false;
        }

        return true;
    }

    public Boolean refresh(int limit){
        if (refresh())
            return true;
        else{
            int time = 0;
            while(!restart() && time < limit)
                time += 1;

            if (currentTree == null) {
                log("restart too many times, give up");
                return false;
            }else
                return true;
        }
    }

    public Boolean restart(){
        //ConnectUtil.force_stop(ConnectUtil.launch_pkg);
        //ClientUtil.startApp(ConnectUtil.launch_pkg);
        if (!refresh()){
            log("restart app failure");
            return false;
        }else
            return true;
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

    void start_droidwalker(){
        ConnectUtil.force_stop("ias.deepsearch.com.helper");
        ConnectUtil.force_stop(ConnectUtil.launch_pkg);
        CommonUtil.sleep(2000);
        ClientUtil.startPkg("ias.deepsearch.com.helper");
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell input keyevent KEYCODE_HOME");
        CommonUtil.sleep(2000);
        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " forward tcp:6161 tcp:6161");

        ClientUtil.startApp(ConnectUtil.launch_pkg);
    }
}
