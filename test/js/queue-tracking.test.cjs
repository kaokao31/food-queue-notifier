const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2] || path.join(__dirname,'../..')),ctx=vm.createContext({setTimeout,clearTimeout});
vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets/queue-ui.js'),'utf8'),ctx);
async function run(){
 const {createTracker,renderOrder}=ctx.QueueUI;let next,calls=0,rendered=0,errors=[];
 const tracker=createTracker({id:'42',getToken:()=> 'secret',request:async(url,opts)=>{assert.equal(url,'/api/v1/orders/42');assert.equal(opts.headers['X-Queue-Token'],'secret');calls++;if(calls===2)throw Error('offline');return {queue:{status:calls===3?'COMPLETED':'WAITING'}};},render:()=>rendered++,onError:e=>errors.push(e),schedule:(fn,ms)=>{assert.equal(ms,5000);next=fn;return 1;},cancel:()=>{}});
 await tracker.load();assert.equal(rendered,1);await next();assert.equal(errors.at(-1),'offline');const retry=next;next=null;await retry();assert.equal(rendered,2);assert.equal(next,null);
 let release,lateRender=false,scheduled=false;
 const late=createTracker({id:1,getToken:()=> 'token',request:()=>new Promise(resolve=>release=resolve),render:()=>lateRender=true,onError:()=>{},schedule:()=>scheduled=true,cancel:()=>{}});
 const pending=late.load();late.stop();release({queue:{status:'WAITING'}});await pending;assert.equal(lateRender,false);assert.equal(scheduled,false);
 for(const id of ['bad',1]){const denied=createTracker({id,getToken:()=>null,request:()=>assert.fail('must not request'),render:()=>{},onError:e=>assert.ok(e),schedule:()=>{},cancel:()=>{}});await denied.load();denied.stop();}
 const elements={number:{},status:{},date:{},help:{},items:{},total:{},steps:[]};
 renderOrder({queue:{queueNumber:1000,status:'READY'},items:[{menuItemName:'<rice>',quantity:2,subtotal:10}],totalAmount:10},{elements,escapeHtml:s=>String(s).replaceAll('<','&lt;').replaceAll('>','&gt;'),money:s=>String(s),labels:{READY:'ready'}});
 assert.equal(elements.number.textContent,'1000');assert.equal(elements.status.textContent,'ready');assert.match(elements.items.innerHTML,/&lt;rice&gt;/);assert.equal(elements.date.textContent,'');
 console.log('PASS: queue per-bill token, polling/retry, terminal stop, late response cancellation, absent token and escaped rendering.');
}
run().catch(e=>{console.error(e);process.exitCode=1;});
