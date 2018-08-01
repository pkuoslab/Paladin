package com.sei.modules;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.ActivityNode;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sei.util.CommonUtil.log;

public class ModelReplay implements Strategy{
    private FragmentNode start;
    public GraphAdjustor graphAdjustor;
    Map<String, Device> devices;
    private Map<String, List<Action>> actionTable;
    private Map<String, Integer> replayLimit;
    int test_case = 0;

    public ModelReplay(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
        actionTable = new HashMap<>();
        replayLimit = new HashMap<>();
    }

    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        Device d = devices.get(serial);
        if (prev_decision.action == null){
            if (d.getRoute_list().size() == 0){
                return new Decision(Decision.CODE.STOP);
            }
            log(serial, "To " + d.getRoute_list().get(0));
            replayLimit.put(serial, 0);
            Decision decision = replay(newTree, d.serial, d.getRoute_list().get(0));
            if (decision.code == Decision.CODE.CONTINUE){
                //即将执行一个操作
            }
            return decision;
        }

        //FragmentNode currentNode = graphAdjustor.locate(newTree);
        String activity = prev_decision.action.target_activity;
        int hash = prev_decision.action.target_hash;
        FragmentNode expectNode = graphAdjustor.appGraph.getFragment(activity, hash);
        List<Action> paths = actionTable.get(serial);
        //log("current: " + newTree.getTreeStructureHash() + " expect: " + expectNode.getSignature());
        if (paths.size() < 1){
            if (newTree.getActivityName().equals(expectNode.getActivity()) &&
                    expectNode.calc_similarity(newTree.getClickable_list()) > 0.7)
                d.visits.add(expectNode.getSignature());
            return new Decision(Decision.CODE.STOP);
        }

        Action action = paths.remove(paths.size()-1);
        String xpath;
        if (action.path.indexOf("#") != -1){
            int idx = action.path.indexOf("#");
            xpath = action.path.substring(0, idx);
        }else{
            xpath = action.path;
        }

        if ((newTree.getClickable_list().contains(xpath) || xpath.equals("menu"))
                && newTree.getActivityName().equals(expectNode.getActivity())){
            d.visits.add(expectNode.getSignature());
            // 即将执行一个操作

            return new Decision(Decision.CODE.CONTINUE, action);
        }else if (currentTree.getTreeStructureHash() != newTree.getTreeStructureHash()){
            return replay(newTree, d.serial, d.getRoute_list().get(0));
        }else{
            // for debug
            log("xpath: " + xpath);
            for(String s: newTree.getClickable_list()){
                log(s);
            }
        }
        return new Decision(Decision.CODE.STOP);
    }

    private List<Action> buildPath(ViewTree tree, String serial, String target){
        //FragmentNode start = graphAdjustor.locate(tree);
        Device d = devices.get(serial);

        FragmentNode start = graphAdjustor.getFragmentInGraph(tree);
        FragmentNode end = graphAdjustor.appGraph.getFragment(target);
        List<Action> actions = null;
        if (end == null) return null;

        if (start != null) {
            log("start: " + start.getActivity() + "_" + start.getStructure_hash());
            if (start.getStructure_hash() == end.getStructure_hash()) {
                return null;
            }
            actions = graphAdjustor.BFS(start, end);
            graphAdjustor.resetColor();
            if (actions != null) {
                d.visits.add(start.getSignature());
                return actions;
            }
        }

        // 如果找不到起始点，就在该activity内的节点逐个试一遍
        ActivityNode an = graphAdjustor.appGraph.getAct(tree.getActivityName());
        for(FragmentNode fn : an.getFragments()){
            if (start != null && fn.getStructure_hash() == start.getStructure_hash())
                continue;
            actions = graphAdjustor.BFS(fn, end);
            graphAdjustor.resetColor();
            if (actions != null){
                String path = actions.get(actions.size()-1).path;
                int idx = path.indexOf("#");
                log("re-search " + path);
                if (idx == -1 || tree.getClickable_list().contains(path.substring(0,idx))){
                    d.visits.add(fn.getSignature());
                    log("match start: " + fn.getSignature() + " " + path +
                            " rate: " + fn.calc_similarity(tree.getClickable_list()));
                    return actions;
                }
            }
        }

        return null;
    }

    private Decision replay(ViewTree tree, String serial, String target){
        if (replayLimit.get(serial) > 4){
            return new Decision(Decision.CODE.STOP);
        }

        replayLimit.put(serial, replayLimit.get(serial)+1);
        List<Action> paths = buildPath(tree, serial, target);
        if (paths != null){
            log("path size: " + paths.size());
            if (checkWebViewOrder(target, paths)) {
                actionTable.put(serial, paths);
                Action action = actionTable.get(serial).remove(paths.size() - 1);
                return new Decision(Decision.CODE.CONTINUE, action);
            }else{
                return new Decision(Decision.CODE.STOP);
            }
        }else {
            return new Decision(Decision.CODE.STOP);
        }
    }

    private boolean checkWebViewOrder(String target, List<Action> actions){
        List<String> webFragments = graphAdjustor.appGraph.getWebFragments();
        if (!webFragments.contains(target)){
            return true;
        }
        int target_index = webFragments.indexOf(target);
        for(Action action: actions){
            String t = action.target;
            if (webFragments.contains(t)){
                int idx = webFragments.indexOf(t);
                if (idx > target_index){
                    log("encounter webview which should be test after");
                    return false;
                }
            }
        }
        return true;

    }
}
