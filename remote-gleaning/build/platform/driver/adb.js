"use strict";
const child_process = require("child_process");
const spawn = child_process.spawn;
const events = require("events");
const EventEmitter = events.EventEmitter;
const pSpawn = require("../../util/pSpawn.js");
const iaSpawn = require("../../util/iaSpawn.js");
const DEFAULT = {
    devices: {
        timeout: 5000
    },
    forward: {
        timeout: 5000
    },
    ps: {
        timeout: 5000
    },
    dumpsys: {
        timeout: 5000
    },
    screenshot: {
        timeout: 15000
    },
    cat: {
        timeout: 5000
    },
    pull: {
        timeout: 5000
    },
    input: {
        timeout: 5000
    },
    wm: {
        timeout: 5000
    },
    getevent: {
        timeout: 5000
    },
    monkey: {
        timeout: 5000
    },
    am: {
        timeout: 5000
    },
    logcat: {
        timeout: 5000
    }
};
class adb {
    static async devices(options) {
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.devices.timeout;
        let stdout = await pSpawn("adb", ["devices"], { timeout });
        let devices = stdout.trim().split("\n");
        devices.shift();
        devices = devices.map((device) => device.split(/\s+/)[0]);
        return devices;
    }
    constructor(device) {
        if (device === undefined)
            throw new Error("device serial omitted.");
        this._device = device;
    }
    get_serial() {
        return this._device;
    }
    async command(cmd, args, options) {
        if (args === void 0)
            args = [];
        return await pSpawn("adb", ["-s", this._device, cmd, ...args], options);
    }
    async shell(cmd, args, options) {
        if (args === void 0)
            args = [];
        return await pSpawn("adb", ["-s", this._device, "shell", cmd, ...args], options);
    }
    async forward(params, options) {
        if (params === void 0)
            params = {};
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.forward.timeout;
        let args = [];
        if (params.remove_all)
            args.push("--remove-all");
        else
            args.push(params.local, params.remote);
        await this.command("forward", args, { timeout });
    }
    async ps(params, options) {
        if (params === void 0)
            params = {};
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.ps.timeout;
        let args = [];
        if (params.pid)
            args.push(params.pid);
        let stdout = await this.shell("ps", args, { timeout });
        let procs = stdout.trim().split('\n');
        let header = procs.shift();
        procs = procs
            .map((proc) => {
            return proc.split(/\s+/).filter((s) => s !== "");
        })
            .map((proc) => ({
            "pid": parseInt(proc[1]),
            "name": proc[8]
        }));
        return procs;
    }
    async dumpsys(params, options) {
        if (params === void 0)
            params = {};
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.dumpsys.timeout;
        const { service } = params;
        let args = [];
        if (params.service)
            args.push(params.service);
        if (params.args)
            args = args.concat(params.args);
        const s = await this.shell("dumpsys", args, { timeout });
        const ret = {};
        if (service === "window") {
            if (params.args.length === 1 && params.args[0] === "windows") {
                const rgx = /^\s+mCurrentFocus\=Window\{\S+ \S+ (\S+)\}$/m;
                const matched = s.match(rgx);
                if (matched !== null)
                    ret.mCurrentFocus = matched[1];
            }
        }
        return ret;
    }
    async screenshot(params, options) {
        if (params === void 0 || params.path === void 0)
            throw new Error("params.path cannot be omitted in screenshot().");
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.screenshot.timeout;
        let args = ["-p"];
        args.push(params.path);
        await this.shell("screencap", args, { timeout });
    }
    async pull(params, options) {
        if (params === void 0)
            throw new Error("params undefined");
        if (params.remote_path === void 0 || params.local_path === void 0)
            throw new Error("params.local_path and params.remote_path cannot be omitted in pull().");
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.pull.timeout;
        let args = [];
        args.push(params.remote_path);
        args.push(params.local_path);
        await this.command("pull", args, { timeout });
    }
    async unix_domain_sockets(options) {
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.cat.timeout;
        let msg = await this.shell("cat", ["/proc/net/unix"], { timeout });
        let lines = msg.trim().split("\n");
        let headers = lines.shift();
        let sckts = lines.map((line) => {
            let frags = line.split(/\s+/).filter((s) => s !== "");
            return {
                "Path": frags[7]
            };
        });
        return sckts;
    }
    async input(params, options) {
        if (params === void 0)
            throw new Error("No params provided.");
        if (options === void 0)
            options = {};
        let { command } = params;
        if (command === void 0)
            throw new Error("command must be provided.");
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.input.timeout;
        if (command === "tap") {
            if (params.location.x === void 0 || params.location.y === void 0)
                throw new Error("location must be provided.");
            await this.shell("input", ["touchscreen", command, params.location.x, params.location.y], { timeout });
        }
        else if (command === "swipe") {
            if (params.from.x === void 0 || params.from.y === void 0 || params.to.x === void 0 || params.to.y === void 0)
                throw new Error("from and to must be provided.");
            await this.shell("input", ["touchscreen", command, params.from.x, params.from.y, params.to.x, params.to.y, params.duration], { timeout });
        }
        else
            throw new Error("unidentified command.");
        return;
    }
    async wm(params, options) {
        if (params === void 0)
            throw new Error("params must be provided.");
        if (options === void 0)
            options = {};
        let args = [];
        let { subcommand } = params;
        if (subcommand === void 0)
            throw new Error("No subcommand provided.");
        args.push(subcommand);
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.wm.timeout;
        let s = await this.shell("wm", args, { timeout });
        let ret = s;
        if (subcommand === "size") {
            let matched = s.match(/^Physical\s*size:\s*(\d+)x(\d+)$/m);
            if (matched === null || matched.length !== 3)
                throw new Error("\"adb shell wm size\" returns data in wrong format.");
            let width = parseInt(matched[1]);
            let height = parseInt(matched[2]);
            ret = { width, height };
        }
        else {
        }
        return ret;
    }
    async getevent(params, options) {
        if (params === void 0)
            params = {};
        if (options === void 0)
            options = {};
        let args = [];
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.getevent.timeout;
        if (params.plain_text)
            args.push("-l");
        if (params.timestamps)
            args.push("-t");
        if (params.possible_events)
            args.push("-p");
        let s = await this.shell("getevent", args, { timeout });
        let ret = s;
        if (params.possible_events) {
            ret = [];
            let lines = s.split('\n');
            for (let i = 0, j; i < lines.length; i = j) {
                for (j = i + 1; j < lines.length; ++j)
                    if (lines[j].startsWith("add device"))
                        break;
                let k = i;
                let device_line = lines[k];
                let name_line = lines[++k];
                let matched = name_line.match(/^\s*name:\s*\"(\w+)\"$/m);
                if (matched === null || matched.length !== 2)
                    continue;
                let name = matched[1];
                matched = device_line.match(/^add device \d*:\s([\w\/]*)\s*$/m);
                if (matched === null || matched.length !== 2)
                    continue;
                let device = matched[1];
                let events = [];
                ++k;
                for (++k; k < j; ++k) {
                    let event = lines[k];
                    let pattern = "(\\w+)\\s*:"
                        + "\\svalue\\s(\\d+)"
                        + ",\\smin\\s(\\d+)"
                        + ",\\smax\\s(\\d+)"
                        + ",\\sfuzz\\s(\\d+)"
                        + ",\\sflat\\s(\\d+)"
                        + ",\\sresolution\\s(\\d+)"
                        + "\\s*"
                        + "$";
                    matched = event.match(new RegExp(pattern, "m"));
                    if (matched === null)
                        break;
                    let name = params.plain_text ? matched[1] : parseInt(matched[1], 16);
                    let min = parseInt(matched[3]);
                    let max = parseInt(matched[4]);
                    events.push({ name, min, max });
                }
                ret.push({ device, name, events });
            }
        }
        else {
        }
        return ret;
    }
    async monkey(params, options) {
        if (params === void 0)
            params = {};
        if (options === void 0)
            options = {};
        let args = [];
        if (params.allowed_package !== void 0)
            args.push("-p", params.allowed_package);
        if (params.main_category !== void 0)
            args.push("-c", params.main_category);
        if (params.count !== void 0)
            args.push(params.count);
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.monkey.timeout;
        await this.shell("monkey", args, { timeout });
    }
    async am(params, options) {
        if (params === void 0)
            throw new Error("params must be provided.");
        if (options === void 0)
            options = {};
        let args = [];
        let { subcommand } = parmas;
        if (subcommand === void 0)
            throw new Error("subcommand must be provided.");
        args.push(subcommand);
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.am.timeout;
        if (subcommand === "force-stop") {
            if (params.package === void 0)
                throw new Error("package must be provided for force-stop subcommand.");
            args.push(params.package);
            await this.shell("am", args, { timeout });
        }
        else {
        }
    }
    async logcat(params, options) {
        if (params === void 0)
            params = {};
        const args = [];
        if (params.clear)
            args.push("--clear");
        if (options === void 0)
            options = {};
        let { timeout } = options;
        if (timeout === void 0)
            timeout = DEFAULT.logcat.timeout;
        return await this.command("logcat", args, { timeout });
    }
    ia_command(cmd, args) {
        args = args || [];
        return iaSpawn("adb", ["-s", this._device, cmd, ...args]);
    }
    ia_shell(cmd, args) {
        args = args || [];
        return iaSpawn("adb", ["-s", this._device, "shell", cmd, ...args]);
    }
    ia_getevent(params) {
        if (params === void 0)
            params = {};
        let args = [];
        if (params.plain_text)
            args.push("-l");
        if (params.timestamp)
            args.push("-t");
        let c = this.ia_shell("getevent", args);
        let out = "";
        let pcd = new EventEmitter();
        const pattern = "^"
            + "\\[\\s*([\\d\\.]+)\\]"
            + "\\s*([\\w\\/]+):"
            + "\\s*(\\w+)"
            + "\\s*(\\w+)"
            + "\\s*(\\w+)"
            + "\\s*"
            + "$";
        const regex = new RegExp(pattern, "m");
        c.stdout.on("data", (chunk) => {
            out += chunk;
            let lines = out.split('\n');
            out = lines.pop();
            for (let line of lines)
                if (line.startsWith("[")) {
                    let matched = line.match(regex);
                    if (matched === null)
                        continue;
                    let time;
                    let value;
                    time = parseFloat(matched[1]);
                    value = parseInt(matched[5], 16);
                    let event = {
                        time: time,
                        device: matched[2],
                        type: matched[4],
                        value: value
                    };
                    pcd.emit("event", event);
                }
        });
        c.once("close", (code, signal) => {
            pcd.emit("close");
        });
        pcd.once("stop", () => {
            c.kill("SIGINT");
        });
        return pcd;
    }
    ia_logcat(params) {
        if (params === void 0)
            params = {};
        let args = [];
        if (params.format)
            args.push("--format", params.format);
        if (params.filterspecs)
            args = args.concat(params.filterspecs);
        let c = this.ia_shell("logcat", args);
        let pcd = new EventEmitter();
        let data = "";
        if (params.format === "raw") {
            c.stdout.on("data", (chunk) => {
                data += chunk;
                let lines = data.split("\n");
                data = lines.pop();
                for (let line of lines) {
                    let log = { message: line };
                    pcd.emit("log", log);
                }
            });
        }
        else {
            c.stdout.on("data", (chunk) => {
                data += chunk;
                let lines = data.split("\n");
                data = lines.pop();
                for (let log of lines)
                    pcd.emit("log", log);
            });
        }
        c.once("close", (code, signal) => {
            pcd.emit("close");
        });
        pcd.once("stop", () => {
            c.kill("SIGINT");
        });
        return pcd;
    }
    ia_od(params, options) {
        if (params === void 0)
            throw new Error("params must be provided.");
        let { file } = params;
        if (file === void 0)
            throw new Error("file must be specified.");
        let args = [];
        if (params.type !== void 0) {
            let type = params.type;
            if (type.alias !== void 0)
                args.push("-" + type.alias);
            else if (type.type !== void 0 && type.size !== void 0) {
                args.push("-t");
                args.push(type.type + type.size);
            }
        }
        args.push(file);
        let c = this.ia_shell("od", args);
        let pcd = new EventEmitter();
        c.stdout.on("data", (chunk) => {
            pcd.emit("data", new String(chunk));
        });
        c.once("close", (code, signal) => {
            pcd.emit("close");
        });
        pcd.once("stop", () => {
            c.kill("SIGINT");
        });
        return pcd;
    }
    ia_screenrecord(params, options) {
        if (params === void 0)
            throw new Error("params must be provided.");
        const { size, timeLimit, bitRate, file } = params;
        const args = [];
        if (size !== void 0) {
            const { width, height } = size;
            args.push("--size", `${width}x${height}`);
        }
        if (timeLimit !== void 0)
            args.push("--time-limit", timeLimit);
        if (bitRate !== void 0)
            args.push("--bit-rate", bitRate);
        if (file === void 0)
            throw new Error("No destination file specified.");
        args.push(file);
        const c = this.ia_shell("screenrecord", args);
        const pcd = new EventEmitter();
        c.once("close", (code, signal) => {
            pcd.emit("close");
        });
        pcd.once("stop", () => {
            c.kill("SIGINT");
        });
        return pcd;
    }
}
module.exports = adb;
