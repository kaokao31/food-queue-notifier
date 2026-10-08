const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(process.argv[2] || path.join(__dirname, '../..'));
const context = vm.createContext({document:{querySelector:()=>null},setTimeout:()=>{}});
for (const name of ['core-ui.js','menu-ui.js']) vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets',name),'utf8'),context);
const {CoreUI,MenuUI}=context;
function elements() {
 const categories={html:'',buttons:[],get innerHTML(){return this.html;},set innerHTML(value){this.html=value;this.buttons=[...value.matchAll(/<button[^>]*>(.*?)<\/button>/g)].map(m=>({textContent:m[1]}));},querySelectorAll(){return this.buttons;}};
 return {categories,grid:{},sort:{value:'name,asc'},prev:{},next:{},pageInfo:{}};
}
async function run() {
 const e=elements(),requests=[];
 const fixtures=[{id:1,name:'<unsafe>',category:'FOOD',price:30},{id:2,name:'Tea',category:'DRINK',price:20}];
 let failure=false;
 const browser=MenuUI.createMenuBrowser({elements:e,escapeHtml:CoreUI.escapeHtml,renderer:MenuUI.createMenuRenderer(CoreUI),request:async url=>{
  requests.push(url);if(failure)throw Error('API unavailable');
  const params=new URL(url,'http://test').searchParams;
  return {content:fixtures,page:Number(params.get('page')),totalPages:2};
 }});
 await browser.load();
 assert.match(e.grid.innerHTML,/&lt;unsafe&gt;/);
 assert.match(e.grid.innerHTML,/ disabled/);
 assert.equal(e.prev.disabled,true);assert.equal(e.next.disabled,false);
 assert.equal(e.categories.buttons.length,3);
 e.categories.buttons[2].onclick();assert.match(e.grid.innerHTML,/Tea/);assert.doesNotMatch(e.grid.innerHTML,/&lt;unsafe&gt;/);
 e.next.onclick();await new Promise(setImmediate);assert.match(requests.at(-1),/page=1/);assert.equal(e.next.disabled,true);
 e.sort.value='price,desc';e.sort.onchange();await new Promise(setImmediate);
 assert.match(requests.at(-1),/page=0&sort=price%2Cdesc/);
 const menus=browser.getMenus();menus.pop();assert.equal(browser.getMenus().length,2);
 failure=true;await browser.load();assert.equal(e.grid.textContent,'API unavailable');assert.equal(e.next.disabled,true);assert.equal(e.categories.innerHTML,'');
 let resolveOld;
 const stale=MenuUI.createMenuBrowser({elements:elements(),escapeHtml:CoreUI.escapeHtml,renderer:MenuUI.createMenuRenderer(CoreUI),request:()=>new Promise(resolve=>{resolveOld=resolve;})});
 const pending=stale.load();const first=resolveOld;const newer=stale.load();resolveOld({content:[],page:0,totalPages:0});await newer;first({content:fixtures,page:0,totalPages:1});await pending;
 assert.equal(stale.getMenus().length,0);
 let fetchCalls=0;context.fetch=async()=>{fetchCalls++;return {status:404,ok:false};};
 await assert.rejects(CoreUI.api('/api/v1/menu-items'),/ยังไม่พร้อม/);
 await assert.rejects(CoreUI.api('/api/v1/orders',{method:'POST'}),/ยังไม่เปิด/);assert.equal(fetchCalls,1);
 const app=fs.readFileSync(path.join(root,'code/src/main/resources/static/assets/app.js'),'utf8');
 assert.doesNotMatch(app,/localStorage|PushManager|serviceWorker|checkout|QueueToken/);
 console.log('PASS: menu loading, escaped names, disabled ordering, categories, paging, sorting, errors, stale-response handling and read-only API.');
}
run().catch(error=>{console.error(error);process.exitCode=1;});
