
const Phone: any = require("./platform/Phone.js");
const WebPage: any = require("./platform/WebPage.js");

import * as jsdom from "jsdom";
const {JSDOM} = jsdom;

interface DOMRect{
	top?: number,
	bottom?: number,
	left?: number,
	right?: number,
	width?: number,
	height?: number
}

export class query{
	private static async getWebpage(): Promise<any>{
		const phone: any = await Phone.device();
		const debuggerUrl: string = await phone.get_active_debugger_url();
		const webpage: any = new WebPage(debuggerUrl);
		return webpage;
	}

	public static async getDOM(): Promise<string>{
		const webpage: any = await query.getWebpage();
		await webpage.connect();
		const dom: string = await webpage.get_rendered_HTML();
		webpage.close();
		return dom;
	}

	public static async getLocation(xpath: string): Promise<DOMRect>{
		const webpage: any = await query.getWebpage();
		await webpage.connect();
		let code: string = `document.evaluate(\"${xpath}\", document, null, XPathResult.ANY_TYPE, null)`;
		code = code + ".iterateNext()";
		code = code + ".getBoundingClientRect()";
		const rect: DOMRect = await webpage.evaluate(code);
		webpage.close();
		return rect;
	}

	public static async getAllLeavesLocations(): Promise<{}>{
		const webpage: any = await query.getWebpage();
		await webpage.connect();
		const domString: string = await webpage.get_rendered_HTML();

		const dom = new JSDOM(domString);
		const document = dom.window.document;

		type dict = {[index: string]: any};
		async function getLocation(xpath: string): Promise<DOMRect>{
			let code: string = `document.evaluate(\"${xpath}\", document, null, XPathResult.ANY_TYPE, null)`;
			code = code + ".iterateNext()";
			code = code + ".getBoundingClientRect()";
			const rect: DOMRect = await webpage.evaluate(code);
			return rect;
		}

		async function getDevicePixelRatio(): Promise<number>{
			const code = `window.devicePixelRatio`;
			const r = parseFloat(await webpage.evaluate(code));
			return r;
		}

		function findLeafNodes(e: HTMLElement, xpath: string, clickable: boolean): Promise<dict>[]{
			if (e.firstElementChild == null){
				if (!e.hasChildNodes()) return [];
				const name = e.localName;
				if (name === "script" || name == "style" || name === "link") return [];
				const p = getLocation(xpath)
					.then((r: DOMRect): dict => {
						const ret: dict = {};
						ret.tag = name;
						ret.clickable = clickable;
						ret.innerHTML = e.innerHTML;
						ret.location = r;
						ret.xpath = xpath;
						return ret;
					});
				return [p];
			} else {
				let ret: Promise<dict>[] = [];
				let tagCount: dict = {};
				for (let c: HTMLElement|null = <HTMLElement|null>e.firstElementChild; c != null; c = <HTMLElement|null>c.nextElementSibling){
					const name = c.localName;
					if (tagCount[name] == null) tagCount[name] = 1; else ++tagCount[name];
					ret = ret.concat(findLeafNodes(c, `${xpath}/${name}[${tagCount[name]}]`, clickable || (name === "a" || name === "li")));
				}
				return ret;
			}
		}

		const leaves = await Promise.all(findLeafNodes(document.body, "//html/body", false));
		const ratio = await getDevicePixelRatio();
		const result = {
			"devicePixelRatio": ratio,
			"leaves": leaves
		}

		webpage.close();
		return result;
	}
}
