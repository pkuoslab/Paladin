package com.sei.modules;

import com.sei.agent.Device;
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
import java.util.List;
import java.util.concurrent.CompletionException;

import static com.sei.util.CommonUtil.log;

public class ModelReplay implements Strategy{
    public volatile Boolean verify = false;
    FragmentNode start;
    public GraphAdjustor graphAdjustor;
    List<Device> devices;
    List<String> route_list;
    int test_case = 0;

//    public ModelReplay(GraphManager graphManager){
//        super();
//        this.graphManager = graphManager;
//        verify = true;
//    }

//    public ModelReplay(GraphManager graphManager, List<String> route_list){
//        super();
//        this.graphManager = graphManager;
//        this.route_list = route_list;
//        verify = false;
//    }
    public ModelReplay(GraphAdjustor graphAdjustor, List<Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
    }

    public Decision make(int id, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        return null;
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
