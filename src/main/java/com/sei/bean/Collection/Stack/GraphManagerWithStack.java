package com.sei.bean.Collection.Stack;

import com.sei.bean.Collection.Graph.ActivityNode;
import com.sei.bean.Collection.Graph.AppGraph;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.Collection.UiTransition;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.ViewUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.ArrayList;
import java.util.List;

import static com.sei.util.CommonUtil.log;

public class GraphManagerWithStack extends UiTransition {
    GraphManager graphManager;
    FragmentStack fragmentStack;
    AppGraph appGraph;
    RuntimeFragmentNode currentFragmentNode;
    Boolean currentFragmentChanged = false;

    public interface STACK_STATUS{
        int EMPTY = 0;
        int STACK = -1;
        int NEW = -2;
        int OUT = -3;
        int RECOVER = -4;
        int CLEAN = -5;
    }

    public GraphManagerWithStack(ViewTree currentTree, GraphManager global_graph){
        graphManager = global_graph;
        appGraph = graphManager.appGraph;

        fragmentStack = new FragmentStack();
        currentFragmentNode = new RuntimeFragmentNode(currentTree);
        fragmentStack.add(currentFragmentNode);

        registerAllHandlers();
    }

    @Override
    public int update(Action action, ViewTree currentTree, ViewTree new_tree){
        int status = graphManager.queryGraph(currentTree, new_tree);
        Handler handler = handler_table.get(status);
        return handler.adjust(action, currentTree, new_tree);
    }

