package com.sei.modules;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;

import java.util.List;

public class DFGraphStrategy extends DepthFirstStrategy{
    public DFGraphStrategy(GraphAdjustor graphAdjustor, List<Device> devices){
        super(graphAdjustor, devices);
    }

    @Override
    public Action select_action(Device d, ViewTree tree){
        //改为选择边进行遍历
        Action action = null;
        do {
            action = this.graphAdjustor.getEdgeAction(tree);
            //if (action != null) d.log("action: to " + action.target);
            if (action == null) return action;
//            FragmentNode fragmentNode = graphAdjustor.appGraph.getFragment(action.target_activity, action.target_hash);
//            if (fragmentNode != null) {
//                int p = d.fragmentStack.getPosition(action.target_activity, action.target_hash, fragmentNode.get_Clickable_list());
//                if (p == -1) break;
//                else d.log(action.target + " in stack");
//            }
        }while(action != null);

        return action;
    }
}
