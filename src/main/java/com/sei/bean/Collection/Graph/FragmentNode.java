package com.sei.bean.Collection.Graph;

import com.alibaba.fastjson.annotation.JSONField;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;
import java.util.ArrayList;
import java.util.List;
import static com.sei.util.CommonUtil.log;

/**
 * Created by vector on 16/6/20.
 */
public class FragmentNode {
    String activity;
    int structure_hash;
    List<String> click_list;
    boolean traverse_over;
    boolean traverse;
    Integer title;
    Action action;
    List<Action> intrapaths;
    List<Action> interpaths;
    String color;
    Boolean menuClicked = false;
    public List<String> unclick_list;
    public List<Integer> xpath_index;
    public List<Integer> path_index;

    public FragmentNode(){
        traverse_over = false;
        intrapaths = new ArrayList<>();
        interpaths = new ArrayList<>();
        unclick_list = new ArrayList<>();
        click_list = new ArrayList<>();
        xpath_index = new ArrayList<>();
        path_index = new ArrayList<>();
        color = "white";
    }

    public FragmentNode(int hash){
        this();
        structure_hash = hash;
    }


    public FragmentNode(ViewTree tree){
        this(tree.getTreeStructureHash(), tree.getClickable_list());
        this.activity = tree.getActivityName();
    }

    public FragmentNode(int hash, List<String> click_list){
        this();
        this.click_list = click_list;
        structure_hash = hash;
    }

    public Boolean getMenuClicked(){return menuClicked;}

    public void setMenuClicked(Boolean b){menuClicked = true;}

    public String getSignature(){return activity + "_" + structure_hash;}

    public String getActivity() {return activity;}

    public void setActivity(String activity) {this.activity = activity;}

    public Integer getTitle() {
        return title;
    }

    public void setTitle(Integer title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @JSONField(serialize = false)
    FragmentNode previous = null;

    public FragmentNode getPrevious() {
        return previous;
    }

    public void setPrevious(FragmentNode previous) {
        this.previous = previous;
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
    public boolean isTraverse() {
        return traverse;
    }

    public void setTraverse(boolean traverse) {
        this.traverse = traverse;
    }

    public List<Action> getInterpaths() {
        return interpaths;
    }

    public void setInterpaths(List<Action> interpaths) {
        this.interpaths = interpaths;
    }

    public List<Action> getIntrapaths() {
        return intrapaths;
    }

    public void addIntrapath(Action ea) {
        for (Action ee : intrapaths){
            if (ee != null) {
                if (ee.getAction() == ea.getAction() && ee.path == null)
                    return;
                if (ee.path != null && ee.path.equals(ea.path))
                    return;
            }
        }
        intrapaths.add(ea);
    }

    public void addInterpath(Action ea) {
        for (Action ee : interpaths){
            if (ee.path.equals(ea.path))
                return;
        }
        interpaths.add(ea);
    }

    public void setIntrapaths(List<Action> intrapaths) {
        this.intrapaths = intrapaths;
    }

    public List<String> getUnclick_list() {
        return unclick_list;
    }

    public void setUnclick_list(List<String> unclick_list) {
        this.unclick_list = unclick_list;
    }

    public List<String> get_Clickable_list() {
        return click_list;
    }

    public void set_Clickable_list(List<String> click_list) {
        this.click_list = click_list;
    }

    public boolean isTraverse_over() {
        return traverse_over;
    }

    public void setTraverse_over(boolean traverse_over) {
        this.traverse_over = traverse_over;
    }

    public double calc_similarity(FragmentNode node){
        float match = 0f;
        for (String s : click_list){
            if (node.get_Clickable_list().contains(s)) {
                match += 1;
            }
        }
        int tot = (click_list.size() + node.get_Clickable_list().size());
        log(node.getStructure_hash() + " match : " + 2 * match + "size: " + tot + "rate: " + 2 * match / tot);
        return 2 * match / tot;
    }
}
