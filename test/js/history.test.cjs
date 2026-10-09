const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2] || path.join(__dirname,'../..'));
const context=vm.createContext({document:{querySelector:()=>null},setTimeout:()=>{}});
for(const name of ['core-ui.js','history-ui.js'])vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets',name),'utf8'),context);
function storage(values){const keys=Object.keys(values);return {length:keys.length,key:i=>keys[i],getItem:key=>values[key] ?? null};}
function elements(){const handlers={};return {open:{},close:{},prev:{},next:{},page:{},list:{},dialog:{showModal(){},close(){handlers.close();},addEventListener:(name,fn)=>{handlers[name]=fn;}}};}
async function run(){
 const {HistoryUI,CoreUI}=context;
 assert.deepEqual(Array.from(HistoryUI.storedOrderIds(storage({'last-order':'2','queue-token-01':'bad','queue-token-2':'two','queue-token-10':'ten','queue-token-3':'','queue-token-9007199254740993':'large'}))),['9007199254740993','10','2']);
 const values=Object.fromEntries(Array.from({length:7},(_,i)=>['queue-token-'+(i+1),'token-'+(i+1)])),e=elements(),calls=[];
 HistoryUI.createHistory({elements:e,getStorage:()=>storage(values),escapeHtml:CoreUI.escapeHtml,money:CoreUI.money,labels:CoreUI.labels,request:async(url,options)=>{
  const id=url.split('/').at(-1);calls.push(id);assert.equal(options.headers['X-Queue-Token'],'token-'+id);
  if(id==='6')throw Error('<blocked>');
  return {queue:{queueNumber:Number(id),status:'COMPLETED'},items:[{menuItemName:'<rice>',quantity:1}],totalAmount:25,createdAt:'2026-10-09T00:00:00Z'};
 }});
 await e.open.onclick();assert.equal(calls.length,5);assert.equal(e.prev.disabled,true);assert.equal(e.next.disabled,false);assert.match(e.list.innerHTML,/&lt;rice&gt;/);assert.match(e.list.innerHTML,/&lt;blocked&gt;/);assert.doesNotMatch(e.list.innerHTML,/token-/);assert.match(e.list.innerHTML,/รับอาหารเรียบร้อย/);
 await e.next.onclick();assert.deepEqual(calls.slice(-2),['2','1']);assert.equal(e.next.disabled,true);assert.equal(e.prev.disabled,false);
 await e.prev.onclick();assert.match(e.page.textContent,/1 \/ 2/);
 const empty=elements();HistoryUI.createHistory({elements:empty,getStorage:()=>storage({}),escapeHtml:CoreUI.escapeHtml,money:CoreUI.money,labels:CoreUI.labels,request:()=>assert.fail('empty history must not request')});await empty.open.onclick();assert.match(empty.list.innerHTML,/ยังไม่มี/);
 const denied=elements();HistoryUI.createHistory({elements:denied,getStorage:()=>{throw Error('denied');},escapeHtml:CoreUI.escapeHtml,money:CoreUI.money,labels:CoreUI.labels,request:()=>assert.fail('blocked storage must not request')});await denied.open.onclick();assert.match(denied.list.textContent,/ไม่อนุญาต/);
 let finish;const late=elements();HistoryUI.createHistory({elements:late,getStorage:()=>storage({'queue-token-1':'one'}),escapeHtml:CoreUI.escapeHtml,money:CoreUI.money,labels:CoreUI.labels,request:()=>new Promise(resolve=>{finish=resolve;})});const pending=late.open.onclick();late.close.onclick();finish({queue:{queueNumber:1,status:'WAITING'},items:[],totalAmount:0});await pending;assert.match(late.list.innerHTML,/กำลังโหลด/);
 console.log('PASS: history IDs/order, per-bill token, paging, escaped content, partial errors, empty/blocked storage and close cancellation.');
}
run().catch(error=>{console.error(error);process.exitCode=1;});
