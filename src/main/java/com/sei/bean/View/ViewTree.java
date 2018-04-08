package com.sei.bean.View;

import java.io.Serializable;
import java.util.*;

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

    public Boolean existAction(String path){
        for(String c : clickable_list)
            if (path.contains(c))
                return true;

        return false;
    }

    public List<String> clickable_list;

    public void setClickable_list(List<String> clickable_list) {
        this.clickable_list = clickable_list;
    }


    public List<String> getClickable_list() {
        if (clickable_list != null) {
            return clickable_list;
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
        clickable_list = list;
        return clickable_list;
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
        for (String s : new_tree.getClickable_list()){
            if (old_tree.getClickable_list().contains(s)) {
                match += 1;
            }
        }
        int tot = (new_tree.getClickable_list().size() + old_tree.getClickable_list().size());
        return 2 * match / tot;
    }

    public double calc_similarity(List<String> clickable_list2){
        float match = 0f;
        for(String s : clickable_list2){
            if (clickable_list.contains(s))
                match += 1;
        }

        int tot = clickable_list2.size() + clickable_list.size();
        return 2 * match / tot;
    }



    public String matchPath(String path){
        int idx = path.indexOf("#");
        String xpath = path.substring(0, idx);

        String[] slist1 = xpath.split("/");
        String ret = "";
        float best = 0;

        for (String s2 : clickable_list){
            float xmatch = 0f;
            String[] slist2 = s2.split("/");
            if (!slist1[slist1.length-1].equals(slist2[slist2.length-1]))
                continue;

            for (int a=slist1.length-1, b=slist2.length-1; a >=0 && b >=0;){
                if (slist1[a].equals(slist2[b])) {
                    xmatch++;
                    --a;
                    --b;
                }else if (slist1.length < slist2.length)
                    --b;
                else
                    --a;
            }
            float rate = (2 * xmatch) / (slist1.length + slist2.length);
            if (rate > best){
                best = rate;
                ret = s2 + "#" + path.substring(idx + 1);
            }
        }

        if (best > 0.7) {
            if (!path.equals(ret)) {
                log("record path: " + path);
                log("match path: " + ret);
            }
            return ret;
        }

        return path;
    }
}