package com.sei.modules;

import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.Collection.Stack.GraphManagerWithStack;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ConnectUtil;
import com.sei.util.ClientUtil.Status;
import com.sei.bean.Collection.Stack.GraphManagerWithStack.STACK_STATUS;
import com.sei.util.ViewUtil;

import static com.sei.util.CommonUtil.log;

public class DepthFirstTraversal extends Strategy {
    ViewTree new_tree;
    GraphManager graphManager;
    GraphManagerWithStack manager;
    Boolean RECOVER = false;

    public DepthFirstTraversal(GraphManager graphManager){
        super();
        this.graphManager = graphManager;
    }

    public DepthFirstTraversal(){
        super();
    }

    public void run(){
        try {
            if (!initiate()) return;
            if (graphManager.appGraph == null) graphManager.graphManagerFactor(currentTree);
            manager = new GraphManagerWithStack(currentTree, graphManager);
            RUNNING = true;

            while (!manager.isStackEmpty()){
                if(check_exit(EXIT)) return;
                if(!recover(RECOVER)) return;
                RECOVER = false;
                //获取栈顶的Runtime fragment
                manager.getStackTop();

                currentTree = ClientUtil.getCurrentTree();

                while(!manager.hasCurrentFragmentOver()){
                    if(check_exit(EXIT)) return;
                    // select a xpath randomly
                    String xpath = manager.getOneXpath();
                    if (manager.xpathIsUnclicked(xpath))
                        continue;
                    while(!manager.hasXpathOver(currentTree, xpath)){
                        if(check_exit(EXIT)) return;
                        // select a path randomly
                        int ser = manager.getXpathItem(currentTree, xpath);
                        Response response = execute(xpath + "#" + ser);

                        if (response.code == Status.NEW || response.code == Status.PIDCHANGE){
                            new_tree = ClientUtil.getCurrentTree();
                            Action action = response.action;
                            action.setTarget(new_tree.getActivityName(), new_tree.getTreeStructureHash());
                            int stack_status = manager.update(action, currentTree, new_tree);
                            currentTree = ClientUtil.getCurrentTree();
                            if (stack_status == Status.OUT) {
                                log("Stack recover error, need restart");
                                RECOVER = true;
                                break;
                            }else if (stack_status == STACK_STATUS.NEW)
                                break;
                            else if (stack_status == STACK_STATUS.STACK)
                                break;

                        }else if(response.code == Status.OUT){
                            log("reach outside of the app, need recover size: " + manager.getStackSize());
                            RECOVER = true;
                            break;
                        }
                    }

                    if (RECOVER) break;
                    if (manager.hasCurrentFragmentChanged()) {
                        manager.setCurrentFragmentChanged(false);
                        continue;
                    }else {
                        //log("current xpath over, next");
                        manager.setCurrentXpathOver(xpath);
                    }
                    if (manager.hasFragmentXpathOver() &&
                            !manager.hasFragmentMenuClicked()){
                        log("click menu");
                        manager.setFragmentMenuClicked();
                        int code =ClientUtil.checkStatus(ClientUtil.execute_action(Action.action_list.MENU));
                        if (code == Status.NEW || code == Status.PIDCHANGE){
                            new_tree = ClientUtil.getCurrentTree();
                            manager.update(new Action("", Action.action_list.MENU), currentTree, new_tree);
                            currentTree = ClientUtil.getCurrentTree();
                        }else if(code == Status.OUT) {
                            RECOVER = true;
                            break;
                        }
                    }
                }

                if (RECOVER){
                    continue;
                }
                manager.setCurrentFragmentOver();
                int stack_status = manager.handleFragmentOver(true);
                manager.pushBackCurrentFragment();
                if (stack_status == Status.OUT) {
                    manager.popCurrentFragmentNode();
                    log("Can't go back or outside of the app, need recover");
                    RECOVER = true;
                    continue;
                }
                currentTree = ClientUtil.getCurrentTree();
            }
        }catch (Exception e){
            RUNNING = false;
            e.printStackTrace();
            return;
        }
    }

    public Boolean recover(Boolean need_recover){
        if (!need_recover) return true;
        manager.pushBackCurrentFragment();
        ConnectUtil.force_stop(ConnectUtil.launch_pkg);
        ClientUtil.startApp(ConnectUtil.launch_pkg);
        if (!initiate()){
            log("restart app failure");
            return false;
        }
        manager.handleRecover();
        return true;
    }

    public Boolean check_exit(Boolean need_exit){
        if(!need_exit){
            manager.save();
            return false;
        }
        log("stop");
        return true;
    }

    Response execute(String path){
        String status = ClientUtil.execute_action(Action.action_list.CLICK, path);
        Action action;
        int response = ClientUtil.checkStatus(status);
        if (response == Status.SAME){
            ViewNode vn= ViewUtil.getViewByPath(currentTree.root, path);
            if (vn != null && vn.getViewTag().contains("EditText")){
                response = ClientUtil.checkStatus(ClientUtil.execute_action(Action.action_list.ENTERTEXT, "Beijing"));
                action = new Action(path, Action.action_list.ENTERTEXT, "Beijing");
            }else
                action = null;
        }else
            action = new Action(path, Action.action_list.CLICK);

        return new Response(response, action);
    }

    class Response{
        int code;
        Action action;
        Response(int code, Action action){
            this.code = code;
            this.action = action;
        }
    }
}
