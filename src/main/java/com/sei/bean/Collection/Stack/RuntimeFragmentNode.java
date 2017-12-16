package com.sei.bean.Collection.Stack;

import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 17-9-18.
 */

public class RuntimeFragmentNode {
    private int structure_hash;
    public String activity;
    private Action action;
    public List<Integer> xpath_index;
    public List<Integer> path_index;
    public int xpath;
    public List<String> clickable_list;
    public boolean if_menu;
    public boolean if_input;
    public List<String> filter;



    public List<String> get_Clickable_list() {
        return clickable_list;
    }

    public void set_Clickable_list(List<String> clickable_list) {
        this.clickable_list = clickable_list;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getStructure_hash() {
        return structure_hash;
    }

    public void setStructure_hash(int structure_hash) {
        this.structure_hash = structure_hash;
    }

    public void setActivity(String act){activity = act;}
    public String getActivity(){return activity;}

    public RuntimeFragmentNode(){
        filter = new ArrayList<>();
        xpath_index = new ArrayList<>();
        path_index = new ArrayList<>();
        if_menu = false;
        if_input = false;
        xpath = -1;
        //xpath_index = -1;
        //path_index = 0;
    }
    public RuntimeFragmentNode(String act, int hash){
        this();
        structure_hash = hash;
        activity = act;
    }

    public RuntimeFragmentNode(ViewTree tree){
        this();
        structure_hash = tree.getTreeStructureHash();
        activity = tree.getActivityName();
        clickable_list = tree.get_Clickabke_list();
    }
}
