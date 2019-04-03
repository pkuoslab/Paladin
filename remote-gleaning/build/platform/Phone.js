"use strict";
const adb = require("./driver/adb.js");
const http = require("http");
const events = require("events");
const EventEmitter = events.EventEmitter;
const log = require("log");
const DEFAULT = {
    device: {
        device_index: 0
    },
    get_active_debugger_url: {
        port: 9222,
        http_get_timeout: 5000
    }
};
class Phone {
    static async devices() {
        return await adb.devices();
    }
    static async device(options) {
        if (options === void 0)
            options = {};
        let { device, device_index } = options;
        let phone;
        if (device === void 0) {
            let devices = await Phone.devices();
            if (device_index === void 0)
                device_index = DEFAULT.device.device_index;
            if (device_index >= devices.length)
                throw new Error("the given device index larger than the number of devices detected.");
            device = devices[device_index];
        }
        return new Phone(device);
    }
    constructor(device) {
        if (device === void 0)
            throw new Error("No device number provided.");
        this._driver = new adb(device);
    }
    activityProceeding() {
        const pcd = new EventEmitter();
        setTimeout(() => {
            pcd.emit("onLoad");
        }, 5000);
        return pcd;
    }
    get_serial() {
        return this._driver.get_serial();
    }
    async get_app_name_by_pid(pid, options) {
        if (pid === void 0)
            throw new Error("pid must not be omitted.");
        if (options === void 0)
            options = {};
        let { timeout } = options;
        let procs = await this._driver.ps({ pid }, { timeout });
        if (procs.length === 0)
            throw new Error("process " + pid + " not found.\n");
        return procs[0].name;
    }
    async get_pid_by_app_name(name, options) {
        if (name === void 0)
            throw new Error("APP name must not be empty.");
        if (options === void 0)
            options = {};
        let { timeout } = options;
        let procs = await this._driver.ps(void 0, { timeout });
        let proc = procs.find((proc) => proc.name === name);
        if (proc === void 0)
            throw new Error("process \"" + name + "\" not found.\n");
        return proc.pid;
    }
    async get_focus_on(options) {
        if (options === void 0)
            options = {};
        const { timeout } = options;
        let sysinfo;
        do {
            sysinfo = await this._driver.dumpsys({
                "service": "window",
                "args": ["windows"]
            }, { timeout });
        } while (sysinfo.mCurrentFocus === void 0);
        const focus = sysinfo.mCurrentFocus.split("/");
        return {
            "app": focus[0],
            "activity": focus[1]
        };
    }
    async get_active_debugger_url(options) {
        if (options === void 0)
            options = {};
        let { app } = await this.get_focus_on();
        let pid = await this.get_pid_by_app_name(app);
        await this.check_unix_domain_socket("@webview_devtools_remote_" + pid);
        let { port } = options;
        if (port === void 0)
            port = DEFAULT.get_active_debugger_url.port;
        let { forward_timeout } = options;
        await this.forward({
            "remote": "localabstract:webview_devtools_remote_" + pid,
            "local": "tcp:" + port
        }, { forward_timeout });
        let pages = await new Promise((resolve, reject) => {
            let c = http.get({
                "hostname": "localhost",
                "port": port,
                "path": "/json"
            }, (res) => {
                const { statusCode, headers } = res;
                if (statusCode != 200)
                    reject(new Error(`HTTP response status code ${statusCode}`));
                let rawData = "";
                res.on("data", (chunk) => { rawData += chunk; });
                res.once("end", () => {
                    const parsedData = JSON.parse(rawData);
                    resolve(parsedData);
                });
            });
            c.once("error", reject);
            let { http_get_timeout } = options;
            if (http_get_timeout === void 0)
                http_get_timeout = DEFAULT.get_active_debugger_url.http_get_timeout;
            c.setTimeout(http_get_timeout, () => {
                reject(new Error("HTTP GET time out."));
            });
        });
        let found = pages.find((page, pageIndex) => {
            if (page.description !== void 0) {
                let desc;
                try {
                    desc = JSON.parse(page.description);
                    if (desc.attached !== void 0 && !desc.attached)
                        return false;
                    if (desc.visible !== void 0 && !desc.visible)
                        return false;
                }
                catch (e) {
                }
                ;
            }
            if (options.url !== void 0)
                if (page.title !== options.url)
                    return false;
            return true;
        });
        if (found === void 0 || found.webSocketDebuggerUrl === void 0)
            throw new Error("No page found debuggable.");
        let webSocketDebuggerUrl = found.webSocketDebuggerUrl;
        return webSocketDebuggerUrl;
    }
    async forward(params, options) {
        return await this._driver.forward(params, options);
    }
    async pull(remote, local) {
        return await this._driver.pull({
            "remote_path": remote,
            "local_path": local
        });
    }
    async check_unix_domain_socket(sockname) {
        let sockets = await this._driver.unix_domain_sockets();
        let socket = sockets.find((socket) => socket.Path === sockname);
        if (socket === void 0)
            throw new Error("socket not found.");
        return;
    }
    async save_screenshot(path, options) {
        if (options === void 0)
            options = {};
        let { screenshot_timeout } = options;
        await this._driver.screenshot({ path }, { timeout: screenshot_timeout });
    }
    async get_touch_dev_info(options) {
        if (options === void 0)
            options = {};
        let { timeout, plain_text } = options;
        if (plain_text === void 0)
            plain_text = true;
        let devinfos = await this._driver.getevent({
            plain_text,
            possible_events: true
        }, { timeout });
        let info = devinfos.find((info) => info.name === "touch_dev");
        if (info === void 0)
            throw new Error("No touch_dev detected.");
        return info;
    }
    async input(input, options) {
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (input.type === "tap") {
            await this._driver.input({
                command: "tap",
                location: input.location
            }, { timeout });
        }
        else if (input.type === "swipe") {
            await this._driver.input({
                command: "swipe",
                from: input.from,
                to: input.to,
                duration: input.duration
            }, { timeout });
        }
        else if (input.type === "unknown")
            throw new Error("unknown input type.");
    }
    async get_resolution(options) {
        if (options === void 0)
            options = {};
        let { timeout } = options;
        return await this._driver.wm({
            subcommand: "size"
        }, { timeout });
    }
    getevent(args) {
        if (args === void 0)
            throw new Error("args should be given.");
        if (args.device === void 0)
            throw new Error("device should be specified.");
        let { device } = args;
        let od_pcd = this._driver.ia_od({
            type: {
                alias: "x",
                type: "x",
                size: 2
            },
            file: device
        });
        let event_pcd = new EventEmitter();
        let data = "";
        const callback = (chunk) => {
            data += chunk;
            let lines = data.split("\n");
            data = lines.pop();
            for (let line of lines) {
                let frags = line.split(/\s+/);
                if (frags.length !== 9) {
                    log.error("Phone.getevent", "chunk format error");
                    event_pcd.emit("error");
                    return;
                }
                let t1, t2;
                t1 = parseInt(frags[1], 16);
                t2 = parseInt(frags[2], 16);
                if (isNaN(t1) || isNaN(t2)) {
                    log.error("Phone.getevent", "Bad format of tv_sec.");
                    event_pcd.emit("error");
                }
                let sec = t2 * 0x10000 + t1;
                t1 = parseInt(frags[3], 16);
                t2 = parseInt(frags[4], 16);
                if (isNaN(t1) || isNaN(t2)) {
                    log.error("Phone.getevent", "Bad format of tv_usec.");
                    event_pcd.emit("error");
                }
                let usec = t2 * 0x10000 + t1;
                t1 = parseInt(frags[5], 16);
                if (isNaN(t1)) {
                    log.error("Phone.getevent", "Bad format of type.");
                    event_pcd.emit("error");
                }
                ;
                let type = t1;
                t1 = parseInt(frags[6], 16);
                if (isNaN(t1)) {
                    log.error("Phone.getevent", "Bad format of code.");
                    event_pcd.emit("error");
                }
                ;
                let code = t1;
                t1 = parseInt(frags[7], 16);
                t2 = parseInt(frags[8], 16);
                if (isNaN(t1) || isNaN(t2)) {
                    log.error("Phone.getevent", "Bad format of value.");
                    event_pcd.emit("error");
                }
                let value = t2 * 0x10000 + t1;
                let event = {
                    sec, usec, type, code, value
                };
                event_pcd.emit("event", event);
            }
        };
        od_pcd.on("data", callback);
        od_pcd.once("close", () => {
            event_pcd.emit("close");
        });
        event_pcd.once("stop", () => {
            od_pcd.removeListener("data", callback);
            od_pcd.emit("stop");
        });
        return event_pcd;
    }
    logcat(filter) {
        return this._driver.ia_logcat({
            format: "raw",
            filterspecs: ["*:S", filter]
        });
    }
    async clearLogcat() {
        return await this._driver.logcat({
            clear: true
        });
    }
    screenrecord(remotePath) {
        return this._driver.ia_screenrecord({
            file: remotePath
        });
    }
}
module.exports = Phone;
