package com.sei.bean.View;

import com.alibaba.fastjson.JSON;
import com.sei.agent.Device;
import com.sei.util.*;
import com.sei.util.client.ClientAdaptor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.xml.xpath.XPath;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.sql.ClientInfoStatus;
import java.util.*;
import com.sei.util.CmdUtil;

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
    public boolean hasWebview = false;
    String html_nodes = "";
    static String[] filtsBys = new String[]{"AbsListView", "GridView", "RecyclerView"};

    public String Deeplink = "";

    public ViewTree() {
    }

    public ViewTree(Device d, String xml){
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        activityName = ClientAdaptor.getTopActivityName(d);
        if (doc.children().size() == 0) {
            root = null;
            return;
        }

        Element startNode = doc.child(0);
        if (startNode == null) return;
        root = construct(startNode, 0, null);
        totalViewCount = root.total_view;
        treeStructureHash = root.getNodeRelateHash();
        getClickable_list();
    }

    ViewNode construct(Element rootView, int depth, ViewNode par){
        ViewNode now = new ViewNode();
        String relate_hash_string;
        if (!rootView.tagName().equals("hierarchy")) {
            now.clickable = Boolean.parseBoolean(rootView.attr("clickable")) ||
                    Boolean.parseBoolean(rootView.attr("long-clickable")) ||
                    Boolean.parseBoolean(rootView.attr("enabled"));
            // not include native background view
            if (rootView.attr("resource-id").contains("BarBackground")) {
                return null;
            }

            now.setResourceID(rootView.attr("resource-id"));
            now.setDepth(depth);
            List<Integer> coordinates = parse_coordinates(rootView.attr("bounds"));
            now.setX(coordinates.get(0));
            now.setY(coordinates.get(2));
            now.setWidth(coordinates.get(1) - coordinates.get(0));
            now.setHeight(coordinates.get(3) - coordinates.get(2));
            now.setViewTag(rootView.attr("class"));
            now.setContentDesc(rootView.attr("content-desc"));
            now.setParent(par);
            if (par != null)
                now.xpath = par.xpath + "/" + ViewUtil.getLast(rootView.attr("class"));
            else
                now.xpath = ViewUtil.getLast(rootView.attr("class"));

            if (rootView.attr("class").contains("TextView")) {
                now.setViewText(rootView.attr("text"));
            } else
                now.setViewText("");


            relate_hash_string = now.calStringWithoutPosition();
            now.total_view = 1;

            // now include webview content
            // change by ycx on 2019/1/25
            if (rootView.attr("class").contains("webkit") ||
                    rootView.attr("class").contains("WebView")){

                //CommonUtil.log("find web view!");
                hasWebview = true;
                now.setXpath("@"+now.xpath);

                if(CommonUtil.WEBVIEW) {
                    String html = "";
                    for(int i = 0; i < 3; i++) {
                        html = ConnectUtil.sendHttpGet("http://127.0.0.1:9234/getDOM");
                        if(!html.contains("Server returned HTTP response code: 500"))  {
                            now.setDOMTree(html);
                            break;
                        }
                        CommonUtil.sleep(500);
                        // restart process
                        CmdUtil.restartProcess();
                        if(i == 2) CommonUtil.log("fail to get dom tree!");
                    }

                    if(!buildLeafNode(now)) CommonUtil.log("build leaf node failed!");
                }

                // add children's hash string into relate_hash_string
                for(ViewNode child : now.getChildren()){
                    relate_hash_string += child.calStringWithoutPosition();
                }

                now.setNodeRelateHash(relate_hash_string.hashCode());
                return now;
            }
        }else{
            now.xpath = "";
            relate_hash_string = "";
        }
        Elements children = rootView.children();
        List<ViewNode> child_list = new ArrayList<>();
        for(Element child: children){
            if (child.attr("package").equals("com.android.systemui"))
                continue;
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
        //ArrayList<ViewNode> stack = new ArrayList<>();
        Queue<ViewNode> queue = new LinkedList<>();
        ArrayList<String> locs = new ArrayList<>();
        queue.addAll(root.getChildren());
        while (!queue.isEmpty()) {
            ViewNode node = queue.poll();
            if (node.clickable && node.getChildren().size() == 0) {
                String loc = (node.getX() + node.getWidth() / 2.0) + " " + (node.getY() + node.getHeight() / 2.0);
                if (!list.contains(node.xpath) && !locs.contains(loc)) {
                    list.add(node.xpath);
                    locs.add(loc);
                }
            }
            queue.addAll(node.getChildren());
        }
        clickable_list = list;
        return clickable_list;
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


    /*
     * 将DOM树压平放入view tree中，采用递归深搜
     */
    public void DFS(Element element, ViewNode now, String xpath, boolean clickable) {
        //CommonUtil.log("============================================");
        //CommonUtil.log("DFS on element:" + element.className());
        //CommonUtil.log("element html:" + element.html());
        Elements children = element.children();
        if (children.size() == 0  && !element.html().equals("")) {
            // 叶子结点
            ViewNode leaf = new ViewNode();
            leaf.setParent(now);
            leaf.setWebContent(true);
            leaf.setViewText(element.html());
            leaf.setWebXpath(xpath);
            for(int i = 0; i < 3; i ++) {
                try {
                    String result = ConnectUtil.sendHttpGet("http://127.0.0.1:9234/getLocation?xpath="+xpath);
                    //CommonUtil.log("result:" + result);
                    JSONObject res = new JSONObject(result);

                    // set x,y
                    int x = new Double(res.getDouble("left") + 1).intValue();
                    int y = new Double(res.getDouble("top") + 1).intValue();
                    x += now.getX();
                    y += now.getY();
                    leaf.setX(x);
                    leaf.setY(y);

                    // set width and height
                    int width = res.getInt("width");
                    int height = res.getInt("height");
                    leaf.setWidth(width);
                    leaf.setHeight(height);

                    // other things
                    leaf.clickable = clickable;
                    // 统一定义为webNode
                    leaf.setViewTag("webNode");
                    // 这些叶子结点的xpath做特殊处理，定义为"@X,Y"
                    leaf.setXpath("@"+x+","+y);
                    break;

                } catch (Exception e) {
                    CommonUtil.sleep(500);
                    if(i == 2) e.printStackTrace();
                }
            }
            now.addChild(leaf);
        }

        // tag2Num: key -- tagName   value -- total number of this tag
        // tag2Cnt: key -- tagName   value -- current index of this tag
        // all 1 based index
        //Map<String, Integer> tag2Num = new HashMap<String, Integer>();
        Map<String, Integer> tag2Cnt = new HashMap<String, Integer>();

        // init tag2Num and tag2Cnt
        for(Element e : children) {
            String tagName = e.tagName();
            tag2Cnt.put(tagName, 1);
        }

        for(Element e : children) {
            String tagName = e.tagName();
            int oldCnt = tag2Cnt.get(tagName);
            int newCnt = oldCnt+1;
            tag2Cnt.put(tagName, newCnt);
            String newXpath = xpath+"/"+tagName+"["+oldCnt+"]";
            // 超链接或者列表可点击
            if (tagName.equals("a")||tagName.equals("li")) {
                DFS(e, now, newXpath, true);
            } else {
                DFS(e, now, newXpath, clickable);
            }

        }
    }


    /*
     * 将DOM树压平放入view tree中，调用新接口。
     */
    public boolean buildLeafNode(ViewNode now) {
        for(int i = 0; i < 3; i++) {
            try {
                String result = ConnectUtil.sendHttpGet("http://127.0.0.1:9234/getAllLeavesLocations");
                //CommonUtil.log("result:" + result);
                JSONObject res = new JSONObject(result);
                assert(res.has("leaves"));
                double devicePixelRatio = res.getDouble("devicePixelRatio");
                JSONArray leaves = res.getJSONArray("leaves");
                int length = leaves.length();
                for(int j = 0; j < length; j++) {
                    ViewNode leaf = new ViewNode();
                    JSONObject JSONLeaf = leaves.getJSONObject(j);

                    leaf.setParent(now);
                    leaf.setWebContent(true);
                    leaf.setViewText(JSONLeaf.getString("innerHTML"));
                    leaf.clickable = JSONLeaf.getBoolean("clickable");
                    leaf.setViewTag("webNode/"+JSONLeaf.getString("tag"));

                    JSONObject location = JSONLeaf.getJSONObject("location");

                    //set x,y
                    int x = new Double(location.getDouble("left") * devicePixelRatio).intValue();
                    int y = new Double(location.getDouble("top") * devicePixelRatio).intValue();
                    x += now.getX();
                    y += now.getY();
                    leaf.setX(x);
                    leaf.setY(y);
                    // set width and height
                    int width = new Double(location.getInt("width") * devicePixelRatio).intValue();
                    int height = new Double(location.getInt("height") * devicePixelRatio).intValue();
                    leaf.setWidth(width);
                    leaf.setHeight(height);

                    // 这些叶子结点的xpath做特殊处理，定义为"@xpath"
                    String xpath = new String(JSONLeaf.getString("xpath"));
                    leaf.setXpath("@"+xpath);

                    // depth
                    leaf.setDepth(now.getDepth()+1);

                    now.addChild(leaf);
                }
                break;
            } catch (Exception e) {
                CmdUtil.restartProcess();
                if(i == 2) {
                    //e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    public String getDeeplink() {
        return Deeplink;
    }

    public void setDeeplink(String deeplink) {
        Deeplink = deeplink;
    }
}