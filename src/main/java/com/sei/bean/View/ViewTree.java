package com.sei.bean.View;

import com.sei.agent.Device;
import com.sei.util.ClientUtil;
import com.sei.util.CommonUtil;
import com.sei.util.SerializeUtil;
import com.sei.util.ViewUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.sql.ClientInfoStatus;
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
    static String[] filtsBys = new String[]{"AbsListView", "GridView", "RecyclerView"};

    public static void main(String[] args){
        String content = CommonUtil.readFromFile("view.xml");
        Device d =  new Device("http://127.0.0.1:6161", 6161, "abc", "com.tencent.mm", "monkey");
        ViewTree tree = new ViewTree(d, content);
        System.out.println(tree.treeStructureHash);
        String treeStr = SerializeUtil.toBase64(tree);
        try{
            File file = new File("tree.json");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(treeStr);
            fileWriter.close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public ViewTree() {
    }

    public ViewTree(Device d, String xml){
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        Element startNode = doc.child(0).child(0);
        root = construct(startNode, 0, null);
        //activityName = ClientUtil.getTopActivityName(d);
        totalViewCount = root.total_view;
        treeStructureHash = root.getNodeRelateHash();
        getClickable_list();
    }

    ViewNode construct(Element rootView, int depth, ViewNode par){
        ViewNode now = new ViewNode();

        now.clickable = Boolean.parseBoolean(rootView.attr("clickable")) ||
                Boolean.parseBoolean(rootView.attr("long-clickable")) ||
                Boolean.parseBoolean(rootView.attr("enabled"));
        now.setResourceID(rootView.attr("resource-id"));
        now.setDepth(depth);
        List<Integer> coordinates = parse_coordinates(rootView.attr("bounds"));
        now.setX(coordinates.get(0));
        now.setY(coordinates.get(2));
        now.setWidth(coordinates.get(1)-coordinates.get(0));
        now.setHeight(coordinates.get(3)-coordinates.get(2));
        now.setViewTag(rootView.attr("class"));
        now.setContentDesc(rootView.attr("content-desc"));;
        now.setParent(par);
        if (par != null)
            now.xpath = par.xpath + "/" + ViewUtil.getLast(rootView.attr("class"));
        else
            now.xpath = ViewUtil.getLast(rootView.attr("class"));

        if (rootView.attr("class").contains("TextView")){
            now.setViewText(rootView.attr("text"));
        }else
            now.setViewText("");

        now.total_view = 1;
        String relate_hash_string = now.calStringWithoutPosition();
        Elements children = rootView.children();
        List<ViewNode> child_list = new ArrayList<>();
        for(Element child: children){
            ViewNode child_node = construct(child, depth+1, now);
            if (child_node == null) continue;
            child_list.add(child_node);
            now.total_view += child_node.total_view;
        }

        if (child_list.size() > 0){
            Collections.sort(child_list);
            boolean isListNode = isList(rootView);
            List<Integer> cnt = new ArrayList<>();
            int ccnt = 0;
            for(ViewNode childNode: child_list) {
                int id = childNode.getNodeRelateHash();
                if (cnt.contains(id))
                    ++ccnt;
                else{
                    cnt.add(id);
                    relate_hash_string += id;
                }
            }
            if (!isListNode && ccnt > child_list.size() * 2 / 3)
                isListNode = true;
            now.isList = isListNode;
            now.setChildren(child_list);
        }
        now.setNodeRelateHash(relate_hash_string.hashCode());
        return now;
    }

    public boolean isList(Element view){
        String className = view.attr("class");
        boolean beFiltered = false;
        for(int i=0; i < filtsBys.length; i++){
            beFiltered = (beFiltered || className.contains(filtsBys[i]));
            if (beFiltered)
                return true;

        }
        return beFiltered;
    }

    public List<Integer> parse_coordinates(String bounds){
        int x1 = Integer.parseInt(bounds.substring(1, bounds.indexOf(",")));
        int x2 = Integer.parseInt(bounds.substring(bounds.lastIndexOf("[")+1, bounds.lastIndexOf(",")));
        int y1 = Integer.parseInt(bounds.substring(bounds.indexOf(",")+1, bounds.indexOf("]")));
        int y2 = Integer.parseInt(bounds.substring(bounds.lastIndexOf(",")+1, bounds.lastIndexOf("]")));
        List<Integer> coordinates = new ArrayList<>();
        coordinates.add(x1);
        coordinates.add(x2);
        coordinates.add(y1);
        coordinates.add(y2);
        return coordinates;
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

    public List<ViewNode> fetch_clickable_nodes(){
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