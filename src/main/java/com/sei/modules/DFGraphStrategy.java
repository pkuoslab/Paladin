package com.sei.modules;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.ActivityNode;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;

import java.util.List;
import java.util.Map;

public class DFGraphStrategy extends DepthFirstStrategy{
    public DFGraphStrategy(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        super(graphAdjustor, devices);
    }

    @Override
    public Action select_action(Device d, ViewTree tree){
        //改为选择边进行遍历
        Action action = this.graphAdjustor.getEdgetActionInOrder(d, tree);
        //通知测webview的程序即将要点击
        List<String> webFragments = graphAdjustor.appGraph.getWebFragments();
        if (action != null && webFragments.contains(action.target)){
            //点击后即将出现webview
        }else{
            //点击后无webview
        }
        String name = tree.getActivityName() + "_" + tree.getTreeStructureHash();
        if (webFragments.contains(name)){
            //当前测试的是webview
            //通知测试webview的程序，并等待测试结束
        }
        return action;
    }

    @Override
    public void custom(String serial){
        int tot = 0;
        int visit = 0;
        for(ActivityNode a: graphAdjustor.appGraph.getActivities()){
            tot += a.getFragments().size();
            for(FragmentNode f: a.getFragments()){
                if (f.VISIT == true){
                    visit += 1;
                }else{
                    log(serial, f.getSignature());
                }
            }
        }

        log(serial, "total fragments: " + tot + " visit: " + visit);
    }
}
