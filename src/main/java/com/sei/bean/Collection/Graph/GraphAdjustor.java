package com.sei.bean.Collection.Graph;

import com.sei.bean.Collection.UiTransition;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.SerializeUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sei.util.CommonUtil.log;

public class GraphAdjustor extends UiTransition{
    public AppGraph appGraph;
    Boolean REPLAY_MODE = false;
    Map<String, List<String>> explored_tags;

    public GraphAdjustor(String argv){
        File graph = new File( "./graph.json");
        if (graph.exists()) load(argv);

        if (appGraph == null) {
            appGraph = new AppGraph();
            appGraph.setPackage_name(ConnectUtil.launch_pkg);
        }
        registerAllHandlers();
    }

    @Override
    public void registerAllHandlers(){
        registerHandler(UI.NEW_ACT, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addInterpath(action);
                ActivityNode activityNode = new ActivityNode(new_tree.getActivityName());
                FragmentNode frag_cur = new FragmentNode(new_tree);
                activityNode.appendFragment(frag_cur);
                appGraph.appendActivity(activityNode);
                return UI.NEW_ACT;
            }
        });

        registerHandler(UI.OLD_ACT_NEW_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addInterpath(action);
                ActivityNode activityNode = appGraph.find_Activity(new_tree.getActivityName());
                FragmentNode frag_cur = new FragmentNode(new_tree);
                activityNode.appendFragment(frag_cur);
                return UI.OLD_ACT_NEW_FRG;
            }
        });

        registerHandler(UI.OLD_ACT_OLD_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addInterpath(action);
                return UI.OLD_ACT_OLD_FRG;
            }
        });

        registerHandler(UI.NEW_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addIntrapath(action);
                ActivityNode activityNode = appGraph.find_Activity(new_tree.getActivityName());
                FragmentNode frag_cur = new FragmentNode(new_tree);
                activityNode.appendFragment(frag_cur);
                return UI.NEW_FRG;
            }
        });

        registerHandler(UI.OLD_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addIntrapath(action);
                return UI.OLD_FRG;
            }
        });
    }


    public int update(int id, Action action, ViewTree currentTree, ViewTree new_tree){
        if (action == null){
            log("device #" + id + "'s first node");
            if (REPLAY_MODE){
                String act = currentTree.getActivityName();
                int hash = currentTree.getTreeStructureHash();
                explored_tags.put(act, new ArrayList<>());
                explored_tags.get(act).add(String.valueOf(hash));
            }else
                locate(currentTree);
            return 0;
        }

        if (currentTree.getTreeStructureHash() == new_tree.getTreeStructureHash())
            return UI.OLD_FRG;


        if (!REPLAY_MODE) {
            Handler handler = handler_table.get(queryGraph(id, currentTree, new_tree));
            return handler.adjust(action, currentTree, new_tree);
        }else
            return handleTags(id, currentTree, new_tree);
    }

    public int handleTags(int id, ViewTree currentTree, ViewTree new_tree){
        String activity = new_tree.getActivityName();
        int hash = new_tree.getTreeStructureHash();
        if (!explored_tags.containsKey(activity)) {
            log("device #" + id + ": new activity " + activity);
            explored_tags.put(activity, new ArrayList<>());
            explored_tags.get(activity).add(String.valueOf(hash));
        }else if (!explored_tags.get(activity).contains(String.valueOf(hash))) {
            log("device #" + id + ": new fragment " + activity + "_" + hash);
            explored_tags.get(activity).add(String.valueOf(hash));
        }
        return 0;
    }

    public int queryGraph(int id, ViewTree currentTree, ViewTree new_tree){
        ActivityNode actNode = appGraph.getAct(new_tree.getActivityName());

        if(!currentTree.getActivityName().equals(new_tree.getActivityName())){
            if (actNode == null){
                log("device #" + id + ": brand new activity " + new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                return UI.NEW_ACT;
            }else if(actNode.find_Fragment(new_tree) == null){
                log("device #" + id + ": old activity brand new fragment " + actNode.getActivity_name() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_ACT_NEW_FRG;
            }else{
                log("device #" + id + ": old activity and old fragment " + actNode.getActivity_name() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_ACT_OLD_FRG;
            }
        }else{
            if(actNode.find_Fragment(new_tree) == null){
                log("device #" + id + ": brand new fragment " + new_tree.getActivityName() + " " +  new_tree.getTreeStructureHash());
                return UI.NEW_FRG;
            }else{
                log("device #" + id + ": old fragment " + new_tree.getTreeStructureHash());
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
    public void load(String argv){
        try{
            if (argv.contains("-r")){
                REPLAY_MODE = true;
                explored_tags = new HashMap<>();
            }

            String graphStr = CommonUtil.readFromFile("graph.json");
            appGraph = (AppGraph) SerializeUtil.toObject(graphStr, AppGraph.class);
            for(ActivityNode actNode: appGraph.getActivities()){
                for(FragmentNode frgNode: actNode.getFragments()) {
                    frgNode.clicked_edges = new ArrayList<>();
                    frgNode.intrapath_index = new ArrayList<>();
                    frgNode.interpath_index = new ArrayList<>();
                    if (argv.contains("-r")){
                        frgNode.setTraverse_over(false);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void reset(){
        appGraph = null;
    }

    public FragmentNode locate(ViewTree tree){
        ActivityNode activityNode = appGraph.getAct(tree.getActivityName());
        FragmentNode fragmentNode = null;
        if (activityNode == null){
            log("fail to find " + tree.getActivityName() + "_" + tree.getTreeStructureHash());
            activityNode = new ActivityNode(tree.getActivityName());
            appGraph.appendActivity(activityNode);
            fragmentNode = new FragmentNode(tree);
            activityNode.appendFragment(fragmentNode);
            return fragmentNode;
        }

        fragmentNode = activityNode.find_Fragment(tree);
        if (fragmentNode == null){
            log("fail to locate " + tree.getActivityName() + "_" + tree.getTreeStructureHash());
            fragmentNode = new FragmentNode(tree);
            activityNode.appendFragment(fragmentNode);
        }

        return fragmentNode;
    }

    public Action getAction(ViewTree currentTree){
        FragmentNode currentNode = locate(currentTree);
        Action action = null;
        if (currentNode.path_index.size() < currentNode.path_list.size()) {
            int ser = CommonUtil.shuffle(currentNode.path_index, currentNode.path_list.size());
            currentNode.path_index.add(ser);
            String path = currentNode.path_list.get(ser);
            if (currentNode.edit_fields.contains(path))
                action = new Action(path, Action.action_list.ENTERTEXT);
            else
                action = new Action(path, Action.action_list.CLICK);
        }else
            currentNode.setTraverse_over(true);
        return action;
    }

    public Action getEdgeAction(ViewTree currentTree){
        FragmentNode currentNode = locate(currentTree);
        while (currentNode.interpath_index.size() < currentNode.interpaths.size()){
            int ser = CommonUtil.shuffle(currentNode.interpath_index, currentNode.interpaths.size());
            currentNode.interpath_index.add(ser);
            Action action = currentNode.interpaths.get(ser);
            if (!currentNode.clicked_edges.contains(action.target)){
                currentNode.clicked_edges.add(action.target);
                return action;
            }
        }

        while (currentNode.intrapath_index.size() < currentNode.intrapaths.size()){
            int ser = CommonUtil.shuffle(currentNode.intrapath_index, currentNode.intrapaths.size());
            currentNode.intrapath_index.add(ser);
            Action action = currentNode.intrapaths.get(ser);
            if (!currentNode.clicked_edges.contains(action.target)){
                currentNode.clicked_edges.add(action.target);
                return action;
            }
        }

        return null;
    }

    public Boolean hasAction(String activity, int hash){
        ActivityNode activityNode = appGraph.getAct(activity);
        if (activityNode == null) return false;
        FragmentNode fragmentNode = activityNode.getFragment(hash);
        if (fragmentNode == null) return false;
        if (fragmentNode.path_index.size() < fragmentNode.path_list.size()) return true;

        return false;
    }

    public Boolean match(ViewTree tree, String activity, int hash){
        ActivityNode activityNode = appGraph.getAct(activity);
        if (activityNode == null) return false;
        FragmentNode fragmentNode = activityNode.getFragment(hash);
        if (fragmentNode == null) return false;
        if (fragmentNode.calc_similarity(tree.getClickable_list()) > 0.8)
            return true;
        else
            return false;
    }

    public FragmentNode searchFragment(ViewTree tree){
        ActivityNode actNode = appGraph.getAct(tree.getActivityName());
        if (actNode == null) return null;
        return actNode.find_Fragment(tree);
    }

    public Map<String, List<String>> getAllNodesTag(){
        if (REPLAY_MODE) return explored_tags;

        Map<String, List<String>> tags = new HashMap<>();
        for(int i=0; i < appGraph.getActivities().size(); i++){
            ActivityNode act = appGraph.getActivities().get(i);
            tags.put(act.activity_name, new ArrayList<>());
            for(int j=act.getFragments().size()-1; j>=0; j--){
                FragmentNode frg = act.getFragments().get(j);
                tags.get(act.activity_name).add(String.valueOf(frg.getStructure_hash()));
            }
        }
        return tags;
    }
}
