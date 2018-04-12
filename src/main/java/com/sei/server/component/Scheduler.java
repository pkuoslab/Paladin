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
import com.sei.util.CommonUtil;
import com.sei.util.SerializeUtil;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scheduler {
    List<Device> devices;
    List<FragmentStack> stacks;
    public GraphAdjustor graphAdjustor;
    DepthFirstStrategy strategy;

    Map<Tuple2<Integer, Integer>, Integer> ErrorLog;

    public Scheduler(String argv){
        devices = new ArrayList<>();
        stacks = load();
        graphAdjustor = new GraphAdjustor(argv);
        ErrorLog = new HashMap<>();
        if (argv.contains("-r")) {
            strategy = new DFGraphStrategy(graphAdjustor, devices);
        }else
            strategy = new DepthFirstStrategy(graphAdjustor, devices);
    }

    public void bind(Device d){
        devices.add(d);
        int id = devices.size()-1;
        CommonUtil.log("device #" + id + ": " + d.serial);
        if (stacks == null)
            d.bind(id, this, graphAdjustor);
        else
            d.bind(id, this, graphAdjustor, stacks.get(id));
    }

    // 作出决策，继续点击或调度
    public synchronized Decision update(int id, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        return strategy.make(id, currentTree, newTree, prev_decision, response);
    }

    public void log(int id, String info){
        CommonUtil.log("device #" + id + ": " + info);
    }

    public void save(){
        try {
            CommonUtil.log("save stacks");
            File file = new File("stacks.json");
            FileWriter writer = new FileWriter(file);
            List<FragmentStack> stacks = new ArrayList<>();
            for(Device d: devices)
                stacks.add(d.fragmentStack);
            String content = SerializeUtil.toBase64(stacks);
            writer.write(content);
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public List<FragmentStack> load(){
        File stackf = new File( "./stacks.json");
        if (!stackf.exists()) return null;
        String str = CommonUtil.readFromFile("./stacks.json");
        List<FragmentStack> stacks = JSON.parseObject(str, new TypeReference<List<FragmentStack>>(){});
        CommonUtil.log("stack number: " + stacks.size());
        return stacks;
    }

}
