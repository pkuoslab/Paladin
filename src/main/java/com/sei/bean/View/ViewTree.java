package com.sei.bean.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sei.util.CommonUtil.log;

/**
 * Created by vector on 16/5/11.
 */
public class ViewTree implements Serializable {

    //表示树的结构的hash值这里只考虑了节点的class+深度，而且对于子节点性质相同的list，子节点不考虑
    public int treeStructureHash;
    //树的跟节点

    public ViewNode root;
    static String _url = "com";
    public int totalViewCount;
    public int relativeCount;
    public String activityName;
    String html_nodes = "";

    public ViewTree() {
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getTreeStructureHash() {
        return treeStructureHash;
    }

    public void setTreeStructureHash(int treeStructureHash) {
        this.treeStructureHash = treeStructureHash;
    }

    public ViewNode getRoot() {
        return root;
    }

    public void setRoot(ViewNode root) {
        this.root = root;
    }

    public int getTotalViewCount() {
        return totalViewCount;
    }

    public void setTotalViewCount(int totalViewCount) {
        this.totalViewCount = totalViewCount;
    }

    public List<String> clickabke_list;

    public void display_click() {
        List<String> click_list = get_Clickabke_list();
        Collections.sort(click_list);
        for (String s : click_list) {
            log(s);
        }
    }

    public void setClickabke_list(List<String> clickabke_list) {
        this.clickabke_list = clickabke_list;
    }


    public List<String> get_Clickabke_list() {
        if (clickabke_list != null) {
            return clickabke_list;
        }
//        display(root, 0);
        ArrayList<String> list = new ArrayList<>();
        ArrayList<ViewNode> stack = new ArrayList<>();
        ArrayList<String> locs = new ArrayList<>();
        stack.addAll(root.getChildren());
        while (!stack.isEmpty()) {
            ViewNode node = stack.remove(0);
            if (node.clickable && node.getChildren().size() == 0) {
                String loc = (node.getX() + node.getWidth() / 2.0) + " " + (node.getY() + node.getHeight() / 2.0);
                if (!list.contains(node.xpath) && !locs.contains(loc)) {
                    list.add(node.xpath);
                    locs.add(loc);
                }
            }
            stack.addAll(node.getChildren());
        }
        clickabke_list = list;
        return clickabke_list;
    }

    public int count_leaves(){
        ArrayList<ViewNode> stack = new ArrayList<>();
        stack.addAll(root.getChildren());
        int tot = 0;
        while(!stack.isEmpty()){
            ViewNode node = stack.remove(0);
            if (node.getChildren().size() == 0)
                tot++;
            else
                stack.addAll(node.getChildren());
        }
        return tot;
    }

    public List<ViewNode> get_clickable_nodes(){
        ArrayList<String> list = new ArrayList<>();
        ArrayList<ViewNode> stack = new ArrayList<>();
        ArrayList<ViewNode> clickable_nodes = new ArrayList<>();
        ArrayList<String> locs = new ArrayList<>();
        stack.addAll(root.getChildren());
        while (!stack.isEmpty()) {
            ViewNode node = stack.remove(0);
            if (node.clickable && node.getChildren().size() == 0) {
                String loc = (node.getX() + node.getWidth() / 2.0) + " " + (node.getY() + node.getHeight() / 2.0);
                if (!list.contains(node.xpath) && !locs.contains(loc)) {
                    clickable_nodes.add(node);
                    locs.add(loc);
                }
            }
            stack.addAll(node.getChildren());
        }

        return clickable_nodes;
    }


    public static double calc_similarity(ViewTree new_tree, ViewTree old_tree){
        float match = 0f;
        for (String s : new_tree.get_Clickabke_list()){
            if (old_tree.get_Clickabke_list().contains(s)) {
                match += 1;
            }
        }
        int tot = (new_tree.get_Clickabke_list().size() + old_tree.get_Clickabke_list().size());
        return 2 * match / tot;
    }

    public double calc_similarity(List<String> clickable_list2){
        float match = 0f;
        for (String s : clickabke_list){
            if (clickable_list2.contains(s)) {
                match += 1;
            }
        }
        int tot = (clickabke_list.size() + clickable_list2.size());
        return 2 * match / tot;
    }
}