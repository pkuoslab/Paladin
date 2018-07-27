package com.sei.modules.test;

import com.sei.agent.Device;
import com.sei.bean.Collection.Graph.ActivityNode;
import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.Collection.Graph.GraphAdjustor;
import com.sei.bean.View.ViewTree;
import com.sei.server.component.Scheduler;
import com.sei.util.CommonUtil;
import com.sei.util.client.ClientAdaptor;

import java.util.ArrayList;
import java.util.List;

public class ReplayTest extends Thread{
    private  Scheduler scheduler;
    private GraphAdjustor graphAdjustor;
    private double match = 0.0;
    private Device d;
    private List<String> visits;
    public ReplayTest(Device d, Scheduler scheduler){
        this.d = d;
        this.scheduler = scheduler;
        this.graphAdjustor = scheduler.graphAdjustor;
        visits = new ArrayList<>();
    }

    @Override
    public void run(){
        double tot = 0.0;
        for(int i = 0;i < graphAdjustor.appGraph.getActivities().size();i++){
        //for(int i=graphAdjustor.appGraph.getActivities().size()-1; i>=0; i--){
            ActivityNode an = graphAdjustor.appGraph.getActivities().get(i);
            CommonUtil.log("No." + i);
            tot += an.getFragments().size();
            for(int j = an.getFragments().size()-1;j >= 0;j--){
                FragmentNode fn = an.getFragments().get(j);
                if (visits.contains(fn.getSignature()))
                    continue;

                List<String> routes = new ArrayList<>();
                routes.add(fn.getSignature());
                d.setRoute_list(routes);
                ClientAdaptor.stopApp(d, d.current_pkg);
                d.start();

                while(true){
                    if (d.Exit){
                        checkOutcome(d, fn);
                        d = new Device(d.ip, d.port, d.serial, d.current_pkg, d.password, d.mode);
                        scheduler.bind(d);
                        break;
                    }
                    CommonUtil.sleep(2000);
                }
            }
        }
        CommonUtil.log("total: " + tot + " match: " + visits.size());
        CommonUtil.log("total reproducible rate: " + visits.size() / tot);
    }

    private void checkOutcome(Device d, FragmentNode fn){
        try {
            ViewTree tree = d.getCurrentTree();
            double sm = fn.calc_similarity(tree.getClickable_list());
            String s = tree.getActivityName() + "_" + tree.getTreeStructureHash();
            d.log(s + "-" + fn.getSignature() + " size: " + d.visits.size() + " rate: " + sm);

            for(String n: d.visits){
                if (!visits.contains(n)){
                    visits.add(n);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
