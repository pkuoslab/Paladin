package com.sei.modules;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.Collection.Tuple2;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;
import com.sei.util.CommonUtil;
import com.sei.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpiderStrategy implements Strategy{
    GraphAdjustor graphAdjustor;
    Map<String, Device> devices;
    //调度错误的日志
    //Key: (目标设备serial, 调度设备serial)
    //内容: 调度设备serial中恢复到栈的某个位置错误
    Map<Tuple2<String, String>, Integer> ErrorLog;
    List<String> deepLinks = new ArrayList<String>();       //记录已经爬到的页面的deeplink


    public SpiderStrategy(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
        ErrorLog = new HashMap<>();
    }

    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        Action new_action = null;
        Device device = devices.get(serial);

        //是要爬的页面
        if (newTree.getActivityName().equals(device.targetActivity)) {
            CommonUtil.log("find target Activity!!!");
            //比较content hash。
            String deepLink = CommonUtil.getDeeplink();
            //CommonUtil.log("deep link:" + deepLink);
            if (!deepLinks.contains(deepLink)) {
                CommonUtil.log("new content!");
                //记录并截图
                newTree.setDeeplink(deepLink);
                CommonUtil.SCREENSHOT = true;
                CommonUtil.getSnapshot(newTree, device);
                CommonUtil.SCREENSHOT = false;
                deepLinks.add(deepLink);
            } else {
                CommonUtil.log("old content!");
            }
            //不继续点击，回到他的父页面。
            CommonUtil.log(newTree.getActivityName() + "_" + newTree.getTreeStructureHash() + " is over");
            //此节点已点击完毕，判断此节点是否是栈顶
            //log(serial, "has finished dfs");
            if (device.fragmentStack.getSize() >= 1) {
                device.fragmentStack.pop();
                return new Decision(Decision.CODE.GO, device.fragmentStack.top().getSignature());
            }else {
                return new Decision(Decision.CODE.STOP);
            }
        }

        if (prev_decision.code == Decision.CODE.CONTINUE && response != Device.UI.SAME)
            update_graph(device, prev_decision, currentTree, newTree, response);

        if (response == Device.UI.OUT)
            return new Decision(Decision.CODE.RESTART);

        if (prev_decision.code == Decision.CODE.SEQ && response != Device.UI.NEW)
            ErrorLog.put(new Tuple2<>(serial, prev_decision.target_serial), prev_decision.position);

        int top = device.fragmentStack.getSize() - 1;

        int p = device.fragmentStack.getPosition(newTree);
        //在栈中
        if (p != top)
            return new Decision(Decision.CODE.GO, device.fragmentStack.top().getSignature());

        new_action = select_action(device, newTree);
        if (new_action != null)
            return new Decision(Decision.CODE.CONTINUE, new_action);

        CommonUtil.log(newTree.getActivityName() + "_" + newTree.getTreeStructureHash() + " is over");
        //此节点已点击完毕，判断此节点是否是栈顶
        //log(serial, "has finished dfs");
        if (device.fragmentStack.getSize() >= 1) {
            device.fragmentStack.pop();
            return new Decision(Decision.CODE.GO, device.fragmentStack.top().getSignature());
        }else {
            return new Decision(Decision.CODE.STOP);
        }
    }

    public void log(String serial, String info){
        CommonUtil.log("device #" + serial + ": " + info);
    }

    public Action select_action(Device d, ViewTree tree){
        return graphAdjustor.getAction(tree);
    }

    public void update_graph(Device d, Decision prev_decision, ViewTree currentTree, ViewTree newTree, int response){
        graphAdjustor.update(d, prev_decision.action, currentTree, newTree, response);
    }
}
