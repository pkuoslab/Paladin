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
        Action action = this.graphAdjustor.getEdgetActionInOrder(d, tree);

        List<String> webFragments = graphAdjustor.appGraph.getWebFragments();
        if (action != null && webFragments.contains(action.target)){

        }else{

        }
        String name = tree.getActivityName() + "_" + tree.getTreeStructureHash();
        if (webFragments.contains(name)){

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
