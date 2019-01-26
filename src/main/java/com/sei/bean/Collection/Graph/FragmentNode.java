package com.sei.bean.Collection.Graph;

import com.alibaba.fastjson.annotation.JSONField;
import com.sei.bean.View.Action;
import com.sei.bean.View.ViewNode;
import com.sei.bean.View.ViewTree;
import com.sei.util.CommonUtil;
import com.sei.util.ViewUtil;

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
    List<Action> allPaths;
    List<Action> interAppPaths;
    String color;
    Boolean menuClicked = false;

    public List<Integer> xpath_index;
    public List<Integer> path_index;
    public List<String> path_list;
    public List<String> edit_fields;

    public List<String> clicked_edges;
    public List<String> targets;
    public Boolean VISIT = false;

    public FragmentNode(){
        traverse_over = false;
        intrapaths = new ArrayList<>();
        interpaths = new ArrayList<>();
        allPaths = new ArrayList<>();

        interAppPaths = new ArrayList<>();

        click_list = new ArrayList<>();
        xpath_index = new ArrayList<>();
        path_index = new ArrayList<>();

        path_list = new ArrayList<>();
        edit_fields = new ArrayList<>();

        clicked_edges = new ArrayList<>();
        targets = new ArrayList<>();

        color = "white";
    }

    public FragmentNode(int hash){
        this();
        structure_hash = hash;
    }


    public FragmentNode(ViewTree tree){
        this(tree.getTreeStructureHash(), tree.getClickable_list());
        this.activity = tree.getActivityName();
        for(String xpath: tree.getClickable_list()){
            List<ViewNode> vl = ViewUtil.getViewByXpath(tree.root, xpath);
            if (vl.size() <= 8) {
                for (int i = 0; i < vl.size(); i++) {
                    ViewNode vn = vl.get(i);
                    if (vn == null) continue;
                    path_list.add(xpath + "#" + i);
                    if (vn != null && vn.getViewTag().contains("EditText"))
                        edit_fields.add(xpath + "#" + i);
                }
            }else{
                //随机抽取6个
                List<Integer> random_list = new ArrayList<>();
                for (int i = 0; i < 8; i++){
                    int ser = CommonUtil.shuffle(random_list, vl.size());
                    ViewNode vn = vl.get(ser);
                    random_list.add(ser);
                    if (vn == null) continue;
                    path_list.add(xpath + "#" + ser);
                    if (vn != null && vn.getViewTag().contains("EditText"))
                        edit_fields.add(xpath + "#" + ser);
                }
            }
        }
        path_list.add("menu");
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

    public void setAllPaths(List<Action> allPaths){
        this.allPaths = allPaths;
    }

    public List<Action> getAllPaths(){
        return allPaths;
    }

    public List<Action> getInterAppPaths(){ return this.interAppPaths;}

    public void setInterAppPaths(List<Action> interAppPaths){ this.interAppPaths = interAppPaths;}

    //为了记录点击顺序，这里把所有path记录到allPaths中
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
        allPaths.add(ea);
    }

    public void addInterpath(Action ea) {
        for (Action ee : interpaths){
            if (ee.path.equals(ea.path))
                return;
        }
        interpaths.add(ea);
        allPaths.add(ea);
    }

    public void setIntrapaths(List<Action> intrapaths) {
        this.intrapaths = intrapaths;
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
        return calc_similarity(node.get_Clickable_list());
    }

    public double calc_similarity(List<String> click_list2){
        float match = 0f;
        for (String s : click_list){
            if (click_list2.contains(s)) {
                match += 1;
            }
        }
        int tot = (click_list.size() + click_list2.size());
        return 2 * match / tot;
    }

    public List<Action> fetch_edges(){
        List<Action> actions = new ArrayList<>();
        //actions.addAll(getInterpaths());
        //actions.addAll(getIntrapaths());
        actions.addAll(getAllPaths());
        return actions;
    }
}
