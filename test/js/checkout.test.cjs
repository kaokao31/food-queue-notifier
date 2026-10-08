const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(process.argv[2] || path.join(__dirname, '../..'));
const context = vm.createContext({document:{querySelector:()=>null},setTimeout:()=>{}});
for(const name of ['core-ui.js','basket-ui.js','checkout-ui.js'])vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets',name),'utf8'),context);
async function run() {
 const changes=[];
 const basketElements=()=>({items:{querySelectorAll:()=>[]},total:{},checkout:{},grid:{}});
 const empty=context.BasketUI.createBasket({elements:basketElements(),escapeHtml:String,money:String,onChange:valid=>changes.push(valid)});
 assert.equal(empty.isValid(),false);assert.deepEqual(changes,[false]);
 assert.doesNotThrow(()=>context.BasketUI.createBasket({elements:basketElements(),escapeHtml:String,money:String}));
 const button={},error={},saved=[],locations=[];let resolve,calls=0,valid=true;
 const basket={isValid:()=>valid,getItems:()=>[{menuItemId:8,quantity:3,price:999,name:'Rice'}]};
 const checkout=context.CheckoutUI.createCheckout({basket,button,error,saveToken:(...args)=>saved.push(args),navigate:url=>locations.push(url),request:async(url,options)=>{
  calls++;assert.equal(url,'/api/v1/orders');assert.equal(options.method,'POST');assert.deepEqual(JSON.parse(options.body),{items:[{menuItemId:8,quantity:3}]});
  return new Promise(done=>{resolve=done;});
 }});
 valid=false;checkout.refresh();assert.equal(button.disabled,true);assert.equal(await checkout.submit(),false);assert.equal(calls,0);
 valid=true;checkout.refresh();const pending=checkout.submit();assert.equal(button.disabled,true);assert.equal(await checkout.submit(),false);
 resolve({id:12,queueToken:'opaque-token-from-server'});assert.equal(await pending,true);assert.deepEqual(saved,[[12,'opaque-token-from-server']]);assert.deepEqual(locations,['/queue/12']);assert.equal(button.disabled,true);assert.equal(await checkout.submit(),false);assert.equal(calls,1);
 const blocked=context.CheckoutUI.createCheckout({basket,button:{},error:{},request:async()=>({id:13,queueToken:'server-token'}),saveToken:()=>{throw Error('storage blocked');},navigate:()=>assert.fail('must not navigate after storage failure')});
 assert.equal(await blocked.submit(),false);assert.equal(await blocked.submit(),false);
 const apiCalls=[];let forbidden=false;
 context.fetch=async(url,options)=>{
  apiCalls.push([url,options]);if(url==='/api/v1/csrf')return {ok:true,status:200,json:async()=>({headerName:'X-CSRF-TOKEN',token:'csrf-from-server'})};
  return forbidden?{ok:false,status:403,json:async()=>({message:'Forbidden'})}:{ok:true,status:201,json:async()=>({id:1})};
 };
 await context.CoreUI.api('/api/v1/orders',{method:'POST',body:'{}',headers:{'X-Custom':'kept'}});
 assert.equal(apiCalls[1][1].headers['X-CSRF-TOKEN'],'csrf-from-server');assert.equal(apiCalls[1][1].headers['X-Custom'],'kept');assert.equal(apiCalls[1][1].headers['Content-Type'],'application/json');
 forbidden=true;await assert.rejects(context.CoreUI.api('/api/v1/orders',{method:'POST',body:'{}'}),/Forbidden/);
 forbidden=false;await context.CoreUI.api('/api/v1/orders',{method:'POST',body:'{}'});assert.equal(apiCalls.filter(call=>call[0]==='/api/v1/csrf').length,2);
 console.log('PASS: valid checkout payload, duplicate submission lock, server token storage, navigation, storage failure and CSRF header refresh.');
}
run().catch(error=>{console.error(error);process.exitCode=1;});
