package com.sei.server.component;

import fi.iki.elonen.NanoHTTPD;

public interface Handler {
    NanoHTTPD.Response onRequest(NanoHTTPD.IHTTPSession session);
}
