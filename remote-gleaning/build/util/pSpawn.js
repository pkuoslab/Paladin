"use strict";
const child_process = require("child_process");
const spawn = child_process.spawn;
let pSpawn = function (command, args, options) {
    if (!command)
        return Promise.reject(new Error("command omitted."));
    options = options || {};
    return new Promise((resolve, reject) => {
        let c = spawn(command, args);
        let stdout = "", stderr = "";
        c.stdout.on("data", (chunk) => stdout += chunk);
        c.stderr.on("data", (chunk) => stderr += chunk);
        c.on("error", reject);
        c.on("exit", (code, signal) => {
            if (code !== undefined && code != 0)
                reject(new Error(`program \"${command} ${args.join(" ")}\" exited abnormally, with return code ${code} and stderr ${stderr}`));
            else
                resolve(stdout);
        });
        let timeout = options.timeout;
        if (timeout) {
            setTimeout(() => {
                reject(new Error(`command ${command} ${args} time out.`));
            }, timeout);
        }
    });
};
module.exports = pSpawn;
