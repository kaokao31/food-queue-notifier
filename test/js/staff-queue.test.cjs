const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||path.join(__dirname,'../..')),ctx=vm.createContext({});
vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets/staff-queue-ui.js'),'utf8'),ctx);
const node=()=>({disabled:true,textContent:'',innerHTML:'',addEventListener(name,fn){this[name]=fn;},removeEventListener(){},querySelectorAll(){return [];},contains(){return true;},showModal(){this.open=true;},close(){this.open=false;}});
const make=()=>Object.fromEntries(['refresh','filters','list','prev','next','page','error','dialog','detail','close'].map(key=>[key,node()]));
const order={id:7,queue:{status:'WAITING',queueNumber:1000},items:[{menuItemName:'<script>',quantity:2,subtotal:20}],totalAmount:20};
const format={escapeHtml:value=>String(value??'').replaceAll('<','&lt;').replaceAll('>','&gt;'),money:String,labels:{WAITING:'waiting'}};
async function run(){
 const card=ctx.StaffQueueUI.card(order,format);assert.match(card,/&lt;script&gt;/);assert.match(card,/1000/);assert.match(card,/data-action="advance"/);
 for(const state of ['COMPLETED','CANCELLED'])assert.doesNotMatch(ctx.StaffQueueUI.card({...order,queue:{status:state}},format),/data-action="(?:advance|cancel|delete)"/);
 let requests=[],release;const elements=make();
 const ui=ctx.StaffQueueUI.createStaffQueue({...format,elements,confirmAction:()=>true,request:async(url,options)=>{requests.push([url,options]);if(options)return new Promise(resolve=>release=resolve);if(url==='/api/v1/orders/7')return order;return {content:[order],totalPages:2};}});
 await ui.load();assert.equal(elements.refresh.disabled,false);assert.equal(elements.prev.disabled,true);assert.equal(elements.next.disabled,false);
 const pending=ui.action('advance',7);await ui.action('advance',7);assert.equal(requests.length,2);assert.equal(elements.refresh.disabled,true);assert.equal(requests[1][0],'/api/v1/queues/7/advance');assert.equal(requests[1][1].method,'PATCH');release(null);await pending;assert.equal(requests.length,3);
 await ui.action('detail',7);assert.equal(elements.dialog.open,true);assert.equal(requests.at(-1)[0],'/api/v1/orders/7');
 // Verify cancellation/deletion contracts and confirmation before sending a mutation.
 let calls=[],allow=false;const destructive=ctx.StaffQueueUI.createStaffQueue({...format,elements:make(),confirmAction:()=>allow,request:async(url,options)=>{calls.push([url,options]);return options?null:{content:[order],totalPages:1};}});await destructive.load();await destructive.action('delete',7);assert.equal(calls.length,1);allow=true;await destructive.action('cancel',7);assert.equal(calls[1][0],'/api/v1/queues/7/cancel');assert.equal(calls[1][1].method,'PATCH');await destructive.action('delete',7);assert.equal(calls[3][0],'/api/v1/orders/7');assert.equal(calls[3][1].method,'DELETE');await destructive.action('advance','bad');assert.equal(calls.length,5);destructive.stop();
 // Reject unavailable APIs without a production fallback.
 ui.stop();const denied=make();const broken=ctx.StaffQueueUI.createStaffQueue({...format,elements:denied,request:async()=>{throw Error('staff access unavailable');}});await broken.load();assert.equal(denied.error.textContent,'staff access unavailable');assert.equal(denied.list.innerHTML,'');broken.stop();
 let resolve;const late=make();const stopped=ctx.StaffQueueUI.createStaffQueue({...format,elements:late,request:()=>new Promise(r=>resolve=r)});const loading=stopped.load();stopped.stop();resolve({content:[order],totalPages:1});await loading;assert.equal(late.list.innerHTML,'');
 const filters=make();let urls=[];const filtered=ctx.StaffQueueUI.createStaffQueue({...format,elements:filters,request:async url=>{urls.push(url);return {content:[],totalPages:0};}});await filtered.load();filters.filters.click({target:{closest:()=>({dataset:{status:'READY'}})}});await new Promise(r=>setImmediate(r));assert.match(urls.at(-1),/page=0&size=12&queueStatus=READY/);filtered.stop();
 console.log('PASS: staff queue escaping, state-specific buttons, filtering, duplicate mutation guard, detail, missing access and disposed response.');
}
run().catch(error=>{console.error(error);process.exitCode=1;});
