package com.sei.modules;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.ActivityNode;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.Collection.Tuple2;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;
import com.sei.util.CommonUtil;
import com.sei.util.client.ClientAdaptor;
import com.sei.util.client.ClientAutomator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sei.util.CommonUtil.log;

public class NewSpiderStrategy implements Strategy{
    private FragmentNode start;
    public GraphAdjustor graphAdjustor;
    Map<String, Device> devices;
    private Map<String, List<Action>> actionTable;
    private Map<String, Integer> replayLimit;
    private List<Tuple2<FragmentNode,Action>> mutationPages;
    private static Boolean first_start = true;
    private static Boolean mutation = false;            //mutation为true时开始路径变换
    private static int originalMutationTreeHash = -1;  // 用于恢复mutation page
    private static int mutationTreeHash = -1;
    private static String mutationTreeActivity = null;
    private static String mutationPath = null;
    private static int scroll_count = 0;           //滑动次数
    private static int recover_scroll_count = 0;   //用于恢复mutation page时已经有的滑动次数。
    private static int MAX_SCROLL_COUNT = 10;
    private static Boolean change_mutation_page = false;
    private static Boolean recover_mutation_page = false;   //当其为true时，需要恢复mutation page
    private static Boolean start_recover = false;           //表示开始对mutation page进行恢复
    private static int times_out = 0;                       //不在mutation page的次数。
    private List<String> deepLinks;                         //防止重复的deep link
    private List<String> finished_mutation_page;            //防止重复的mutation page


