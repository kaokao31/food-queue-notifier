const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../..'),fixture=JSON.parse(fs.readFileSync(path.join(root,'test/fixtures/ui-api.json'),'utf8'));
const ctx=vm.createContext({});for(const name of ['basket-ui.js','checkout-ui.js','history-ui.js','queue-ui.js'])vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets',name),'utf8'),ctx);
async function run(){
 const model=ctx.BasketUI.createBasketModel();model.add(fixture.menu,2);const saved=new Map(),paths=[],calls=[];
 const checkout=ctx.CheckoutUI.createCheckout({basket:{isValid:()=>true,getItems:model.getItems},button:{},error:{},saveToken:(id,token)=>saved.set('queue-token-'+id,token),navigate:url=>paths.push(url),request:async(url,options)=>{calls.push(url);assert.deepEqual(JSON.parse(options.body),{items:[{menuItemId:7,quantity:2}]});return fixture.order;}});
 assert.equal(await checkout.submit(),true);assert.deepEqual(paths,['/queue/42']);assert.equal(await checkout.submit(),false);assert.equal(calls.length,1);
 const storage={get length(){return saved.size;},key:i=>[...saved.keys()][i],getItem:key=>saved.get(key)};assert.equal(ctx.HistoryUI.storedOrderIds(storage).join(','),'42');
 let scheduled=0,seen=[];let terminal=false;const tracker=ctx.QueueUI.createTracker({id:42,getToken:()=>storage.getItem('queue-token-42'),request:async(url,options)=>{assert.equal(url,'/api/v1/orders/42');assert.equal(options.headers['X-Queue-Token'],fixture.order.queueToken);const {queueToken,...order}=fixture.order;return {...order,queue:{...order.queue,status:terminal?'COMPLETED':'WAITING'}};},render:order=>seen.push(order.queue.status),onError:error=>assert.equal(error,''),schedule:()=>++scheduled,cancel:()=>{}});
 await tracker.load();assert.equal(scheduled,1);terminal=true;await tracker.load();assert.deepEqual(seen,['WAITING','COMPLETED']);assert.equal(scheduled,1);tracker.stop();
 saved.clear();let unauthorizedCalls=0,message='';const missing=ctx.QueueUI.createTracker({id:42,getToken:()=>storage.getItem('queue-token-42'),request:async()=>unauthorizedCalls++,render:()=>assert.fail(),onError:error=>message=error,schedule:()=>0,cancel:()=>{}});await missing.load();assert.equal(unauthorizedCalls,0);assert.ok(message);missing.stop();
 console.log('PASS: basket to checkout, creation token storage, history lookup, authenticated polling, terminal stop and missing token; test fixtures only.');
}run().catch(error=>{console.error(error);process.exitCode=1;});
