package com.sei.bean.Collection.Graph;

import com.sei.bean.View.Action;
import com.sei.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import static com.sei.util.CommonUtil.log;

public class AppGraph {
    String package_name;
    String home_activity;
    List<ActivityNode> activities;
    List<String> webFragments;


    public AppGraph(){
        activities = new ArrayList<>();
        webFragments = new ArrayList<>();
    }

    public ActivityNode getAct(String act){
        for (ActivityNode ac: activities)
            if (ac.getActivity_name().equals(act))
                return ac;
        return null;
    }

    public ActivityNode find_Activity(String activity_name){
        return getAct(activity_name);
    }

    public String getHome_activity() {
        return home_activity;
    }


    public void setHome_activity(String home_activity) {
        this.home_activity = home_activity;
    }

    public String getPackage_name() {
        return package_name;
    }

    public void setPackage_name(String package_name) {
        this.package_name = package_name;
    }


    public List<ActivityNode> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityNode> activities) {
        this.activities = activities;
    }

    public void appendActivity(ActivityNode node){
        activities.add(node);
    }

    public FragmentNode getFragment(String activity, int hash){
        ActivityNode activityNode = getAct(activity);
        if (activityNode == null) return null;
        return activityNode.getFragment(hash);
    }

    public FragmentNode getFragment(String activity_hash){
        int idx = activity_hash.indexOf("_");
        if (idx == -1) return null;
        String activity = activity_hash.substring(0, idx);
        try {
            int hash = Integer.parseInt(activity_hash.substring(idx + 1));
            return getFragment(activity, hash);
        }catch (Exception e){
            return null;
        }
    }

    public void setWebFragments(List<String> webFragments){
        this.webFragments = webFragments;
    }

    public List<String> getWebFragments(){
        return this.webFragments;
    }

    public void transfer_actions(FragmentNode fragmentNode){
        String activity = fragmentNode.getActivity();
        ActivityNode activityNode = getAct(activity);
        if (activityNode == null) {
            fragmentNode.path_list = new ArrayList<>();
            return;
        }
        List<String> effective_path = new ArrayList<>();
        List<String> targets = new ArrayList<>();

        if (activity.contains("SettingsSwitchAccountUI")) {
            fragmentNode.path_list = new ArrayList<>();
            return;
        }

        FragmentNode matchNode = null;
        FragmentNode maxNode = null;
        double max = 0.0;
        for(FragmentNode node: activityNode.getFragments()){
            if (node.getStructure_hash() == fragmentNode.getStructure_hash()){
                node.VISIT = true;
                matchNode = node;
                break;
            }
            double s = node.calc_similarity(fragmentNode);
            if (s> max){
                max = s;
                maxNode = node;
            }
        }

        if (matchNode == null && max > 0.5){
            matchNode = maxNode;
            matchNode.VISIT = true;
        }

        if (matchNode != null){
            List<Action> actions = matchNode.fetch_edges();
            for (Action action: actions){
                if (!effective_path.contains(action.path)){
                    effective_path.add(action.path);
                    targets.add(action.target);
                }
            }
        }

//        if (effective_path.size() != 0)
//            fragmentNode.path_list = new ArrayList<>();
        fragmentNode.path_list = new ArrayList<>();
        for(int i=0; i < effective_path.size(); i++){
            String path = effective_path.get(i);
            int idx = path.indexOf("#");
            String xpath;
            if (idx != -1){
                xpath = path.substring(0,idx);
            }else {
                xpath = "menu";
            }
            if (xpath.equals("menu") || fragmentNode.get_Clickable_list().contains(xpath)) {
                fragmentNode.path_list.add(path);
                fragmentNode.targets.add(targets.get(i));
            }
        }
    }
}
