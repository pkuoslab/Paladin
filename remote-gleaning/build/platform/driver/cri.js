"use strict";
const ws = require("ws");
const events = require("events");
const EventEmitter = events.EventEmitter;
const DEFAULT = {
    connect: {
        timeout: 5000
    }
};
class cri extends EventEmitter {
    constructor(url) {
        super();
        this._url = url;
        this._id = 0;
        this._callbacks = null;
        this._socket = null;
    }
    connect(options) {
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.connect.timeout;
        return new Promise((resolve, reject) => {
            let socket = new ws(this._url);
            socket.on("open", () => {
                this._id = 0;
                this._callbacks = new Map();
                this._socket = socket;
                resolve();
            });
            socket.on("message", (message) => this._recv(message));
            socket.once("error", reject);
            socket.once("close", this.close.bind(this));
            setTimeout(() => {
                reject(new Error("connect to websocket timeout"));
            }, timeout);
        });
    }
    send(method, params, options) {
        let id = ++this._id;
        if (params === void 0)
            params = {};
        if (options === void 0)
            options = {};
        return new Promise((resolve, reject) => {
            if (this._socket === null)
                reject(new Error("websocket connection has closed."));
            let message = JSON.stringify({ id, method, params });
            this._socket.send(message);
            this._callbacks.set(id, { resolve, reject });
            if (options.timeout !== void 0)
                setTimeout(reject, options.timeout);
        });
    }
    _recv(message) {
        let cmd = JSON.parse(message);
        if (cmd.id === undefined) {
            this.emit(cmd.method, cmd.params);
        }
        else {
            if (this._callbacks === null)
                return;
            let callback = this._callbacks.get(cmd.id);
            if (callback === undefined)
                return;
            this._callbacks.delete(cmd.id);
            if (cmd.error !== undefined)
                callback.reject(cmd.error);
            else
                callback.resolve(cmd.result);
        }
    }
    close() {
        this._id = 0;
        this._callbacks = null;
        if (this._socket != null) {
            this._socket.close();
            this._socket = null;
        }
    }
}
module.exports = cri;
