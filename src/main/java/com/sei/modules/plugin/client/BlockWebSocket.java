package com.sei.modules.plugin.client;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class BlockWebSocket {
    private String msg;
    private Object lock;
    private WebSocket client;

    public BlockWebSocket(){
        lock = new Object();
    }

    public void connect(String wsURL) throws URISyntaxException, IOException, WebSocketException{
        BlockWebSocket self = this;
        URI url = new URI(wsURL);

        client = new WebSocketFactory().createSocket(url, 5000);
        client.connect();

        client.addListener(new WebSocketAdapter(){
            @Override
            public void onTextMessage(WebSocket webSocket, String message){
                self.msg = message;
                synchronized (lock){
                    self.lock.notify();
                }
            }
        });
    }


    public String send(String message) throws InterruptedException{
        client.sendText(message);
        synchronized (lock){
            lock.wait();
        }

        return this.msg;
    }

    public void close(){
        client.sendClose();
    }
}
