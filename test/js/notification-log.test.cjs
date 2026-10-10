const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const context={};vm.createContext(context);vm.runInContext(fs.readFileSync('code/src/main/resources/static/assets/notification-log-ui.js','utf8'),context);
const {createViewer,render}=context.NotificationLogUI;
const escapeHtml=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function element(){return {disabled:false,textContent:'',innerHTML:'',value:'8',open:false,events:{},addEventListener(k,f){this.events[k]=f;},removeEventListener(k){delete this.events[k];},showModal(){this.open=true;},close(){this.open=false;}};}
function setup(request){const elements={form:element(),id:element(),open:element(),error:element(),dialog:element(),title:element(),content:element(),close:element()};return {elements,viewer:createViewer({request,elements,escapeHtml})};}
(async()=>{
 const html=render([{deliveryStatus:'ACCEPTED',channel:'PUSH',httpStatus:201,attemptedAt:'<img onerror=x>',sentAt:null,message:'SECRET'}],escapeHtml);
 assert.ok(html.includes('ยังไม่ยืนยัน'));assert.ok(html.includes('&lt;img'));assert.ok(!html.includes('<img'));assert.ok(!html.includes('SECRET'));assert.ok(html.includes('201'));
 for(const deliveryStatus of ['PENDING','PREVIEW','FAILED','LEGACY','__proto__',null])assert.doesNotThrow(()=>render([{deliveryStatus}],escapeHtml));
 assert.throws(()=>render({}));assert.throws(()=>render([null]));assert.match(render([],escapeHtml),/ยังไม่มีบันทึก/);
 let calls=[];let s=setup(async path=>{calls.push(path);return [];});await s.viewer.open();assert.equal(calls[0],'/api/v1/orders/8/notifications');assert.equal(s.elements.dialog.open,true);assert.match(s.elements.title.textContent,/#8/);
 for(const id of ['0','-1','1.5','../../other','', 'x']) await s.viewer.open(id);assert.equal(calls.length,1);
 let release;s=setup(()=>new Promise(resolve=>release=resolve));const first=s.viewer.open();await s.viewer.open();s.viewer.stop();release([]);await first;
 assert.equal(s.elements.dialog.open,false);assert.equal(s.elements.open.disabled,true);assert.equal(s.elements.form.events.submit,undefined);
 s=setup(async()=>{throw Error('SECRET_KEY_PROVIDER');});await s.viewer.open();assert.ok(!s.elements.error.textContent.includes('SECRET'));assert.match(s.elements.error.textContent,/พนักงาน/);assert.equal(s.elements.open.disabled,false);
 let releases=[];s=setup(()=>new Promise(resolve=>releases.push(resolve)));const old=s.viewer.open();s.elements.close.events.click();const newer=s.viewer.open('9');releases[0]([{deliveryStatus:'FAILED'}]);await old;
 assert.equal(s.elements.dialog.open,false);releases[1]([]);await newer;assert.equal(s.elements.dialog.open,true);assert.match(s.elements.title.textContent,/#9/);
 const nodes={},events={};let stopped=0,loaded=0;
 Object.assign(context,{document:{body:{dataset:{page:'staff'}}},CoreUI:{$:key=>nodes[key]??=element(),api:async()=>[],escapeHtml,money:String,labels:{}},
  StaffQueueUI:{createStaffQueue:()=>({stop(){stopped++;},async load(){loaded++;}})},addEventListener:(name,f)=>events[name]=f});
 vm.runInContext(fs.readFileSync('code/src/main/resources/static/assets/app.js','utf8'),context);assert.equal(loaded,1);assert.equal(typeof nodes['#notification-log-form'].events.submit,'function');events.pagehide();assert.equal(stopped,1);assert.equal(nodes['#notification-log-form'].events.submit,undefined);
 const template=fs.readFileSync('code/src/main/resources/templates/staff.html','utf8');assert.ok(template.includes('id="notification-log-dialog"'));assert.ok(template.indexOf('/assets/notification-log-ui.js')<template.indexOf('/assets/app.js'));
 console.log('PASS: notification log metadata escaping, status meanings, empty results, ID validation, duplicate guard, safe authorization errors, stale/disposed responses and staff bootstrap.');
})().catch(error=>{console.error(error);process.exitCode=1;});
