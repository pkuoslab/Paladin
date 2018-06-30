package com.sei.server.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.Collection.Stack.FragmentStack;
import com.sei.bean.Collection.Tuple2;
import com.sei.bean.View.ViewTree;
import com.sei.modules.DFGraphStrategy;
import com.sei.modules.DepthFirstStrategy;
import com.sei.modules.ModelReplay;
import com.sei.modules.Strategy;
import com.sei.util.CommonUtil;
import com.sei.util.SerializeUtil;
import com.sei.util.ShellUtils2;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class Scheduler {
    //List<Device> devices;
    Map<String, Device> devices;
    Map<String, FragmentStack> stacks;
    public GraphAdjustor graphAdjustor;
    Strategy[] strategys;

    Map<Tuple2<Integer, Integer>, Integer> ErrorLog;

    public Scheduler(String argv, Map<String, Device> devices){
        //devices = new HashMap<>();
        this.devices = devices;
        stacks = load();
        graphAdjustor = new GraphAdjustor(argv);
        ErrorLog = new HashMap<>();
        strategys = new Strategy[]{new ModelReplay(graphAdjustor, devices),
                new DepthFirstStrategy(graphAdjustor, devices),
                new DFGraphStrategy(graphAdjustor, devices)};
//        if (argv.contains("-r")) {
//            strategy = new DFGraphStrategy(graphAdjustor, devices);
//        }else if (argv.contains("-p")){
//            strategy = new ModelReplay(graphAdjustor, devices);
//        }else
//            strategy = new DepthFirstStrategy(graphAdjustor, devices);
    }

    public void bind(Device d){
        if (devices.containsKey(d.serial)){
            log(d.serial, " has not finished");
            return;
        }
        devices.put(d.serial, d);
        //int id = devices.size()-1;
        CommonUtil.log("device #" + d.serial);
        if (stacks == null)
            d.bind(this, graphAdjustor);
        else{
            //d.bind(this, graphAdjustor, stacks.get(id));
        }
    }

    // 作出决策，继续点击或调度
    public synchronized Decision update(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        if (prev_decision.code == Decision.CODE.STOP){
            devices.remove(serial);
        }
        Device d = devices.get(serial);
        Strategy strategy = strategys[d.MODE];
        return strategy.make(serial, currentTree, newTree, prev_decision, response);
    }

    public void log(String serial, String info){
        CommonUtil.log("device #" + serial + ": " + info);
    }

    public void save(){
        try {
            CommonUtil.log("save stacks");
            File file = new File("stacks.json");
            FileWriter writer = new FileWriter(file);
            //List<FragmentStack> stacks = new ArrayList<>();
            Map<String, FragmentStack> stacks = new HashMap<>();
            for(String key : devices.keySet())
                stacks.put(key, devices.get(key).fragmentStack);
            String content = SerializeUtil.toBase64(stacks);
            writer.write(content);
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Map<String, FragmentStack> load(){
        File stackf = new File( "./stacks.json");
        if (!stackf.exists()) return null;
        String str = CommonUtil.readFromFile("./stacks.json");
        Map<String, FragmentStack> stacks = JSON.parseObject(str, new TypeReference<Map<String, FragmentStack>>(){});
        CommonUtil.log("stack number: " + stacks.size());
        return stacks;
    }

}
