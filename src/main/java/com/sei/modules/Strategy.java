package com.sei.modules;

import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.Collection.Stack.GraphManagerWithStack;
import com.sei.bean.View.ViewTree;
import com.sei.util.*;

import static com.sei.util.CommonUtil.log;

public class Strategy extends Thread {
    String name;
    String description;
    ViewTree currentTree;
    public volatile boolean EXIT = false;
    public volatile boolean RUNNING = false;

    public Boolean initiate(){
        ClientUtil.initiate();
        for (int i = 0; i < 10; i++) {
            ClientUtil.initiate();
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
            ClientUtil.initiate();
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

    public Boolean initiate(int limit){
        if (initiate())
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
        ConnectUtil.force_stop(ConnectUtil.launch_pkg);
        ClientUtil.startApp(ConnectUtil.launch_pkg);
        if (!initiate()){
            log("restart app failure");
            return false;
        }else
            return true;
    }

    void start_droidwalker(){
        ConnectUtil.force_stop("ias.deepsearch.com.helper");
        ConnectUtil.force_stop(ConnectUtil.launch_pkg);
        CommonUtil.sleep(2000);
        ClientUtil.startPkg("ias.deepsearch.com.helper");
        ShellUtils.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " shell input keyevent KEYCODE_HOME");
        CommonUtil.sleep(2000);
        ShellUtils.execCommand(CommonUtil.ADB_PATH + "adb " + CommonUtil.SERIAL + " forward tcp:6161 tcp:6161");

        ClientUtil.startApp(ConnectUtil.launch_pkg);
    }
}
