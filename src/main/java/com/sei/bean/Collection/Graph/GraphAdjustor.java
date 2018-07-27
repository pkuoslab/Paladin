package com.sei.bean.Collection.Graph;

import com.sei.agent.Device;
import com.sei.bean.Collection.Stack.RuntimeFragmentNode;
import com.sei.bean.Collection.UiTransition;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.SerializeUtil;
import com.sei.util.client.ClientAdaptor;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static com.sei.util.CommonUtil.log;

public class GraphAdjustor extends UiTransition{
    public AppGraph appGraph;
    public AppGraph reGraph;
    Boolean REPLAY_MODE = false;
    String RECENT_INTENT_TIMESTAMP = "";

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
            public int adjust(Device d, Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                action.setIntent(getSerIntent(d));
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addInterpath(action);
                ActivityNode activityNode = new ActivityNode(new_tree.getActivityName());
                FragmentNode frag_cur = new FragmentNode(new_tree);
                activityNode.appendFragment(frag_cur);
                if (!REPLAY_MODE) {
                    appGraph.appendActivity(activityNode);
                    CommonUtil.upload(appGraph, new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                }else {
                    appGraph.transfer_actions(frag_cur);
                    reGraph.appendActivity(activityNode);
                }

                return UI.NEW_ACT;
            }
        });

        registerHandler(UI.OLD_ACT_NEW_FRG, new Handler() {
            @Override
            public int adjust(Device d, Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                action.setIntent(getSerIntent(d));
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addInterpath(action);
                AppGraph graph;
                if (!REPLAY_MODE) {
                    graph = appGraph;
                }else
                    graph = reGraph;
                ActivityNode activityNode = graph.find_Activity(new_tree.getActivityName());
                FragmentNode frag_cur = new FragmentNode(new_tree);
                if (REPLAY_MODE) appGraph.transfer_actions(frag_cur);

                activityNode.appendFragment(frag_cur);
                CommonUtil.upload(appGraph, new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_ACT_NEW_FRG;
            }
        });

        registerHandler(UI.OLD_ACT_OLD_FRG, new Handler() {
            @Override
            public int adjust(Device d, Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                action.setIntent(getSerIntent(d));
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addInterpath(action);
                CommonUtil.upload(appGraph, new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_ACT_OLD_FRG;
            }
        });

        registerHandler(UI.NEW_FRG, new Handler() {
            @Override
            public int adjust(Device d, Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addIntrapath(action);
                ActivityNode activityNode;
                if (!REPLAY_MODE)
                    activityNode = appGraph.find_Activity(new_tree.getActivityName());
                else
                    activityNode = reGraph.find_Activity(new_tree.getActivityName());
                FragmentNode frag_cur = new FragmentNode(new_tree);
                activityNode.appendFragment(frag_cur);
                if (REPLAY_MODE) appGraph.transfer_actions(frag_cur);
                CommonUtil.upload(appGraph, new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                return UI.NEW_FRG;
            }
        });

        registerHandler(UI.OLD_FRG, new Handler() {
            @Override
            public int adjust(Device d, Action action, ViewTree currentTree, ViewTree new_tree) {
                action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                FragmentNode frag_prev = locate(currentTree);
                frag_prev.addIntrapath(action);
                CommonUtil.upload(appGraph, new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
                return UI.OLD_FRG;
            }
        });
    }


    public int update(Device d, Action action, ViewTree currentTree, ViewTree new_tree, int response){
        if (action == null){
            log("device #" + d.serial + "'s first node");
            locate(currentTree);
            CommonUtil.getSnapshot(currentTree, d);
            return 0;
        }

        if (response == Device.UI.OUT && !ClientUtil.getForeground(d).equals(ConnectUtil.launch_pkg)){
            String current_app = ClientUtil.getForeground(d);
            String act = ClientUtil.getTopActivityName(d);
            action.setTarget(current_app + "_" + act);
            action.setIntent(getSerIntent(d));
            FragmentNode fragmentNode = locate(currentTree);
            fragmentNode.interAppPaths.add(action);
            d.log("add interAppPath " + current_app + "_" + act);
            return 0;
        }else if(response == Device.UI.OUT){
            return 0;
        }

        if (currentTree.getTreeStructureHash() == new_tree.getTreeStructureHash())
            return UI.OLD_FRG;

        AppGraph graph;
        if (!REPLAY_MODE)
            graph = appGraph;
        else
            graph = reGraph;

        Handler handler = handler_table.get(queryGraph(graph, d, currentTree, new_tree));
        return handler.adjust(d, action, currentTree, new_tree);

    }

    public int queryGraph(AppGraph graph, Device d, ViewTree currentTree, ViewTree new_tree){
        ActivityNode actNode = graph.getAct(new_tree.getActivityName());
        String name = new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash();
        if (new_tree.hasWebview){
            d.log("detect webview " + new_tree.getActivityName() + "_" + new_tree.getTreeStructureHash());
        }

        if(!currentTree.getActivityName().equals(new_tree.getActivityName())){
            if (actNode == null){
                log("device #" + d.serial + ": brand new activity " + name);
                CommonUtil.getSnapshot(new_tree, d);
                return UI.NEW_ACT;
            }else{
                FragmentNode fragmentNode = actNode.find_Fragment(new_tree);
                if (fragmentNode == null){
                    log("device #" + d.serial + ": old activity brand new fragment " + name);
                    CommonUtil.getSnapshot(new_tree, d);
                    return UI.OLD_ACT_NEW_FRG;
                }else{
                    if (actNode.getFragment(fragmentNode.structure_hash) == null){
                        actNode.fragments.add(fragmentNode);
                        CommonUtil.getSnapshot(new_tree, d);
                    }
                    log("device #" + d.serial + ": old activity and old fragment " + name);
                    return UI.OLD_ACT_OLD_FRG;
                }

            }
        }else{
            FragmentNode fragmentNode = actNode.find_Fragment(new_tree);
            if(fragmentNode == null){
                log("device #" + d.serial + ": brand new fragment " + name);
                CommonUtil.getSnapshot(new_tree, d);
                return UI.NEW_FRG;
            }else{
                log("device #" + d.serial + ": old fragment " + name);
                if (actNode.getFragment(fragmentNode.structure_hash) == null) {
                    actNode.fragments.add(fragmentNode);
                    CommonUtil.getSnapshot(new_tree, d);
                }
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

            if (reGraph != null){
                file = new File("re_graph.json");
                FileWriter writer1 = new FileWriter(file);
                content = SerializeUtil.toBase64(reGraph);
                writer1.write(content);
                writer1.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void load(String argv){
        try{
            if (argv.contains("-r")){
                REPLAY_MODE = true;
                reGraph = new AppGraph();
                reGraph.setPackage_name(ConnectUtil.launch_pkg);
            }

            String graphStr = CommonUtil.readFromFile("graph.json");
            appGraph = (AppGraph) SerializeUtil.toObject(graphStr, AppGraph.class);
            for(ActivityNode actNode: appGraph.getActivities()){
                for(FragmentNode frgNode: actNode.getFragments()) {
                    frgNode.VISIT = false;
                    frgNode.clicked_edges = new ArrayList<>();
                    frgNode.setColor("white");
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

    public void resetColor(){
        for(ActivityNode activityNode: appGraph.activities){
            for(FragmentNode fragmentNode: activityNode.fragments){
                fragmentNode.setColor("white");
            }
        }
    }

    public FragmentNode locate(ViewTree tree){
        AppGraph graph;
        if (!REPLAY_MODE) graph = appGraph;
        else graph = reGraph;

        ActivityNode activityNode = graph.getAct(tree.getActivityName());
        FragmentNode fragmentNode = null;
        if (activityNode == null){
            log("fail to find " + tree.getActivityName() + "_" + tree.getTreeStructureHash());
            activityNode = new ActivityNode(tree.getActivityName());
            graph.appendActivity(activityNode);
            fragmentNode = new FragmentNode(tree);
            activityNode.appendFragment(fragmentNode);
            if (REPLAY_MODE) appGraph.transfer_actions(fragmentNode);
            return fragmentNode;
        }

        fragmentNode = activityNode.find_Fragment(tree);

        if (fragmentNode == null){
            log("fail to locate " + tree.getActivityName() + "_" + tree.getTreeStructureHash());
            fragmentNode = new FragmentNode(tree);
            activityNode.appendFragment(fragmentNode);
            if (REPLAY_MODE) appGraph.transfer_actions(fragmentNode);
        }

        if (activityNode.getFragment(tree.getTreeStructureHash()) == null){
            activityNode.appendFragment(fragmentNode);
        }

        return fragmentNode;
    }

    public Action getAction(ViewTree currentTree){
        FragmentNode currentNode = locate(currentTree);
        //log("current node: " + currentNode.getSignature());
        Action action = null;
        if (currentNode.path_index.size() < currentNode.path_list.size()) {
            int ser = CommonUtil.shuffle(currentNode.path_index, currentNode.path_list.size());

            currentNode.path_index.add(ser);
            log(currentNode.getSignature() +  " path: " + currentNode.path_index.size() + "/" + currentNode.path_list.size());

            String path = currentNode.path_list.get(ser);
            if (currentNode.edit_fields.contains(path)) {
                action = new Action(path, Action.action_list.ENTERTEXT);
            }else if (path.equals("menu")){
                action = new Action(path, Action.action_list.MENU);
            }else
                action = new Action(path, Action.action_list.CLICK);
        }else
            currentNode.setTraverse_over(true);
        return action;
    }

    public Action getEdgeAction(Device d, ViewTree currentTree){
        FragmentNode currentNode = locate(currentTree);
        Action action = null;
        while (currentNode.path_index.size() < currentNode.path_list.size()) {
            int ser = CommonUtil.shuffle(currentNode.path_index, currentNode.path_list.size());
            currentNode.path_index.add(ser);
            String path = currentNode.path_list.get(ser);
            if (currentNode.edit_fields.contains(path)) {
                action = new Action(path, Action.action_list.ENTERTEXT);
            }else {
                action = new Action(path, Action.action_list.CLICK);
            }

            if (currentNode.targets.size() == 0) break;

            int index = currentNode.targets.get(ser).indexOf("_");
            String target_act = currentNode.targets.get(ser).substring(0, index);
            int target_hash = Integer.parseInt(currentNode.targets.get(ser).substring(index+1));
            FragmentNode node = appGraph.getFragment(target_act, target_hash);
            if (node == null) break;

            int p = d.fragmentStack.getPosition(target_act, target_hash, node.get_Clickable_list());
            if (p == -1) break;
            else{
                //d.log(target_act + "_" + target_hash + " in stack " + p);
                continue;
            }
        }

        if (currentNode.path_index.size() >= currentNode.path_list.size())
            currentNode.setTraverse_over(true);

        return action;

    }

    public String getSerIntent(Device d){
        if (!CommonUtil.INTENT) return null;

        String record = ConnectUtil.sendHttpGet(d.ip + "/intent");
        int idx = record.indexOf("$");
        if (idx == -1) return null;
        String timestamp = record.substring(0, idx);

        if (timestamp.equals(RECENT_INTENT_TIMESTAMP))
            return null;
        else{
            RECENT_INTENT_TIMESTAMP = timestamp;
            //d.log(record);
            return record.substring(idx+1);
        }
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
        AppGraph graph;
        if (REPLAY_MODE) graph = reGraph;
        else graph = appGraph;

        ActivityNode actNode = graph.getAct(tree.getActivityName());
        if (actNode == null) return null;
        FragmentNode fragmentNode = actNode.find_Fragment(tree);
        if (fragmentNode != null && !actNode.fragments.contains(fragmentNode)){
            actNode.fragments.add(fragmentNode);
        }
        return fragmentNode;
    }

    public Map<String, List<String>> getAllNodesTag(){
        AppGraph graph;
        if (REPLAY_MODE) graph = reGraph;
        else graph = appGraph;

        Map<String, List<String>> tags = new HashMap<>();
        for(int i=0; i < graph.getActivities().size(); i++){
            ActivityNode act = graph.getActivities().get(i);
            tags.put(act.activity_name, new ArrayList<>());
            for(int j=act.getFragments().size()-1; j>=0; j--){
                FragmentNode frg = act.getFragments().get(j);
                tags.get(act.activity_name).add(String.valueOf(frg.getStructure_hash()));
            }
        }
        return tags;
    }

    public Action getInterPathAction(Device d, ViewTree tree){
        FragmentNode fragmentNode = locate(tree);
        if (fragmentNode != null && fragmentNode.interpaths.size() > 0){
            for (Action action: fragmentNode.interpaths){
                String act = action.target_activity;
                if (d.fragmentStack.contains(act))
                    return action;
            }
            return null;
        }else
            return null;
    }

    public void tie(RuntimeFragmentNode runtimeFragmentNode, ViewTree tree, Action action){
        AppGraph graph;
        if (REPLAY_MODE) graph = reGraph;
        else graph = appGraph;

        ActivityNode actNode = graph.getAct(runtimeFragmentNode.getActivity());
        FragmentNode fragmentNode = actNode.getFragment(runtimeFragmentNode.getStructure_hash());
        action.setTarget(tree.getActivityName(), tree.getTreeStructureHash());
        if (tree.getActivityName().equals(actNode.getActivity_name())){
            fragmentNode.addIntrapath(action);
        }else{
            fragmentNode.addInterpath(action);
        }
    }

    public List<Action> BFS(FragmentNode start, FragmentNode end){
        Queue<FragmentNode> queue = new LinkedList<>();
        queue.add(start);
        start.setColor("gray");
        while(!queue.isEmpty()){
            FragmentNode processing = queue.poll();
            if (loop(processing.getIntrapaths(), end, processing, queue)){
                return buildPath(end);
            }else if (loop(processing.getInterpaths(), end, processing, queue)){
                return buildPath(end);
            }
        }
        return null;
    }

    private List<Action> buildPath(FragmentNode end){
        List<Action> path = new ArrayList<>();
        FragmentNode tmp;
        while(end.getPrevious() != null){
            path.add(end.getAction());
            tmp = end;
            end = end.getPrevious();
            tmp.setPrevious(null);
        }
        return path;
    }

    private boolean loop(List<Action> actions, FragmentNode end, FragmentNode processing, Queue<FragmentNode> queue){
        for(Action action : actions){
            FragmentNode n = appGraph.getFragment(action.target_activity, action.target_hash);
            if (n == null){
                continue;
            }
            if (n.getColor().equals("white")){
                n.setPrevious(processing);
                n.setAction(action);
                n.setColor("gray");
                if (n.getActivity().equals(end.getActivity()) &&
                        n.getStructure_hash() == end.getStructure_hash()){
                    //log("search "  + n.getSignature() + " success!"
                    //+ "previous " + n.getPrevious().getSignature());
                    return true;
                }else{
                    queue.add(n);
                }
            }
        }
        return false;
    }

    public FragmentNode getFragmentInGraph(ViewTree tree){
        ActivityNode actNode = appGraph.getAct(tree.getActivityName());
        if (actNode == null){
            log(tree.getActivityName() + " not found");
        }
        return actNode.find_Fragment_in_graph(tree);
    }
}
