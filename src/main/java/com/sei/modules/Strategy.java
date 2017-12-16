package com.sei.modules;

import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.Collection.Stack.GraphManagerWithStack;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;

import static com.sei.util.CommonUtil.log;

public class Strategy extends Thread {
    String name;
    String description;
    ViewTree currentTree;
    public volatile boolean EXIT = false;
    public volatile boolean RUNNING = false;
    public volatile boolean DEBUG = false;
    public volatile boolean VERIFY = false;

    public Boolean initiate(){
        ClientUtil.initiate();
        for (int i = 0; i < 10; i++) {
            ClientUtil.initiate();
            currentTree = ClientUtil.getCurrentTree();
            if (currentTree != null) {
                return true;
            }
            CommonUtil.sleep(1000);
        }

        if (currentTree == null) {
            log("can not get tree, give up");
            EXIT = true;
            RUNNING = false;
            return false;
        }
        return true;
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
}
