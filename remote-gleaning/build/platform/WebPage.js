"use strict";
const events = require("events");
const EventEmitter = events.EventEmitter;
const CRI = require("./driver/cri.js");
const log = require("log");
const DEFAULT = {
    record_trace: {
        onload_time: 10000,
        idle_time: 5000,
        trigger_time: 5000
    }
};
const EventNamesInDomain = {
    "Network": [
        "Network.dataReceived",
        "Network.loadingFinished",
        "Network.loadingFailed",
        "Network.requestIntercepted",
        "Network.requestServedFromCache",
        "Network.requestWillBeSent",
        "Network.responseReceived",
    ],
    "Page": [
        "Page.loadEventFired",
        "Page.domContentEventFired"
    ]
};
class WebPage {
    constructor(webSocketDebuggerUrl) {
        this._driver = new CRI(webSocketDebuggerUrl);
        this._enables = new Map();
    }
    async connect(options) {
        if (options === void 0)
            options = {};
        let { timeout } = options;
        await this._driver.connect({ timeout });
    }
    async enable(domain) {
        let num = this._enables.get(domain);
        if (num === void 0)
            num = 0;
        ++num;
        this._enables.set(domain, num);
        if (num === 1)
            await this._driver.send(`${domain}.enable`);
        const pcd = new EventEmitter();
        const names = EventNamesInDomain[domain];
        const callbacks = names.map((name) => {
            return (params) => {
                pcd.emit("event", {
                    "method": name,
                    "params": params
                });
            };
        });
        names.forEach((name, nameIndex) => {
            this._driver.on(name, callbacks[nameIndex]);
        });
        pcd.once("stop", async () => {
            names.forEach((name, nameIndex) => {
                this._driver.removeListener(name, callbacks[nameIndex]);
            });
            await this.disable(domain);
        });
        return pcd;
    }
    async disable(domain) {
        let num = this._enables.get(domain);
        if (num === void 0)
            num = 0;
        if (num === 0) {
            Log.warn("Webpage", "Trying to disable a domain which is not enabled.");
            return;
        }
        --num;
        this._enables.set(domain, num);
        if (num === 0)
            await this._driver.send(`${domain}.disable`);
    }
    async proceeding() {
        const pcd = await this.enable("Page");
        const loadPcd = new EventEmitter();
        pcd.once("loadEventFired", () => {
            loadPcd.emit("onLoad");
        });
        loadPcd.once("stop", () => {
            pcd.emit("stop");
        });
        return loadPcd;
    }
    async reload() {
        await this._driver.send("Page.reload");
    }
    async clearCache() {
        await this._driver.send("Network.clearBrowserCache");
        await this._driver.send("Network.setCacheDisabled", { cacheDisabled: true });
        await this._driver.send("Network.setCacheDisabled", { cacheDisabled: false });
    }
    async getResponseBody(requestId) {
        return await this._driver.send("Network.getResponseBody", { requestId });
    }
    async evaluate(expression) {
        let result = await this._driver.send("Runtime.evaluate", {
            "expression": expression,
            "includeCommandLineAPI": true,
            "returnByValue": false,
            "silent": false,
            "generatePreview": false
        });
        let ret = undefined;
        if (result.result.type === "object") {
            result = await this._driver.send("Runtime.getProperties", {
                "objectId": result.result.objectId
            });
            ret = {};
            for (const item of result.result) {
                if (!item.isOwn)
                    continue;
                ret[item.name] = item.value.value;
            }
        }
        else {
            ret = result.result.value;
        }
        return ret;
    }
    async get_performance_timing() {
        let result = await this._driver.send("Runtime.evaluate", {
            "expression": "performance.timing",
            "includeCommandLineAPI": false,
            "returnByValue": false,
            "silent": false,
            "generatePreview": false
        });
        result = await this._driver.send("Runtime.getProperties", {
            "objectId": result.result.objectId
        });
        let metrics = {};
        result.result.forEach((property) => {
            if (property.isOwn)
                metrics[property.name] = property.value.value;
        });
        return metrics;
    }
    async get_rendered_HTML() {
        let data = await this._driver.send("DOM.getDocument");
        let rootId = data.root.nodeId;
        let result = await this._driver.send("DOM.getOuterHTML", { "nodeId": rootId });
        return result.outerHTML;
    }
    async get_webpageshot(options) {
        if (options === undefined)
            options = {};
        let shot = this._driver.send("Page.captureScreenshot");
        let cap = new Promise((resolve, reject) => {
            this._driver.once("Page.screencastFrame", (params) => {
                this._driver.send("Page.stopScreencast");
                resolve(params);
            });
            this._driver.send("Page.startScreencast");
        });
        let enough = new Promise((resolve, reject) => {
            if (options.timeout !== undefined)
                setTimeout(() => reject(new Error("record webpageshot timeout")), options.timeout);
        });
        return await Promise.race([shot, cap, enough]);
    }
    async get_url() {
        const result = await this._driver.send("Runtime.evaluate", {
            "expression": "window.location.href",
            "includeCommandLineAPI": false,
            "returnByValue": false,
            "silent": false,
            "generatePreview": false
        });
        return result.result.value;
    }
    close() {
        this._driver.close();
    }
}
module.exports = WebPage;
