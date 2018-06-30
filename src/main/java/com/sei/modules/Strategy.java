package com.sei.modules;

import com.sei.bean.View.ViewTree;
import com.sei.server.component.Decision;

public interface Strategy {
    public Decision make(String serial, ViewTree currentTree, ViewTree newTree, Decision prev_decision, int response);
}
