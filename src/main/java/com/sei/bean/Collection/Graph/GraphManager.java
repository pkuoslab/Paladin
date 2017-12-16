package com.sei.bean.Collection.Graph;


import com.sei.bean.Collection.Tuple2;
import com.sei.bean.Collection.UiTransition;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.SerializeUtil;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static com.sei.util.CommonUtil.log;


/**
 * Created by mike on 17-9-16.
 */

public class GraphManager extends UiTransition{
    public AppGraph appGraph;
    public ActivityNode activityNode;
    public FragmentNode fragmentNode;
    public List<String> route_list = new ArrayList<>();
    public Boolean is_upload = false;


    public GraphManager(ViewTree currentTree){
        graphManagerFactor(currentTree);
    }

    public GraphManager(){}

    public void graphManagerFactor(ViewTree currentTree){
        if (appGraph == null) {
            appGraph = new AppGraph();
            appGraph.setPackage_name(ConnectUtil.current_pkg);
        }
        if (currentTree != null) {
            activityNode = new ActivityNode();
            activityNode.setActivity_name(currentTree.getActivityName());
            activityNode.setSer_intent(ClientUtil.getSerIntent());
            appGraph.appendActivity(activityNode);
            fragmentNode = new FragmentNode(currentTree);
            activityNode.appendFragment(fragmentNode);
        }
        registerAllHandlers();
    }

