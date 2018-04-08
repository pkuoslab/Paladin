package com.sei.agent;

import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.Collection.Stack.FragmentStack;
import com.sei.bean.Collection.Stack.RuntimeFragmentNode;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;
import com.sei.server.component.Scheduler;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;

import java.util.ArrayList;
import java.util.List;

public class Device extends Thread{
    public String ip;
    public int port;
    public String serial;
    public String password;
    public String current_pkg;
    public FragmentStack fragmentStack;
    public int id;
    ViewTree currentTree;
    Scheduler scheduler;
    GraphAdjustor graphAdjustor;
    Boolean Exit;
    Boolean LOGIN_SUCCESS;

    public interface UI{
        int NEW = 0;
        int PIDCHANGE = -1;
        int OUT = -3;
        int SAME = 2;
    }

    public Device(String ip, int port, String serial, String pkg, String password){
        this.ip = ip;
        this.serial = serial;
        this.current_pkg = pkg;
        this.port = port;
        this.password = password;
        LOGIN_SUCCESS = true;
        Exit = false;
    }

    public void bind(int id, Scheduler scheduler, GraphAdjustor graphAdjustor){
        this.id = id;
        this.scheduler = scheduler;
        this.graphAdjustor = graphAdjustor;
    }

    public void bind(int id, Scheduler scheduler, GraphAdjustor graphAdjustor, FragmentStack fragmentStack){
        this.bind(id, scheduler, graphAdjustor);
        this.fragmentStack = fragmentStack;
    }

    public void run(){
        ViewTree newTree;
        int response;
        ClientUtil.startApp(this, ConnectUtil.launch_pkg);
        if(ClientUtil.checkPermission(this)) restart();
        currentTree = getCurrentTree();
        if (currentTree == null) restart();

        if (checkLogin(currentTree)) currentTree = getCurrentTree();

        if (fragmentStack == null) {
            fragmentStack = new FragmentStack();
            RuntimeFragmentNode rfn = new RuntimeFragmentNode(currentTree);
            fragmentStack.add(rfn);
        }else
            recover_stack();

        Action action = null;
        Decision decision = new Decision(Decision.CODE.CONTINUE, action);
        decision = scheduler.update(id, currentTree, currentTree, decision, UI.NEW);
        response = execute_decision(decision);
        do{
            if (response != UI.SAME) {
                newTree = getCurrentTree();
                if (newTree == null) response = UI.OUT;
            }else
                newTree = currentTree;

            decision = scheduler.update(id, currentTree, newTree, decision, response);
            currentTree = newTree;
            response = execute_decision(decision);
        }while(Exit == false);

    }

    public ViewTree getCurrentTree(){
        return ClientUtil.getCurrentTree(this);
    }


    public int execute_decision(Decision decision){
        int response = UI.OUT;
        if (decision.code == Decision.CODE.CONTINUE) {
            Action action = decision.action;
            response = ClientUtil.execute_action(this, Action.action_list.CLICK, action.getPath());
            //如果是输入框，先点击获得焦点再输入
            if (action.getAction() == Action.action_list.ENTERTEXT) {
                if (response == UI.SAME)
                    response = ClientUtil.execute_action(this, Action.action_list.ENTERTEXT, "test");
                else
                    action.setAction(Action.action_list.CLICK);
            }
            if (response != UI.SAME && response != UI.OUT) {
                if (update_stack(action) == UI.OUT) response = UI.OUT;
            }
        }else if (decision.code == Decision.CODE.SEQ){
            return execute_actions(decision);
        }else if (decision.code == Decision.CODE.STOP) {
            log("stop");
            Exit = true;
        }else if (decision.code == Decision.CODE.RESTART || decision.code == Decision.CODE.GO){
            if (decision.code == Decision.CODE.RESTART) {
                log("need restart stack size: " + fragmentStack.getSize());
                restart();
            }
            response = recover_stack();
            if (response != UI.SAME && response != UI.OUT){
                if(update_stack(null) == UI.OUT) response = UI.OUT;
            }
        }

        return response;
    }

