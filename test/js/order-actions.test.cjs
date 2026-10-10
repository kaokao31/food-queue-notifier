const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2] || path.join(__dirname,'../..')),context=vm.createContext({setTimeout,clearTimeout});
vm.runInContext(fs.readFileSync(path.join(root,'code/src/main/resources/static/assets/queue-ui.js'),'utf8'),context);
function input(id,value){return {dataset:{menuId:String(id)},value:String(value),reportValidity:()=>true};}
async function run(){
 let inputs=[input(2,3),input(3,0)],calls=[],release,fail=false,allow=true;const busy=[];
 const elements={edit:{},cancel:{},save:{},close:{},error:{},dialogError:{},fields:{querySelectorAll:()=>inputs},dialog:{showModal(){},close(){},addEventListener(){}}};
 const actions=context.QueueUI.createActions({id:7,getToken:()=> 'bill-token',elements,escapeHtml:v=>String(v).replaceAll('<','&lt;'),money:String,confirmCancel:()=>allow,onBusy:v=>busy.push(v),request:async(url,options)=>{calls.push({url,options});if(fail)throw Error('server rejects');await new Promise(resolve=>release=resolve);}});
 assert.equal(elements.edit.disabled,true);actions.update({queue:{status:'WAITING'},items:[{menuItemId:2,menuItemName:'<rice>',quantity:1,unitPrice:10}]});assert.equal(elements.edit.disabled,false);
 elements.edit.onclick();assert.match(elements.fields.innerHTML,/&lt;rice>/);
 const pending=elements.save.onclick();await elements.save.onclick();assert.equal(calls.length,1);assert.equal(calls[0].url,'/api/v1/orders/7');assert.equal(calls[0].options.method,'PUT');assert.equal(calls[0].options.headers['X-Queue-Token'],'bill-token');assert.deepEqual(JSON.parse(calls[0].options.body),{items:[{menuItemId:2,quantity:3}]});release();await pending;assert.deepEqual(busy,[true,false]);
 elements.edit.onclick();inputs=[input(2,0)];await elements.save.onclick();assert.equal(calls.length,1);assert.match(elements.error.textContent,/อย่างน้อย/);
 inputs=[input(2,1.5)];await elements.save.onclick();assert.equal(calls.length,1);
 actions.update({queue:{status:'PREPARING'}});assert.equal(elements.edit.disabled,true);assert.equal(elements.cancel.disabled,false);allow=false;await elements.cancel.onclick();assert.equal(calls.length,1);
 allow=true;fail=true;await elements.cancel.onclick();assert.equal(calls[1].url,'/api/v1/queues/7/cancel');assert.equal(calls[1].options.method,'PATCH');assert.match(elements.error.textContent,/server rejects/);assert.equal(elements.cancel.disabled,false);
 actions.update({queue:{status:'READY'}});assert.equal(elements.cancel.disabled,true);await elements.cancel.onclick();assert.equal(calls.length,2);
 console.log('PASS: quantity edit/removal, ID-only payload, token header, duplicate lock, zero/fraction rejection, confirmation, server errors and status controls.');
}
run().catch(error=>{console.error(error);process.exitCode=1;});
