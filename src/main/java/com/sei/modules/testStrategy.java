package com.sei.modules;

import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sei.util.CommonUtil.log;
import static com.sei.util.CommonUtil.shuffle;

public class testStrategy extends Strategy {
    public void run(){
        ClientUtil.refreshUI();
        for (int i = 0; i < 10; i++) {
            ClientUtil.refreshUI();
            currentTree = ClientUtil.getCurrentTree();
            if (currentTree != null) {
                break;
            }
            CommonUtil.sleep(1000);
        }

        if (currentTree == null) {
            log("can not get tree, give up");
            return;
        }

        //GraphManager.initiate(currentTree);
        //ConnectUtil.upload(currentTree, GraphManager.appGraph);
        List<String> xpaths = currentTree.getClickable_list();
        List<Integer> foots = new ArrayList<>();
        int xpath_index = shuffle(foots, xpaths.size());
        String xpath = xpaths.get(xpath_index);
        log("xpath: " + xpath_index + "/" + xpaths.size());
        List<ViewNode> vl = ViewUtil.getViewByXpath(currentTree.root, xpath);
        int ser = shuffle(foots, vl.size());

        log("path: " + ser + "/" + vl.size());
        //这里要发送点击
        String path = xpath + "#" + ser;
        String status = ClientUtil.execute_action(Action.action_list.CLICK, path);
        log("status: " + status);

        Map<String, Integer> map = new HashMap<>();

    }
}
