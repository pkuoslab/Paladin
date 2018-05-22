package com.sei.bean.Collection.Stack;

import com.sei.agent.Device;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import com.sei.util.CommonUtil;
import com.sei.util.client.ClientAdaptor;
import com.sei.util.client.ClientAutomator;

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

    public Boolean contains(String activity){
        for (RuntimeFragmentNode runtimeFragmentNode: stack){
            if (runtimeFragmentNode.activity.equals(activity))
                return true;
        }

        return false;
    }

    public int getPosition(ViewTree tree){
        return matchPosition(tree.getActivityName(), tree.getTreeStructureHash(), tree.getClickable_list(), CommonUtil.SIMILARITY);
    }

    public int getPosition(String activity, int hash, List<String> clickable_list){
        return matchPosition(activity, hash, clickable_list, CommonUtil.SIMILARITY);
    }

    public int matchPosition(String activity, int hash, List<String> clickable_list, double sm){
        for (int i =stack.size()-1; i >=0; i--){
            if (stack.get(i).getStructure_hash() == hash)
                return i;
        }

        for (int i=stack.size() - 1; i >= 0; i--){
            String act = stack.get(i).activity;
            if (!activity.equals(act))
                continue;
            double s = CommonUtil.calc_similarity(clickable_list, stack.get(i).get_Clickable_list());
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

    public int recover(Device d, int start) throws Exception{
        int response = Device.UI.OUT;

        int limits = 0;
        ViewTree tree = d.currentTree;
        for(int i=start; i < stack.size()-1; i++){
            RuntimeFragmentNode rfn = stack.get(i);
            Action action = rfn.getAction();
            if (action.getAction() == Action.action_list.MENU)
                ClientAdaptor.execute_action(d, action.getAction(), tree, "");
            else if(action.getAction() == Action.action_list.ENTERTEXT) {
                ClientAdaptor.execute_action(d, Action.action_list.CLICK, tree, action.getPath());
                ClientAdaptor.execute_action(d, Action.action_list.ENTERTEXT, tree, action.getContent());
            }else
                ClientAdaptor.execute_action(d, action.getAction(), tree, action.getPath());


            tree = ClientAutomator.getCurrentTree(d);
            if (tree == null) {
                CommonUtil.sleep(2000);
                tree = ClientAdaptor.getCurrentTree(d);
                if (tree == null) {
                    d.log("out");
                    return Device.UI.OUT;
                }
            }

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

    public void clear(){
        for(int i=stack.size()-1; i >=0; i--)
            stack.remove(i);
    }

}