    public int execute_actions(Decision decision){
        int response = UI.OUT;
        List<Action> actions = decision.actions;
        List<RuntimeFragmentNode> rfn_cache = new ArrayList<>();
        for(Action action: actions){
            ClientUtil.execute_action(this, action.getAction(), action.getPath());
            ViewTree tree = getCurrentTree();
            if (tree == null) return UI.OUT;
            RuntimeFragmentNode rfn = new RuntimeFragmentNode(tree);

            if(graphAdjustor.match(tree, action.target_activity, action.target_hash)){
                if (rfn_cache.size() == 0)
                    fragmentStack.top().setAction(action);
                else
                    rfn_cache.get(rfn_cache.size()-1).setAction(action);
                rfn_cache.add(rfn);
            }else{
                //恢复栈出现错误，观察节点是否还能点击
                FragmentNode frg = graphAdjustor.searchFragment(tree);
                if (frg == null || !frg.isTraverse_over()){
                    Action action1 = new Action(action.getPath(), action.getAction());
                    action1.setTarget(tree.getActivityName(), tree.getTreeStructureHash());
                    rfn.setAction(action1);
                    break;
                }else {
                    decision.position = actions.indexOf(action) + 1;
                    log("not match in position " + decision.position);
                    return UI.SAME;
                }
            }
        }
        //log("after schedule stack size: " + fragmentStack.getSize());
        for(RuntimeFragmentNode rfn : rfn_cache)
            fragmentStack.add(rfn);
        return UI.NEW;
    }

    public int update_stack(Action action){
        ViewTree tree = getCurrentTree();
        if (tree == null) return UI.OUT;
        if (checkLogin(tree)) tree = getCurrentTree();

        FragmentNode frg = graphAdjustor.searchFragment(tree);
        int p = fragmentStack.getPosition(tree);
        
        if ((frg == null || !frg.isTraverse_over()) && p == -1){
            RuntimeFragmentNode rfn = new RuntimeFragmentNode(tree);
            if (action != null)
                fragmentStack.top().setAction(action);
            fragmentStack.add(rfn);
        }
        //无意义
        return 1;
    }

    public int recover_stack(){
        currentTree = getCurrentTree();
        if (currentTree == null)
            return UI.OUT;

        int p = fragmentStack.getPosition(currentTree);
        if (p == fragmentStack.getSize()-1) return UI.SAME;

        if (p != -1){
            String s = currentTree.getActivityName() + "_" + currentTree.getTreeStructureHash();
            log("fragment " +  s + " in stack, recover " + p + "/" + fragmentStack.getSize());
            return fragmentStack.recover(this, p);
        }

        p = go_back(2);
        if (p != -1 && p != UI.OUT)
            return fragmentStack.recover(this, p);
        else
            return UI.OUT;
    }

    int go_back(int limit){
        int t = 0;
        do{
            int response = ClientUtil.execute_action(this, Action.action_list.BACK);
            if (response == UI.OUT) return UI.OUT;
            currentTree = getCurrentTree();
            if (currentTree == null)
                return UI.OUT;
            int p = fragmentStack.getPosition(currentTree);
            if (p != -1){
                log("Back to position " + p);
                return p;
            }else{
                log("Back to unknown position, continue back");
            }
            t += 1;
        }while(t < limit);
        return UI.OUT;
    }

    void restart(){
        int limit = 3;
        int t = 0;

        ClientUtil.stopApp(this, ConnectUtil.launch_pkg);
        ClientUtil.startApp(this, ConnectUtil.launch_pkg);
        currentTree = getCurrentTree();

        while (currentTree == null && t < limit){
            CommonUtil.start_paladin(this);
            currentTree = getCurrentTree();
            t += 1;
        }

        if (currentTree == null){
            log("restart too many times");
            Exit = true;
        }

        int p = -1;
        t = 0;
        do {
            if (checkLogin(currentTree)) currentTree = getCurrentTree();
            if (currentTree == null){
                log("restart can not get tree");
                Exit = true;
            }

            if (fragmentStack != null) p = fragmentStack.getPosition(currentTree);
            log("refresh");
            CommonUtil.sleep(1000);
            currentTree = getCurrentTree();
            t += 1;
        }while(p == -1 && t < limit);

        if (p == -1){
            log("restart can not match stack entry, clear stack");
            if (fragmentStack == null)
                fragmentStack = new FragmentStack();
            else
                fragmentStack.clear();
            RuntimeFragmentNode rfn = new RuntimeFragmentNode(currentTree);
            fragmentStack.add(rfn);
        }
    }

    public Boolean checkLogin(ViewTree tree){
        Boolean success = false;
        if (tree.getActivityName().contains("LoginPasswordUI") && LOGIN_SUCCESS) {
            success = ClientUtil.login(this, tree);
            LOGIN_SUCCESS = success;
        }
        return success;
    }

    public void log(String info){
        CommonUtil.log("device #" + this.id + ": " + info);
    }
}
