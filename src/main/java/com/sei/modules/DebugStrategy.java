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

public class DebugStrategy implements Strategy{
    GraphAdjustor graphAdjustor;
    Map<String, Device> devices;
    //调度错误的日志
    //Key: (目标设备serial, 调度设备serial)
    //内容: 调度设备serial中恢复到栈的某个位置错误
    Map<Tuple2<String, String>, Integer> ErrorLog;


    public DebugStrategy(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
        ErrorLog = new HashMap<>();
    }

    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        CommonUtil.log("now in debug mode");
        CommonUtil.sleep(1000000000);
        return new Decision(Decision.CODE.STOP);
    }
}
