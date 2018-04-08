package com.sei.bean.Collection;

import com.sei.bean.View.Action;
import com.sei.bean.View.ViewTree;

import java.util.HashMap;

public abstract class UiTransition {
    public HashMap<Integer, Handler> handler_table = new HashMap<>();
    public int update(Action action, ViewTree currentTree, ViewTree new_tree){return 0;}
    public interface Handler{
        public int adjust(Action action, ViewTree currentTree, ViewTree new_tree);
    }
    public void registerHandler(int status, Handler handler){
        handler_table.put(status, handler);
    }
    public void registerAllHandlers(){}
    public interface UI{
        int NEW_ACT = -3;
        int OLD_ACT_NEW_FRG = 1;
        int OLD_ACT_OLD_FRG = -1;
        int NEW_FRG = -2;
        int OLD_FRG = 2;
        int LOGIN = 3;
    }
    public void save(){}
    public void load(){}
    public void reset(){}
}
