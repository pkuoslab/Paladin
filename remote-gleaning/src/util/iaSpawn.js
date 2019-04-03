"use strict";

const child_process = require("child_process");
const spawn = child_process.spawn;

let iaSpawn = function (command, args){
	if (command === void 0)
		throw new Error("command omitted.");

	return spawn(command, args);
}

module.exports = iaSpawn;
