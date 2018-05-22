package com.sei.util.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sei.agent.Device;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import com.sei.util.*;

import java.util.List;


public class ClientAutomator {

//    public static ViewTree getCurrentTree(Device d){
//        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell uiautomator dump /sdcard/view.xml";
//        ShellUtils2.execCommand(command);
//        String xml = "view-" + d.serial + ".xml";
//        ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  + " pull /sdcard/view.xml " + CommonUtil.DIR + xml);
//        String content = CommonUtil.readFromFile(CommonUtil.DIR + xml);
//        ViewTree tree = new ViewTree(d, content);
//        return tree;
//    }

    public static void main(String[] argv){
        String pwd = "/home/mike/togithub/droidwalker/droidwalker/out/artifacts/droidwalker_jar";
        String content = CommonUtil.readFromFile(pwd + "/output/com.tencent.mm/FTSAddFriendUI_-644246140.json");
        ViewTree tree = (ViewTree) SerializeUtil.toObject(content, ViewTree.class);
        String xpath = "FrameLayout/FrameLayout/FrameLayout/LinearLayout/FrameLayout/ViewGroup/FrameLayout/FrameLayout/LinearLayout/ListView/LinearLayout/LinearLayout/LinearLayout/LinearLayout/RelativeLayout/TextView";
        for(String x: tree.getClickable_list())
            System.out.println(x);
        List<ViewNode> vns = ViewUtil.getViewByXpath(tree.root, xpath);
        System.out.println(vns.size());
    }
    public static void init(Device d) throws Exception{
        String command = CommonUtil.ADB_PATH + "adb -s " + d.serial + " shell uiautomator runtest" +
                " bundle.jar uiautomator-stub.jar -c com.github.uiautomatorstub.Stub";
        Process p = Runtime.getRuntime().exec(command);
        CommonUtil.sleep(1000);
        String command2 = CommonUtil.ADB_PATH + "adb -s " + d.serial + " forward tcp:" + d.port + " tcp:9008";
        ShellUtils2.execCommand(command2);
    }



    public static ViewTree getCurrentTree(Device d){
        JSONObject data = new JSONObject();
        data.put("jsonrpc", "2.0");
        data.put("method", "dumpWindowHierarchy");
        data.put("id", 1);
        JSONArray params = new JSONArray();
        params.add(false);
        params.add("view.xml");
        data.put("params", params);

        String route = d.ip + ":" + d.port + "/jsonrpc/0";
        try {
            String response = ConnectUtil.postJson(route, data);
            if (response.contains("Success")){
                String xml = "view-" + d.serial + ".xml";
                ShellUtils2.execCommand(CommonUtil.ADB_PATH + "adb -s " + d.serial  + " pull /data/local/tmp/local/tmp/view.xml " + CommonUtil.DIR + xml);
                String content = CommonUtil.readFromFile(CommonUtil.DIR + xml);
                ViewTree tree = new ViewTree(d, content);
                return tree;
            }else{
                d.log(response);
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public static int execute_action(Device d, int code, ViewTree tree, String path){
        int[] pxy = new int[2];
        if (code == Action.action_list.CLICK){
            parse_path(tree, path, pxy);
            if (pxy[0] > CommonUtil.screen_x || pxy[0] < 0)
                return Device.UI.SAME;

            for(int i = 0; i < 6; ++i) {
                if (pxy[1] < 0) {
                    d.log("scroll up " + pxy[1]);
                    ClientAdaptor.scrollUp(d);
                    ViewTree tree1 = getCurrentTree(d);
                    parse_path(tree1, path, pxy);
                }else if(pxy[1] > CommonUtil.screen_y){
                    d.log("scroll down " + pxy[1]);
                    ClientAdaptor.scrollDown(d);
                    ViewTree tree1 = getCurrentTree(d);
                    parse_path(tree1, path, pxy);
                }
            }

            if (pxy[1] < 0 || pxy[1] > CommonUtil.screen_y)
                return Device.UI.NEW;
            else{
                ClientAdaptor.click(d, pxy[0], pxy[1]);
            }
        }

        switch (code){
            case Action.action_list.BACK:
                ClientAdaptor.goBack(d);
                break;
            case Action.action_list.MENU:
                ClientAdaptor.clickMenu(d);
                break;
            case Action.action_list.ENTERTEXT:
                ClientAdaptor.enterText(d, path);
        }

        String f = ClientAdaptor.getForeground(d);
        if (!f.contains(ConnectUtil.launch_pkg))
            return Device.UI.OUT;

        CommonUtil.sleep(800);
        ViewTree newTree = getCurrentTree(d);
        if (newTree.root == null) return Device.UI.OUT;

        if (newTree.getTreeStructureHash() != d.currentTree.getTreeStructureHash())
            return Device.UI.NEW;
        else{
            return Device.UI.SAME;
        }
    }

    public static void parse_path(ViewTree tree, String path, int[] pxy){
        ViewNode vn = ViewUtil.getViewByPath(tree.root, path);
        if (vn == null){
            pxy[0] = -1000;
            pxy[1] = -1000;
            CommonUtil.storeTree(tree);
        }
        pxy[0] = vn.getX() + vn.getWidth() / 2;
        pxy[1] = vn.getY() + vn.getHeight() / 2;

        if (pxy[0] > CommonUtil.screen_x)
            pxy[0] = (vn.getX() + CommonUtil.screen_x) / 2;
        else if (pxy[0] < 0){
            pxy[0] = (pxy[0] + vn.getX() + vn.getWidth()) / 2;
        }
    }

    public static Boolean checkPermission(Device d){
        return false;
    }

}
