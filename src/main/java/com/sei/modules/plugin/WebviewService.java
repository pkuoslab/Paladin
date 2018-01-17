package com.sei.modules.plugin;

import fi.iki.elonen.util.ServerRunner;

public class WebviewService extends Thread{

    @Override
    public void run(){
        WebviewHelper webviewHelper = new WebviewHelper();
        ServerRunner.run(WebviewHelper.class);
        return;
    }
}
