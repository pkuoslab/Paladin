package com.sei.bean.Collection.Stack;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.GraphManager;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;

import java.util.LinkedList;
import java.util.List;

import static com.sei.util.CommonUtil.log;

/**
 * Created by mike on 17-9-19.
 */

public class FragmentStack {
    public List<RuntimeFragmentNode> stack;

    public FragmentStack(){
        stack = new LinkedList<>();
    }

    public RuntimeFragmentNode top() {
        if (!stack.isEmpty())
            return stack.get(stack.size()-1);
        log("top stack empty");
        return null;
    }

    public RuntimeFragmentNode get(int idx){
        return stack.get(idx);
    }

    public void add(RuntimeFragmentNode node){
        stack.add(node);
    }

    public int getSize() { return stack.size();}

    public RuntimeFragmentNode find(int hash){
        for (RuntimeFragmentNode rfn : stack){
            if (rfn.getStructure_hash() == hash)
                return rfn;
        }
        return null;
    }

    public int getPosition(ViewTree tree){
        return matchPosition(tree, CommonUtil.SIMILARITY);
    }

    public int matchPosition(ViewTree tree, double sm){
        for (int i=stack.size() - 1; i >=0; i--){
            if (stack.get(i).getStructure_hash() == tree.getTreeStructureHash())
                return i;
        }

        for (int i=stack.size() - 1; i >= 0; i--){
            String act = stack.get(i).activity;
            if (!tree.getActivityName().equals(act))
                continue;
            double s = tree.calc_similarity(stack.get(i).get_Clickable_list());
            if (s > sm)
                return i;
        }

        return -1;
    }

    public boolean has(int hash){
        for (RuntimeFragmentNode node : stack){
            for(RuntimeFragmentNode rfn : stack)
                if (rfn.getStructure_hash() == hash)
                    return true;

        }
        return false;
    }

    public RuntimeFragmentNode pop(){
        if (!stack.isEmpty())
            return stack.remove(stack.size()-1);
        log("pop stack empty");
        return null;
    }

    public int recover(int start){
        String status = "";
        ViewTree tree;
        int limits = 0;
        for(int i=start; i < stack.size()-1; i++){
            RuntimeFragmentNode rfn = stack.get(i);
            Action action = rfn.getAction();
            if (action.getAction() == Action.action_list.MENU)
                status = ClientUtil.execute_action(action.getAction());
            else if(action.getAction() == Action.action_list.ENTERTEXT) {
                ClientUtil.execute_action(Action.action_list.CLICK, action.getPath());
                status = ClientUtil.execute_action(Action.action_list.ENTERTEXT, action.getContent());
            }else
                status = ClientUtil.execute_action(action.getAction(), action.getPath());
            ClientUtil.checkStatus(status);
            tree = ClientUtil.getCurrentTree();
            if (tree == null){
                return GraphManagerWithStack.STACK_STATUS.OUT;
            }

            int tree_hash = tree.getTreeStructureHash();
            int target_hash = stack.get(i+1).getStructure_hash();
            if (tree_hash == target_hash || (tree.getActivityName().equals(stack.get(i+1).getActivity()) &&
                    tree.calc_similarity(stack.get(i+1).get_Clickable_list()) > CommonUtil.SIMILARITY))
                continue;

            int position = getPosition(tree);
            if (position == i) {
                int c = i+1;
                log("can not recover, cut above " + c + "/" + stack.size());
                for(int j=stack.size()-1; j >= c; j--)
                    stack.remove(j);
                return GraphManagerWithStack.STACK_STATUS.STACK;
            }else if (position != -1 && position != i+1){
                if (limits >= 3){
                    int c = position + 1;
                    log("continuously back to position " + position + " cut above " + c + "/" + stack.size());
                    for(int j=stack.size()-1; j >= c; j--)
                        stack.remove(j);
                    return GraphManagerWithStack.STACK_STATUS.STACK;
                }
                limits += 1;
                i = position-1;
            }else if (position == -1){
                int c = i + 1;
                log("recover to node not in stack, cut above " + c + "/" + stack.size());
                for(int j=stack.size()-1; j >= c; j--)
                    stack.remove(j);
                //RuntimeFragmentNode node = new RuntimeFragmentNode(tree);
                //stack.add(node);
                return GraphManagerWithStack.STACK_STATUS.NOT;
            }
        }
        return GraphManagerWithStack.STACK_STATUS.RECOVER;
    }

    public int recover(Device d, int start){
        int response = Device.UI.OUT;

        int limits = 0;
        for(int i=start; i < stack.size()-1; i++){
            RuntimeFragmentNode rfn = stack.get(i);
            Action action = rfn.getAction();
            if (action.getAction() == Action.action_list.MENU)
                ClientUtil.execute_action(d, action.getAction());
            else if(action.getAction() == Action.action_list.ENTERTEXT) {
                ClientUtil.execute_action(d, Action.action_list.CLICK, action.getPath());
                ClientUtil.execute_action(d, Action.action_list.ENTERTEXT, action.getContent());
            }else
                ClientUtil.execute_action(d, action.getAction(), action.getPath());

//            if (response == Device.UI.OUT) {
//                d.log("out");
//                return Device.UI.OUT;
//            }

            ViewTree tree = ClientUtil.getCurrentTree(d);
            if (tree == null) {
                CommonUtil.sleep(2000);
                tree = ClientUtil.getCurrentTree(d);
                if (tree == null) {
                    d.log("out");
                    return Device.UI.OUT;
                }
            }

            int tree_hash = tree.getTreeStructureHash();
            int target_hash = stack.get(i+1).getStructure_hash();
            int position = getPosition(tree);

            if (position == i+1)
                continue;

            if (position == i) {
                int c = i+1;
                d.log("can not recover, cut above " + c + "/" + stack.size());
                for(int j=stack.size()-1; j >= c; j--)
                    stack.remove(j);
                return Device.UI.SAME;
            }else if (position != -1 && position != i+1){
                if (limits >= 3){
                    int c = position + 1;
                    d.log("continuously back to position " + position + " cut above " + c + "/" + stack.size());
                    for(int j=stack.size()-1; j >= c; j--)
                        stack.remove(j);
                }
                limits += 1;
                i = position-1;
            }else if (position == -1){
                int c = i + 1;
                d.log("recover to node not in stack, cut above " + c + "/" + stack.size());
                for(int j=stack.size()-1; j >= c; j--)
                    stack.remove(j);
                return Device.UI.NEW;
            }
        }
        d.log("recover successfully");
        return Device.UI.SAME;

    }

    public int recover(){
        ViewTree tree = ClientUtil.getCurrentTree();
        int position = getPosition(tree);
        if (position != -1) {
            log("current position: " + position + " /" + stack.size());
            return recover(position);
        }else{
            CommonUtil.sleep(5000);
            ClientUtil.refreshUI();
            tree = ClientUtil.getCurrentTree();
            position = getPosition(tree);
            if (position == -1) {
                log("current tree: " + tree.getTreeStructureHash());
                log("current stack: ");
                for(RuntimeFragmentNode frg: stack){
                    log("hash: " + frg.getStructure_hash() + " " + tree.calc_similarity(frg.get_Clickable_list()));
                }
                RuntimeFragmentNode rfn = new RuntimeFragmentNode(tree);
                stack = new LinkedList<>();
                stack.add(rfn);
                return GraphManagerWithStack.STACK_STATUS.CLEAN;
            }else{
                log("current position: " + position + " /" + stack.size());
                return recover(position);
            }
        }
    }

    public void clear(){
        for(int i=stack.size()-1; i >=0; i--)
            stack.remove(i);
    }

}
