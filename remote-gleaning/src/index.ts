
import * as express from "express";
import * as yargs from "yargs";

import {query} from "./query";

function serve(options: {address: string, port: number}): void{
	const app = express();

	app.listen(options.port, options.address);
	app.get("/getDOM", async (req: any, res: any): Promise<void> => {
		let dom: string = "";
		try {
			dom = await query.getDOM();
			res.send(dom);
		} catch (e){
			res.status(500);
			res.send("Server Internal Error");
		}
	});

	app.get("/getLocation", async (req: any, res: any): Promise<void> => {
		const xpath: string = req.query.xpath;
		let rect: any;
		try {
			rect = await query.getLocation(xpath);
			const ret: string = JSON.stringify(rect);
			res.send(ret);
		} catch (e){
			res.status(500);
			res.send("Server Internal Error");
		}
	});

	app.get("/getAllLeavesLocations", async (req: any, res:any): Promise<void> => {
		try {
			const result: {} = await query.getAllLeavesLocations();
			const ret = JSON.stringify(result);
			res.send(ret);
		} catch (e){
			res.status(500);
			res.send("Server Internal Error");
		}
	});
}

function main(): void{
	const argv: any = yargs
		.number("port").alias("p", "port").default("p", 9234)
		.string("address").alias("a", "address").default("a", "localhost")
		.argv;

	const options: {address: string, port: number} = {
		address: argv.address,
		port: argv.port
	};
	serve(options);
}

main();
