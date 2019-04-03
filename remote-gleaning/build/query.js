"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Phone = require("./platform/Phone.js");
const WebPage = require("./platform/WebPage.js");
const jsdom = require("jsdom");
const { JSDOM } = jsdom;
class query {
    static async getWebpage() {
        const phone = await Phone.device();
        const debuggerUrl = await phone.get_active_debugger_url();
        const webpage = new WebPage(debuggerUrl);
        return webpage;
    }
    static async getDOM() {
        const webpage = await query.getWebpage();
        await webpage.connect();
        const dom = await webpage.get_rendered_HTML();
        webpage.close();
        return dom;
    }
    static async getLocation(xpath) {
        const webpage = await query.getWebpage();
        await webpage.connect();
        //let code = `document.evaluate(\"${xpath}\", document, null, XPathResult.ANY_TYPE, null)`;
        //code = code + ".iterateNext()";
        //code = code + ".getBoundingClientRect()";
        //const rect = await webpage.evaluate(code);
        let code = `(function (){
            var r = document.evaluate(\"${xpath}\", document, null, XPathResult.ANY_TYPE, null)
            .iterateNext()
            .getBoundingClientRect();
            var ret = {}
            ret.x = r.x;
            ret.y = r.y;
            ret.top = r.top;
            ret.down = r.down;
            ret.left = r.left;
            ret.right = r.right;
            ret.width = r.width;
            ret.height = r.height;
            return ret;
        })()`;
        let rect = await webpage.evaluate(code);
        webpage.close();
        return rect;
    }
    static async getAllLeavesLocations() {
        const webpage = await query.getWebpage();
        await webpage.connect();
        const domString = await webpage.get_rendered_HTML();
        const dom = new JSDOM(domString);
        const document = dom.window.document;
        async function getLocation(xpath) {
            //let code = `document.evaluate(\"${xpath}\", document, null, XPathResult.ANY_TYPE, null)`;
            //code = code + ".iterateNext()";
            //code = code + ".getBoundingClientRect()";
            //const rect = await webpage.evaluate(code);
            let code = `(function (){
                var r = document.evaluate(\"${xpath}\", document, null, XPathResult.ANY_TYPE, null)
                .iterateNext()
                .getBoundingClientRect();

                var ret = {};
                ret.x = r.x;
                ret.y = r.y;
                ret.top = r.top;
                ret.bottom = r.bottom;
                ret.left = r.left;
                ret.right = r.right;
                ret.width = r.width;
                ret.height = r.height;
                return ret;
            })()`;
            let rect = await webpage.evaluate(code);
            return rect;
        }
        async function getDevicePixelRatio() {
            const code = `window.devicePixelRatio`;
            const r = parseFloat(await webpage.evaluate(code));
            return r;
        }
        function findLeafNodes(e, xpath, clickable) {
            if (e.firstElementChild == null) {
                if (!e.hasChildNodes())
                    return [];
                const name = e.localName;
                if (name === "script" || name == "style" || name === "link")
                    return [];
                const p = getLocation(xpath)
                    .then((r) => {
                    const ret = {};
                    ret.tag = name;
                    ret.clickable = clickable;
                    ret.innerHTML = e.innerHTML;
                    ret.location = r;
                    ret.xpath = xpath;
                    return ret;
                });
                return [p];
            }
            else {
                let ret = [];
                let tagCount = {};
                for (let c = e.firstElementChild; c != null; c = c.nextElementSibling) {
                    const name = c.localName;
                    if (tagCount[name] == null)
                        tagCount[name] = 1;
                    else
                        ++tagCount[name];
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
        };
        webpage.close();
        return result;
    }
}
exports.query = query;