    public NewSpiderStrategy(GraphAdjustor graphAdjustor, Map<String, Device> devices){
        this.graphAdjustor = graphAdjustor;
        this.devices = devices;
        actionTable = new HashMap<>();
        replayLimit = new HashMap<>();
        deepLinks = new ArrayList<>();       //记录已经爬到的页面的deep link
        mutationPages = new ArrayList<>();
        finished_mutation_page = new ArrayList<>();
    }

    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response){
        //CommonUtil.log("newTree:" + newTree.getActivityName() + "_" + newTree.getTreeStructureHash());
        Device d = devices.get(serial);

        if(newTree == null)
            return new Decision(Decision.CODE.RESTART);

        if(newTree.getTreeStructureHash() == 0)
            return new Decision(Decision.CODE.RESTART);

        if(first_start) {
            CommonUtil.log("try to find all available mutation page");
            first_start = false;
            assert d.targetActivity != null;
            log("target activity:" + d.targetActivity);
            log("tree:" + newTree.getActivityName() + "_" + newTree.getTreeStructureHash());
            graphAdjustor.resetColor();
            FragmentNode start = graphAdjustor.locateForSpider(newTree);
            if(start == null) {
                CommonUtil.log("locate failed! try the first one!");
                //选择activity中的第一个fragment尝试。
                start = graphAdjustor.appGraph.getAct(newTree.activityName).getFragments().get(0);
            }
            CommonUtil.log("start:" + start.getSignature());
            mutationPages = graphAdjustor.getPreviousNodes(start, d.targetActivity);
            graphAdjustor.resetColor();
            for(int i = 0; i < mutationPages.size(); i++) {
                CommonUtil.log("No." + i + ":" + mutationPages.get(i).getFirst().getSignature());
                CommonUtil.log("path:" + mutationPages.get(i).getSecond().path);
            }
            if(mutationPages.size() == 0) {
                CommonUtil.log("can't find mutation pages");
                return new Decision(Decision.CODE.STOP);
            }
            graphAdjustor.clearPathIndex();
            recover_mutation_page = true;
            originalMutationTreeHash = -1;
        }

        if (response == Device.UI.OUT) {
            //firstStart = true;
            return new Decision(Decision.CODE.RESTART);
        }

        if (response == Device.UI.OUTANDIN && originalMutationTreeHash != -1) {
            CommonUtil.log("try to get back to the mutation page");
            CommonUtil.log("mutation page:" + mutationTreeActivity + "_" + mutationTreeHash);
            recover_mutation_page = true;
            start_recover = true;
            recover_scroll_count = 0;
        }

        if(recover_mutation_page) {
            CommonUtil.log("try to recover mutation page!");
            String target = null;
            if(originalMutationTreeHash == -1) {
                CommonUtil.log("build target");
                if(mutationPages.size() == 0 && scroll_count == 0) {
                    CommonUtil.log("1 no mutation pages left!");
                    return new Decision(Decision.CODE.STOP);
                }
                FragmentNode targetMutationPage;
                Action mutationAction;
                while(!mutationPages.isEmpty()){
                    targetMutationPage = mutationPages.get(0).getFirst();
                    mutationAction = mutationPages.remove(0).getSecond();
                    target = targetMutationPage.getSignature();
                    if(finished_mutation_page.contains(target)) {
                        CommonUtil.log("duplicated mutation page, ignore!");
                        if(mutationPages.isEmpty() && scroll_count == 0) {
                            CommonUtil.log("2 no mutation pages left!");
                            return new Decision(Decision.CODE.STOP);
                        }
                    } else {
                        finished_mutation_page.add(target);
                        originalMutationTreeHash = targetMutationPage.getStructure_hash();
                        mutationTreeActivity = targetMutationPage.getActivity();
                        mutationPath = mutationAction.path;
                        scroll_count = 0;
                        start_recover = true;
                        break;
                    }
                }
            }
            if (start_recover) {
                start_recover = false;
                assert originalMutationTreeHash != -1;
                if(target == null) target = mutationTreeActivity + "_" + originalMutationTreeHash;
                replayLimit.put(serial, 0);
                //CommonUtil.log("target:" + target);
                Decision decision = new Decision(Decision.CODE.STOP);
                if(recover_scroll_count == 0)
                    decision = replay(newTree, d.serial, target);
                if (recover_scroll_count > 0 || decision.code == Decision.CODE.STOP){
                    CommonUtil.log("maybe we have get to where we want");
                    CommonUtil.log("newTree:" + newTree.getActivityName()+"_"+newTree.getTreeStructureHash());
                    //可能是已经到达了target mutation page
                    if(newTree.getActivityName().equals(mutationTreeActivity)) {
                        //距离成功恢复，还差几次滑动
                        if(recover_scroll_count < scroll_count) {
                            recover_scroll_count++;
                            start_recover = true;           //!!!!重要
                            Action action = new Action();
                            action.setAction(Action.action_list.SCROLLDOWN);
                            return new Decision(Decision.CODE.CONTINUE, action);
                        }
                        recover_mutation_page = false;
                        recover_scroll_count = 0;
                        mutation = true;
                        mutationTreeHash = newTree.getTreeStructureHash();
                        CommonUtil.log("recover mutation page success");
                        return new Decision(Decision.CODE.DONOTHING);
                    }
                }
                return decision;
            }

            //FragmentNode currentNode = graphAdjustor.locate(newTree);
            String activity = prev_decision.action.target_activity;
            int hash = prev_decision.action.target_hash;
            FragmentNode expectNode = graphAdjustor.appGraph.getFragment(activity, hash);
            List<Action> paths = actionTable.get(serial);
            target = activity+"_"+hash;
            //log("current: " + newTree.getTreeStructureHash() + " expect: " + expectNode.getSignature());
            if (paths.size() < 1){
                if (recover_scroll_count == 0 && newTree.getActivityName().equals(expectNode.getActivity()) &&
                        expectNode.calc_similarity(newTree.getClickable_list()) > 0.4)
                    d.visits.add(expectNode.getSignature());
                //距离成功恢复，还差几次滑动
                if(recover_scroll_count < scroll_count) {
                    recover_scroll_count++;
                    Action action = new Action();
                    action.setAction(Action.action_list.SCROLLDOWN);
                    return new Decision(Decision.CODE.CONTINUE, action);
                }
                recover_mutation_page = false;
                recover_scroll_count = 0;
                mutation = true;
                mutationTreeHash = newTree.getTreeStructureHash();
                CommonUtil.log("recover mutation page success");
                return new Decision(Decision.CODE.DONOTHING);
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
            } else if (currentTree.getTreeStructureHash() != newTree.getTreeStructureHash()){
                CommonUtil.log("get here!!!!!!");
                return replay(newTree, d.serial, target);
            } else{
                // for debug
                log("xpath: " + xpath);
                for(String s: newTree.getClickable_list()){
                    log(s);
                }
            }
            CommonUtil.log("get here2!!!!!!!");
            return new Decision(Decision.CODE.STOP);
        }

        if(change_mutation_page) {
            change_mutation_page = false;
            mutationTreeHash = newTree.getTreeStructureHash();
        }

        if(mutation) {
            //路径变异
            if(newTree.getTreeStructureHash() == mutationTreeHash) {
                times_out = 0;
                //CommonUtil.log("in mutation page");
                Action newAction = null;
                newAction = graphAdjustor.getSimilarTextAction(newTree, mutationPath);
                if(newAction != null) {
                    return new Decision(Decision.CODE.CONTINUE, newAction);
                } else {
                    //尝试在当前页面做一些变化，目前只支持划动
                    if(scroll_count < MAX_SCROLL_COUNT) {
                        CommonUtil.log("try to do some changes on this page");
                        scroll_count++;
                        newAction = new Action();
                        newAction.setAction(Action.action_list.SCROLLDOWN);
                        change_mutation_page = true;
                        FragmentNode fn = graphAdjustor.appGraph.getFragment(mutationTreeActivity, mutationTreeHash);
                        if(fn != null) {
                            fn.text_path_index.clear();
                        } else CommonUtil.log("fn is null");
                        return new Decision(Decision.CODE.CONTINUE, newAction);
                    } else {
                        CommonUtil.log("this time's path mutation is over. try to find another path");
                        scroll_count = 0;
                        originalMutationTreeHash = -1;
                        mutationTreeHash = -1;
                        mutationTreeActivity = null;
                        recover_mutation_page = true;
                        return new Decision(Decision.CODE.RESTART_F);
                    }
                }
            } else {
                //检查是否是目标页面，记录并返回
                if (newTree.getActivityName().equals(d.targetActivity)) {
                    //CommonUtil.log("find target Activity!!!");
                    //比较content hash。
                    String deepLink = CommonUtil.getDeeplink();
                    CommonUtil.log("deep link:" + deepLink);
                    if(!deepLink.contains("not found") && !deepLink.contains("dl://")) {
                        CommonUtil.log("restart deep link server!");
                        ClientAdaptor.startApp(d, "com.example.sei.deeplinnkserver");
                        return new Decision(Decision.CODE.RESTART);
                    }
                    if (!deepLinks.contains(deepLink)) {
                        CommonUtil.log("find new content!");
                        CommonUtil.sleep(1000);
                        //记录并截图
                        newTree.setDeeplink(deepLink);
                        CommonUtil.SCREENSHOT = true;
                        CommonUtil.getSnapshot(newTree, d);
                        CommonUtil.SCREENSHOT = false;
                        deepLinks.add(deepLink);
                    } else {
                        //CommonUtil.log("old content!");
                    }
                    times_out = 0;
                }
                times_out++;
                if(times_out > 3){
                    times_out = 0;
                    return new Decision((Decision.CODE.RESTART_F));
                }
                Action action = new Action();
                action.setAction(Action.action_list.BACK);
                return new Decision(Decision.CODE.CONTINUE,action);
            }
        }
        else {
            CommonUtil.log("Wrong!");
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

    private Decision replay(ViewTree tree, String serial, String target){
        if (replayLimit.get(serial) > 4){
            CommonUtil.log("6");
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
            } else{
                CommonUtil.log("7");
                return new Decision(Decision.CODE.STOP);
            }
        } else {
            CommonUtil.log("8");
            return new Decision(Decision.CODE.STOP);
        }
    }

    private List<Action> buildPath(ViewTree tree, String serial, String target){
        //FragmentNode start = graphAdjustor.locate(tree);
        Device d = devices.get(serial);

        FragmentNode start = graphAdjustor.getFragmentInGraph(tree);
        FragmentNode end = graphAdjustor.appGraph.getFragment(target);
        List<Action> actions = null;
        if (end == null) return null;

        if (start != null) {
            //log("start: " + start.getActivity() + "_" + start.getStructure_hash());
            if (start.getStructure_hash() == end.getStructure_hash()) {
                return null;
            }
            actions = graphAdjustor.BFS(start, end);
            graphAdjustor.resetColor();
            if (actions != null) {
                log("search path success!");
                d.visits.add(start.getSignature());
                return actions;
            }else{
                log("search fail");
            }
        }

        // 如果找不到起始点，就在该activity内的节点逐个试一遍
        ActivityNode an = graphAdjustor.appGraph.getAct(tree.getActivityName());
        if (an == null) return null;
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
}
