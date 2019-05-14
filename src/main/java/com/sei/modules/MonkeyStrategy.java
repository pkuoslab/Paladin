package com.sei.modules;

import com.alibaba.fastjson.JSON;
import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.Collection.Tuple2;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.ViewUtil;

import java.util.*;


public class MonkeyStrategy implements Strategy{
    GraphAdjustor graphAdjustor;
    Map<String, Device> devices;
    //调度错误的日志
    //Key: (目标设备serial, 调度设备serial)
    //内容: 调度设备serial中恢复到栈的某个位置错误
    Map<Tuple2<String, String>, Integer> ErrorLog;
    //List<Step> trace = new ArrayList<Step>();
    static List<String> titleList = new ArrayList<String>();
    static int traceCount = 1;
    static int stepCount = 0;
    static Random random;


    public MonkeyStrategy(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
        ErrorLog = new HashMap<>();
        random = new Random();
        Long randomSeed = new Long(new Random().nextInt());
        CommonUtil.log("random seed:" + randomSeed);
        random.setSeed(randomSeed);
        CommonUtil.random.setSeed(randomSeed);
    }

    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        Action new_action = new Action();
        Device device = devices.get(serial);


        // 新内容新结构都可以。
        if (response != Device.UI.SAME)
            update_graph(device, prev_decision, currentTree, newTree, response);

        if (response == Device.UI.OUT){
            return new Decision(Decision.CODE.RESTART);
        }

        if (prev_decision.code == Decision.CODE.SEQ && response != Device.UI.NEW)
            ErrorLog.put(new Tuple2<>(serial, prev_decision.target_serial), prev_decision.position);

        if(device.targetActivity != null && newTree.getActivityName().equals(device.targetActivity)) {
            Action action = new Action();
            action.setAction(Action.action_list.BACK);
            return new Decision(Decision.CODE.CONTINUE, action);
        }

        /*
         * 30% back
         * 70% click
         */
        int randomInt = random.nextInt(100);
        //CommonUtil.log("select a random int below 100: " + randomInt);
        if(randomInt < 20) {
            //back;
            CommonUtil.log("[monkey mode] select back");
            Action action = new Action();
            action.setAction(Action.action_list.BACK);
            return new Decision(Decision.CODE.CONTINUE, action);
        } else if (randomInt < 30) {
            //restart
            graphAdjustor.clearPathIndex();
            CommonUtil.log("[monkey mode] select restart");
            return new Decision(Decision.CODE.RESTART_F);
        } else {
            //click
            CommonUtil.log("[monkey mode] select click");
            new_action = select_action(device, newTree);
            if (new_action != null)
                return new Decision(Decision.CODE.CONTINUE, new_action);
            return new Decision(Decision.CODE.RESTART_F);
        }
    }

    public void log(String serial, String info){
        CommonUtil.log("device #" + serial + ": " + info);
    }

    public Action select_action(Device d, ViewTree tree){
        return graphAdjustor.getTextAction(tree);
    }


    public void update_graph(Device d, Decision prev_decision, ViewTree currentTree, ViewTree newTree, int response){
        graphAdjustor.update(d, prev_decision.action, currentTree, newTree, response);
    }
}