    @Override
    public void registerAllHandlers(){
        registerHandler(UI.NEW_ACT, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                fragmentNode.addInterpath(action);
                activityNode = new ActivityNode(new_tree.getActivityName());
                String ser_intent = ClientUtil.getSerIntent();
                activityNode.setSer_intent(ser_intent);
                appGraph.appendActivity(activityNode);
                fragmentNode = new FragmentNode(new_tree);
                activityNode.appendFragment(fragmentNode);
                return 0;
            }
        });

        registerHandler(UI.OLD_ACT_NEW_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                fragmentNode.addInterpath(action);
                activityNode = appGraph.getAct(new_tree.getActivityName());
                fragmentNode = new FragmentNode(new_tree);
                activityNode.appendFragment(fragmentNode);
                return 0;
            }
        });

        registerHandler(UI.OLD_ACT_OLD_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                fragmentNode.addInterpath(action);
                activityNode = appGraph.getAct(new_tree.getActivityName());
                fragmentNode = activityNode.find_Fragment(new_tree);
                return 0;
            }
        });

        registerHandler(UI.NEW_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                fragmentNode.addIntrapath(action);
                fragmentNode = new FragmentNode(new_tree);
                activityNode.appendFragment(fragmentNode);
                return 0;
            }
        });

        registerHandler(UI.OLD_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                fragmentNode.addIntrapath(action);
                fragmentNode = activityNode.find_Fragment(new_tree);
                return 0;
            }
        });
    }


    @Override
    public int update(Action action, ViewTree currentTree, ViewTree new_tree){
       int status = queryGraph(currentTree, new_tree);
       Handler handler = handler_table.get(status);
       return handler.adjust(action, currentTree, new_tree);
//       switch (status){
//           case UI.NEW_ACT:
//               fragmentNode.addInterpath(action);
//               activityNode = new ActivityNode(new_tree.getActivityName());
//               String ser_intent = ClientUtil.getSerIntent();
//               activityNode.setSer_intent(ser_intent);
//               appGraph.appendActivity(activityNode);
//               fragmentNode = new FragmentNode(new_tree);
//               activityNode.appendFragment(fragmentNode);
//               break;
//           case UI.OLD_ACT_NEW_FRG:
//               fragmentNode.addInterpath(action);
//               activityNode = appGraph.getAct(new_tree.getActivityName());
//               fragmentNode = new FragmentNode(new_tree);
//               activityNode.appendFragment(fragmentNode);
//               break;
//           case UI.OLD_ACT_OLD_FRG:
//               fragmentNode.addInterpath(action);
//               activityNode = appGraph.getAct(new_tree.getActivityName());
//               fragmentNode = activityNode.find_Fragment(new_tree);
//               break;
//           case UI.NEW_FRG:
//               fragmentNode.addIntrapath(action);
//               fragmentNode = new FragmentNode(new_tree);
//               activityNode.appendFragment(fragmentNode);
//               break;
//           case UI.OLD_FRG:
//               fragmentNode = activityNode.find_Fragment(new_tree);
//               break;
//       }
    }

    public int queryGraph(ViewTree currentTree, ViewTree new_tree){
        if(!currentTree.getActivityName().equals(new_tree.getActivityName())){
            ActivityNode actNode = appGraph.getAct(new_tree.getActivityName());
            if (actNode == null){
                log("brand new activity " + new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                return UI.NEW_ACT;
            }else if(actNode.find_Fragment(new_tree) == null){
                log("old activity brand new fragment " + actNode.getActivity_name() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_ACT_NEW_FRG;
            }else{
                log("old activity and old fragment " + actNode.getActivity_name() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_ACT_OLD_FRG;
            }
        }else{
            if(activityNode.find_Fragment(new_tree) == null){
                log("brand new fragment " + new_tree.getActivityName() + " " +  new_tree.getTreeStructureHash());
                return UI.NEW_FRG;
            }else{
                log("old fragment " + new_tree.getTreeStructureHash());
                return UI.OLD_FRG;
            }
        }
    }

    @Override
    public void save(){
        try {
            File file = new File("graph.json");
            FileWriter writer = new FileWriter(file);
            String content = SerializeUtil.toBase64(appGraph);
            writer.write(content);
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void load(){
        try{
            String graphStr = CommonUtil.readFromFile("graph.json");
            appGraph = (AppGraph) SerializeUtil.toObject(graphStr, AppGraph.class);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void reset(){
        appGraph = null;
        fragmentNode = null;
        activityNode = null;
    }

    public List<FragmentNode> getAllNodes(){
        List<FragmentNode> nodes = new ArrayList<>();
        for(int i=0; i < appGraph.getActivities().size(); i++){
            ActivityNode act = appGraph.getActivities().get(i);
            for(int j=act.getFragments().size()-1; j>=0; j--){
                FragmentNode frg = act.getFragments().get(j);
                nodes.add(frg);
            }
        }

        return nodes;
    }

    public List<FragmentNode> getRouteNodes(List<String> route_list){
        List<FragmentNode> frgNodes = new ArrayList<>();
        Boolean interrupt = false;
        for(String route : route_list){
            if (interrupt){
                log("\ttherefore, can not access " + route);
                continue;
            }

            int index = route.lastIndexOf("_");
            ActivityNode acn = appGraph.getAct(route.substring(0, index));
            if (acn == null){
                interrupt = true;
                log("can not locate " + route);
                continue;
            }
            FragmentNode frg = acn.getFragment(Integer.parseInt(route.substring(index+1)));
            if (frg == null){
                interrupt = true;
                log("can not locate " + route);
                continue;
            }
            frgNodes.add(frg);
        }
        return frgNodes;
    }

    public ActivityNode getActivityNode(){return activityNode;}

    public void setActivityNode(ActivityNode activityNode){this.activityNode = activityNode;}

    public FragmentNode getFragmentNode() {return fragmentNode;}

    public void setFragmentNode(FragmentNode fragmentNode){this.fragmentNode = fragmentNode;}

    public List<List<Tuple2<FragmentNode, Action>>> findOnePath(FragmentNode start, FragmentNode end){
        FragmentNode end_node = BreathFirstSearch(start, end);
        List<List<Tuple2<FragmentNode, Action>>> paths = new ArrayList<>();
        if (end_node != null) {
            List<Tuple2<FragmentNode, Action>> path = buildPath(end_node);
            paths.add(path);
        }else
            log("no path");

        resetColor();
        return paths;
    }

    private void resetColor(){
        for(ActivityNode actNode : appGraph.getActivities()){
            for(FragmentNode frgNode: actNode.getFragments()){
                if (frgNode.getColor().equals("gray"))
                    frgNode.setColor("white");
            }
        }
    }

    FragmentNode BreathFirstSearch(FragmentNode start, FragmentNode end){
        Queue<FragmentNode> queue = new LinkedList<>();
        queue.add(start);
        start.setColor("gray");
        while(!queue.isEmpty()){
            FragmentNode processing = queue.poll();
            List<Tuple2<FragmentNode, Action>> adjPointers = getAdjPointers(processing);
            for(Tuple2<FragmentNode, Action> pointer : adjPointers){
                FragmentNode adjNode = pointer.getFirst();
                Action action = pointer.getSecond();
                if (adjNode.getColor().equals("white")){
                    adjNode.setPrevious(processing);
                    adjNode.setAction(action);
                    adjNode.setColor("gray");

                    if (adjNode.getStructure_hash() == end.getStructure_hash()){
                        log("search graph success");
                        return end;
                    }else
                        queue.add(adjNode);
                }
            }
        }
        return null;
    }

    List<Tuple2<FragmentNode, Action>> buildPath(FragmentNode end_node){
        List<Tuple2<FragmentNode, Action>> path = new ArrayList<>();

        FragmentNode tmp;
        while(end_node.getPrevious() != null){
            path.add(new Tuple2<>(end_node, end_node.getAction()));
            tmp = end_node;
            end_node = end_node.getPrevious();
            tmp.setPrevious(null);
        }
        return path;
    }

    List<Tuple2<FragmentNode, Action>> getAdjPointers(FragmentNode node){
        List<Action> actions = node.getIntrapaths();
        actions.addAll(node.getInterpaths());
        List<Tuple2<FragmentNode, Action>> pointers = new ArrayList<>();
        for(Action action : actions){
            FragmentNode frgNode =  getFragmentByTag(action.target_activity, action.target_hash);
            if (frgNode == null) continue;
            pointers.add(new Tuple2<>(frgNode, action));
        }
        return pointers;
    }

    FragmentNode getFragmentByTag(String activity, int hash){
        ActivityNode actNode = appGraph.getAct(activity);
        if (actNode == null){
            log("fail to find " + activity);
            return null;
        }
        FragmentNode frgNode = actNode.getFragment(hash);
        if (frgNode == null) log("failed to find " + activity + "_" + hash);
        return frgNode;
    }

    public FragmentNode getFragmentInGraph(ViewTree tree){
        ActivityNode actNode = appGraph.getAct(tree.getActivityName());
        return actNode.find_Fragment_in_graph(tree);
    }
}