    @Override
    public void registerAllHandlers(){
        registerHandler(UI.NEW_ACT, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                graphManager.getFragmentNode().addIntrapath(action);
                graphManager.setActivityNode(new ActivityNode(new_tree.getActivityName()));
                graphManager.getActivityNode().setSer_intent(ClientUtil.getSerIntent());
                appGraph.appendActivity(graphManager.getActivityNode());
                graphManager.setFragmentNode(new FragmentNode(new_tree));
                graphManager.getActivityNode().appendFragment(graphManager.getFragmentNode());

                //deal with stack
                handleNewFragment(action, new_tree);
                return STACK_STATUS.NEW;
            }
        });

        registerHandler(UI.OLD_ACT_NEW_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                graphManager.getFragmentNode().addIntrapath(action);
                graphManager.setActivityNode(appGraph.getAct(new_tree.getActivityName()));
                graphManager.setFragmentNode(new FragmentNode(new_tree));
                graphManager.getActivityNode().appendFragment(graphManager.getFragmentNode());
                handleNewFragment(action, new_tree);
                return STACK_STATUS.NEW;
            }
        });

        registerHandler(UI.NEW_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                graphManager.getFragmentNode().addIntrapath(action);
                graphManager.setFragmentNode(new FragmentNode(new_tree));
                graphManager.getActivityNode().appendFragment(graphManager.getFragmentNode());
                handleNewFragment(action, new_tree);
                return STACK_STATUS.NEW;
            }
        });

        registerHandler(UI.OLD_ACT_OLD_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                graphManager.getFragmentNode().addIntrapath(action);
                FragmentNode frg = appGraph.getAct(new_tree.getActivityName()).find_Fragment(new_tree);
                int position = fragmentStack.getPosition(new_tree);
                if (position != -1){
                    return handleFragmentInStack(position);
                }else if(!frg.isTraverse_over())
                    return handleFragmentNotOver(action, new_tree);
                else
                    return handleFragmentOver(false);
            }
        });

        registerHandler(UI.OLD_FRG, new Handler() {
            @Override
            public int adjust(Action action, ViewTree currentTree, ViewTree new_tree) {
                graphManager.getFragmentNode().addIntrapath(action);
                FragmentNode frg = graphManager.getActivityNode().find_Fragment(new_tree);
                int position = fragmentStack.getPosition(new_tree);
                if (position != -1){
                    return handleFragmentInStack(position);
                }else if(!frg.isTraverse_over())
                    return handleFragmentNotOver(action, new_tree);
                else
                    return handleFragmentOver(false);
            }
        });
    }

    int handleFragmentInStack(int position){
        fragmentStack.add(currentFragmentNode);
        log("fragment in stack, need recover: " + position + "/" + fragmentStack.getSize());
        int recover_status = fragmentStack.recover(position);
        if (recover_status == STACK_STATUS.RECOVER){
            log("recover successfully");
            fragmentStack.pop();
        }else if(recover_status == STACK_STATUS.STACK) {
            log("recover failed, continue crawl");
            currentFragmentNode = fragmentStack.pop();
            //调整图的指针，与栈节点同步
            graphManager.setActivityNode(appGraph.getAct(currentFragmentNode.getActivity()));
            int hash = currentFragmentNode.getStructure_hash();
            List<String> click_list = currentFragmentNode.get_Clickable_list();
            graphManager.setFragmentNode(graphManager.getActivityNode().find_Fragment(hash, click_list));
            log("graph adjust finished");

            currentFragmentChanged = true;
        }

        return recover_status;
    }

    void handleNewFragment(Action action, ViewTree new_tree){
        currentFragmentNode.setAction(action);
        fragmentStack.add(currentFragmentNode);
        currentFragmentNode = new RuntimeFragmentNode(new_tree);
        currentFragmentChanged = true;
    }

    int handleFragmentNotOver(Action action, ViewTree new_tree){
        log("old fragment not traversed over");
        currentFragmentNode.setAction(action);
        fragmentStack.add(currentFragmentNode);
        currentFragmentNode = new RuntimeFragmentNode(new_tree);
        log("stack size: " + fragmentStack.getSize());
        graphManager.setActivityNode( appGraph.getAct(new_tree.getActivityName()));
        graphManager.setFragmentNode(graphManager.getActivityNode().find_Fragment(new_tree));
        log("graph adjust finished");
        currentFragmentChanged = true;
        return STACK_STATUS.NEW;
    }

    public int handleFragmentOver(Boolean top_removed) {
        if (top_removed) {
            log("fragment has been traversed over, recover");
            currentFragmentNode = fragmentStack.pop();
            int hash = currentFragmentNode.getStructure_hash();
            List<String> click_list = currentFragmentNode.get_Clickable_list();
            graphManager.setActivityNode(appGraph.getAct(currentFragmentNode.getActivity()));
            graphManager.setFragmentNode(graphManager.getActivityNode().find_Fragment(hash, click_list));
        }else
            log("fragment traversed over, back");

        int position = go_back(2);
        if (position == ClientUtil.Status.OUT)
            return ClientUtil.Status.OUT;

        if (position != STACK_STATUS.RECOVER)
            return handleFragmentInStack(position);
        else
            return STACK_STATUS.RECOVER;
    }

    public void pushBackCurrentFragment(){
        fragmentStack.add(currentFragmentNode);
    }

    public RuntimeFragmentNode popCurrentFragmentNode() {
        return fragmentStack.pop();
    }

    int go_back(int limit){
        String status = "";
        int times = 0;
        while(times < limit) {
            status = ClientUtil.execute_action(Action.action_list.BACK);
            if (ClientUtil.Status.OUT == ClientUtil.checkStatus(status)) return ClientUtil.Status.OUT;

            ViewTree tree = ClientUtil.getCurrentTree();
            int position = fragmentStack.getPosition(tree);
            if (position != -1) {
                log("Back to position " + position);
                return position;
            }else if((tree.getTreeStructureHash() == currentFragmentNode.getStructure_hash())
                    || (tree.calc_similarity(currentFragmentNode.get_Clickable_list()) > CommonUtil.SIMILARITY)) {
                log("Back to top of stack");
                return STACK_STATUS.RECOVER;
            }else
                log("Back to unknown fragment, continue back");
            times += 1;
        }
        return STACK_STATUS.OUT;
    }

    public void handleRecover(){
        int recover_status = fragmentStack.recover();
        if (recover_status == STACK_STATUS.RECOVER){
            log("recover successfully");
        }else if (recover_status == STACK_STATUS.STACK){
            log("recover failed, continue crawl");
            //调整图的指针，与栈节点同步
            graphManager.setActivityNode(appGraph.getAct(fragmentStack.top().getActivity()));
            int hash = fragmentStack.top().getStructure_hash();
            List<String> click_list = fragmentStack.top().get_Clickable_list();
            graphManager.setFragmentNode(graphManager.getActivityNode().find_Fragment(hash, click_list));
            log("graph adjust finished");
        }else if (recover_status == STACK_STATUS.CLEAN){
            ActivityNode actNode = appGraph.getAct(fragmentStack.top().getActivity());
            if (actNode == null) {
                graphManager.setActivityNode(new ActivityNode(fragmentStack.top().getActivity()));
                graphManager.getActivityNode().setSer_intent(ClientUtil.getSerIntent());
                appGraph.appendActivity(graphManager.getActivityNode());
            }
            int hash = fragmentStack.top().getStructure_hash();
            List<String> click_list = fragmentStack.top().get_Clickable_list();
            FragmentNode frgNode = graphManager.getActivityNode().find_Fragment(hash, click_list);
            if (frgNode == null){
                graphManager.setFragmentNode(new FragmentNode(hash, click_list));
                graphManager.getActivityNode().appendFragment(graphManager.getFragmentNode());
            }
        }
    }

    @Override
    public void save(){
        graphManager.save();
    }

    @Override
    public void reset(){
        graphManager.reset();
    }

    @Override
    public void load(){
        graphManager.load();
    }

    public Boolean hasCurrentFragmentChanged(){
        return currentFragmentChanged;
    }

    public void setCurrentFragmentChanged(Boolean b){
        currentFragmentChanged = b;
    }

    public Boolean getStackTop(){
        if (fragmentStack.getSize() == 0) {
            log("fragment stack empty, finish exploration!");
            return false;
        }else {
            currentFragmentNode = fragmentStack.pop();
            return true;
        }
    }

    public Boolean isStackEmpty(){
        return fragmentStack.getSize() == 0;
    }

    public int getStackSize(){
        return fragmentStack.getSize();
    }

    public Boolean hasCurrentFragmentOver(){
        return (currentFragmentNode.xpath_index.size() >= currentFragmentNode.clickable_list.size()) &&
                currentFragmentNode.if_menu;
    }

    public Boolean hasFragmentXpathOver(){
        return currentFragmentNode.xpath_index.size() >= currentFragmentNode.clickable_list.size();
    }
    public Boolean hasXpathOver(ViewTree currentTree, String xpath){
        if (currentFragmentNode.xpath_index.size() >= currentFragmentNode.clickable_list.size())
            return true;
        List<ViewNode> vl = ViewUtil.getViewByXpath(currentTree.root, xpath);
        if (currentFragmentNode.path_index.size() >= vl.size()) {
            return true;
        }else
            return false;
    }

    public Boolean xpathIsUnclicked(String xpath){
        if (graphManager.getFragmentNode().unclick_list.contains(xpath)){
            log("in unclicked list, filter");
            currentFragmentNode.path_index = new ArrayList<>();
            currentFragmentNode.xpath_index.add(currentFragmentNode.xpath);
            return true;
        }else
            return false;
    }

    public String getOneXpath(){
        if (currentFragmentNode.xpath_index.size() >= currentFragmentNode.clickable_list.size()){
            return "menu";
        }

        if (currentFragmentNode.xpath_index.contains(currentFragmentNode.xpath) || currentFragmentNode.xpath == -1) {
            currentFragmentNode.xpath = CommonUtil.shuffle_random(currentFragmentNode.xpath_index, currentFragmentNode.clickable_list.size());
        }
        log("xpath index: " + currentFragmentNode.xpath +  " " + currentFragmentNode.xpath_index.size() + "/" + currentFragmentNode.clickable_list.size());
        return currentFragmentNode.get_Clickable_list().get(currentFragmentNode.xpath);
    }

    public int getXpathItem(ViewTree currentTree, String xpath){
        List<ViewNode> vl = ViewUtil.getViewByXpath(currentTree.root, xpath);
        int ser = CommonUtil.shuffle_random(currentFragmentNode.path_index, vl.size());
        currentFragmentNode.path_index.add(ser);
        log("path index: " + ser + " " + currentFragmentNode.path_index.size() + "/" + vl.size());
        return ser;
    }

    public void setCurrentXpathOver(String xpath){
        if (xpath.equals("menu")) return;
        currentFragmentNode.path_index = new ArrayList<>();
        currentFragmentNode.xpath_index.add(currentFragmentNode.xpath);
        if (!graphManager.getFragmentNode().unclick_list.contains(xpath))
            graphManager.getFragmentNode().unclick_list.add(xpath);
    }

    public Boolean hasFragmentMenuClicked(){
        return currentFragmentNode.if_menu;
    }

    public void setFragmentMenuClicked(){
        currentFragmentNode.if_menu = true;
    }

    public void setCurrentFragmentOver(){
        graphManager.getFragmentNode().setTraverse_over(true);
    }
}
