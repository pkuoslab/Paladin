"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express = require("express");
const yargs = require("yargs");
const query_1 = require("./query");
function serve(options) {
    const app = express();
    app.listen(options.port, options.address);
    app.get("/getDOM", async (req, res) => {
        let dom = "";
        try {
            dom = await query_1.query.getDOM();
            res.send(dom);
        }
        catch (e) {
            res.status(500);
            res.send("Server Internal Error");
        }
    });
    app.get("/getLocation", async (req, res) => {
        const xpath = req.query.xpath;
        let rect;
        try {
            rect = await query_1.query.getLocation(xpath);
            const ret = JSON.stringify(rect);
            res.send(ret);
        }
        catch (e) {
            res.status(500);
            res.send("Server Internal Error");
        }
    });
    app.get("/getAllLeavesLocations", async (req, res) => {
        try {
            const result = await query_1.query.getAllLeavesLocations();
            const ret = JSON.stringify(result);
            res.send(ret);
        }
        catch (e) {
            res.status(500);
            res.send("Server Internal Error");
        }
    });
}
function main() {
    const argv = yargs
        .number("port").alias("p", "port").default("p", 9234)
        .string("address").alias("a", "address").default("a", "localhost")
        .argv;
    const options = {
        address: argv.address,
        port: argv.port
    };
    serve(options);
}
main();
