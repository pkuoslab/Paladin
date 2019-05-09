package com.sei.server.component;

import com.sei.bean.Collection.Graph.FragmentNode;
import com.sei.bean.View.Action;

import java.util.List;

public class Decision {
    public interface CODE{
        int CONTINUE = 0;
        int GO = 1;
        int SEQ = -1;
        int STOP = 2;
        int RESTART = 3;
        int DONOTHING = 4;
    }
    public int code;
    public Action action;
    //public FragmentNode fragmentNode;
    public String signature;
    public List<Action> actions;
    public int target_id;
    public String target_serial;
    public int position;

    public Decision(int code, Action action){
        this.code = code;
        this.action = action;
    }

    public Decision(int code, String signature){
        this.code = code;
        this.signature = signature;
    }

    public Decision(int code, List<Action> actions){
        this.code = code;
        this.actions = actions;
    }

    public Decision(int code){
        this.code = code;
    }
}
