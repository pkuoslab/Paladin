package com.sei.modules;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.ActivityNode;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.Collection.Tuple2;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static com.sei.util.CommonUtil.log;

public class ModelReplay implements Strategy{
    private FragmentNode start;
    public GraphAdjustor graphAdjustor;
    Map<String, Device> devices;
    private Map<String, List<Action>> actionTable;
    int test_case = 0;

    public ModelReplay(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
        actionTable = new HashMap<>();
    }

    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        Device d = devices.get(serial);
        if (prev_decision.action == null){
            if (d.getRoute_list().size() == 0){
                return new Decision(Decision.CODE.STOP);
            }
            log(serial, "To " + d.getRoute_list().get(0));
            return replay(newTree, d.serial, d.getRoute_list().get(0));
        }

        //FragmentNode currentNode = graphAdjustor.locate(newTree);
        String activity = prev_decision.action.target_activity;
        int hash = prev_decision.action.target_hash;
        FragmentNode expectNode = graphAdjustor.appGraph.getFragment(activity, hash);
        List<Action> paths = actionTable.get(serial);
        //log("current: " + newTree.getTreeStructureHash() + " expect: " + expectNode.getSignature());
        if (paths.size() < 1){
            if (newTree.getActivityName().equals(expectNode.getActivity()) &&
                    expectNode.calc_similarity(newTree.getClickable_list()) > 0.65)
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
        List<Action> paths = buildPath(tree, serial, target);
        if (paths != null){
            log("path size: " + paths.size());
            actionTable.put(serial, paths);
            Action action = actionTable.get(serial).remove(paths.size()-1);
            return new Decision(Decision.CODE.CONTINUE, action);
        }else {
            return new Decision(Decision.CODE.STOP);
        }
    }
//    public void run(){
//        ConnectUtil.current_pkg = ConnectUtil.launch_pkg;
//        if (graphManager == null || graphManager.appGraph == null) return;
//        if (!restart()) return;
//
//        if (!verify)
//            replay(false);
//        else
//            replay(true);
//    }



//    public void replay(Boolean is_verify){
//        List<String> visited = new ArrayList<>();
//        List<String> unvisited = new ArrayList<>();
//        List<FragmentNode> nodes;
//        if (is_verify)
//            nodes = graphManager.getAllNodes();
//        else
//            nodes = graphManager.getRouteNodes(route_list);
//
//        for(FragmentNode node : nodes){
//            FragmentNode start = getStartNode();
//            if (start == null) return;
//
//            if (start.getStructure_hash() == node.getStructure_hash()){
//                visited.add(node.getSignature());
//                continue;
//            }
//
//            log("To: " + node.getSignature());
//            if (visited.contains(node.getSignature()))
//                continue;
//            List<List<Tuple2<FragmentNode, Action>>> paths = graphManager.findOnePath(start, node);
//            for(List<Tuple2<FragmentNode, Action>> path: paths){
//                List<String> visited_along_path = execute_path(path);
//                for(String n : visited_along_path){
//                    if (!visited.contains(n))
//                        visited.add(n);
//                }
//
//                if (visited_along_path.contains(node.getSignature()))
//                    break;
//            }
//
//            if(!visited.contains(node.getSignature()))
//                unvisited.add(node.getSignature());
//
//            if (is_verify)
//                if (!restart()) return;
//
//            //graphManager.resetColor();
//        }
//
//        display(visited, unvisited);
//    }
//
//    List<String> execute_path(List<Tuple2<FragmentNode, Action>> path){
//        List<String> visited_along_path = new ArrayList<>();
//        log("path size: " + path.size());
//        ClientUtil.record("start", ++test_case);
//
//        long t1 = System.currentTimeMillis();
//
//        for(int i = path.size()-1; i >=0; i--){
//            Action action = path.get(i).getSecond();
//            FragmentNode expect_node = path.get(i).getFirst();
//            String sub_path = currentTree.matchPath(action.path);
//            ClientUtil.checkStatus(ClientUtil.execute_action(Action.action_list.CLICK, sub_path, true));
//            if (action.getAction() == Action.action_list.ENTERTEXT)
//                ClientUtil.checkStatus(ClientUtil.execute_action(action.getAction(), action.getContent()));
//
//            currentTree = ClientUtil.getCurrentTree();
//            if (currentTree == null) return visited_along_path;
//
//            if (!currentTree.getActivityName().equals("null")) {
//                if (!currentTree.getActivityName().equals(expect_node.getActivity())) {
//                    log("Activity not match, expect: " + expect_node.getActivity() +
//                            " jump to: " + currentTree.getActivityName());
//                    long t2 = System.currentTimeMillis();
//                    double s = (t2 - t1) / 1000.0;
//                    log("time: " + s);
//                    return visited_along_path;
//                }
//            }
//
//            if (expect_node.getStructure_hash() != currentTree.getTreeStructureHash()){
//                double similarity = currentTree.calc_similarity(expect_node.get_Clickable_list());
//                if (similarity < CommonUtil.SIMILARITY){
//                    log("jump to unqualify node : " + currentTree.getTreeStructureHash() +
//                    " expect: " + expect_node.getSignature());
//                    long t2 = System.currentTimeMillis();
//                    double s = (t2 - t1) / 1000.0;
//                    log("time: " + s);
//                    return visited_along_path;
//                }
//            }
//            visited_along_path.add(expect_node.getSignature());
//
////            Boolean pass = false;
////            int j = i;
////            for (; j >=0; j--){
////                expect_node = path.get(j).getFirst();
////                if (expect_node.getStructure_hash() != currentTree.getTreeStructureHash()){
////                    double similarity = currentTree.calc_similarity(expect_node.get_Clickable_list());
////                    if (similarity > CommonUtil.SIMILARITY){
////                        visited_along_path.add(expect_node.getSignature());
////                        pass = true;
////                        break;
////                    }
////                }
////            }
////
////            if (!pass){
////                log("jump to unqualify node: " + currentTree.getTreeStructureHash());
////                long t2 = System.currentTimeMillis();
////                double s = (t2 - t1) / 1000.0;
////                log("time: " + s);
////                return visited_along_path;
////            }
////
////            i = j;
//        }
//
//        ClientUtil.record("stop", 0);
//        long t2 = System.currentTimeMillis();
//        double s = (t2 - t1) / 1000.0;
//        log("time: " + s);
//
//        return visited_along_path;
//    }
//
//    void display(List<String> visited, List<String> unvisited){
//        int tot = visited.size() + unvisited.size();
//        float coverage = (float) visited.size() / tot;
//        log("coverage: " + visited.size() + "/" + tot);
//        log("visited: ");
//        for(String visit : visited)
//            log("*" + visit.replace(ConnectUtil.launch_pkg, ""));
//
//        log("unvisited: ");
//        for(String unvisit : unvisited)
//            log("-" + unvisit.replace(ConnectUtil.launch_pkg, ""));
//    }
//
//    FragmentNode getStartNode(){
//        currentTree = ClientUtil.getCurrentTree();
//        log("find entry: " + currentTree.getActivityName() + "_" + currentTree.getTreeStructureHash() + " size: " + currentTree.getClickable_list().size());
//        FragmentNode start = graphManager.getFragmentInGraph(currentTree);
//        int limits = 0;
//        while (start == null && limits < 5) {
//            CommonUtil.sleep(2000);
//            ClientUtil.refreshUI();
//            currentTree = ClientUtil.getCurrentTree();
//            start = graphManager.getFragmentInGraph(currentTree);
//            limits += 1;
//        }
//
//        if (start == null) {
//            log("fail to find entry");
//            return null;
//        }
//        log("current node: " + start.getSignature());
//        return start;
//    }
}
