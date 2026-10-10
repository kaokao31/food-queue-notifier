const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const context={atob,Uint8Array};vm.createContext(context);
vm.runInContext(fs.readFileSync('code/src/main/resources/static/assets/push-client.js','utf8'),context);
const {createClient,keyBytes}=context.PushClient;
const publicKey=Buffer.concat([Buffer.from([4]),Buffer.alloc(64,1)]).toString('base64url');
function element(){return {disabled:false,textContent:'',handlers:{},addEventListener(k,f){this.handlers[k]=f;},removeEventListener(k){delete this.handlers[k];}};}
function setup(overrides={}){
 const calls=[],elements={enable:element(),disable:element(),message:element()};
 const subscription={toJSON:()=>({endpoint:'https://fcm.googleapis.com/fcm/send/test-only',keys:{p256dh:'test-only',auth:'test-only'}}),unsubscribe(){throw Error('must preserve other orders');}};
 const registration={active:{},pushManager:{async getSubscription(){calls.push(['existing']);return subscription;},async subscribe(options){calls.push(['subscribe',options]);return subscription;}}};
 const environment={isSecureContext:true,PushManager:function(){},Notification:{permission:'granted',async requestPermission(){calls.push(['permission']);return 'granted';}},navigator:{serviceWorker:{async register(path,options){calls.push(['register',path,options]);return registration;},ready:Promise.resolve(registration)}}};
 const request=async(path,options)=>{calls.push(['api',path,options]);return path.endsWith('public-key')?{publicKey}:null;};
 const settings={id:'8',getToken:()=> 'owner-fixture',request,elements,environment,...overrides};
 const client=createClient(settings);return {client,calls,elements,subscription,registration,environment,settings};
}
(async()=>{
 assert.equal(keyBytes(publicKey).length,65);assert.equal(keyBytes(publicKey+'=').length,65);
 for(const key of [null,'invalid',Buffer.alloc(65).toString('base64url')]) assert.throws(()=>keyBytes(key));
 let s=setup();await s.client.attach();
 const post=s.calls.find(x=>x[0]==='api'&&x[2]?.method==='POST');
 assert.equal(post[1],'/api/v1/orders/8/subscription');assert.equal(post[2].headers['X-Queue-Token'],'owner-fixture');
 assert.equal(JSON.parse(post[2].body).endpoint,'https://fcm.googleapis.com/fcm/send/test-only');
 assert.equal(s.calls.filter(x=>x[0]==='subscribe').length,0);assert.match(s.elements.message.textContent,/เชื่อมการแจ้งเตือนกับออเดอร์นี้แล้ว/);
 await s.client.detach();assert.equal(s.calls.at(-1)[2].method,'DELETE');assert.match(s.elements.message.textContent,/ออเดอร์อื่นไม่เปลี่ยนแปลง/);
 s=setup();s.registration.pushManager.getSubscription=async()=>null;await s.client.attach();
 const subscribed=s.calls.find(x=>x[0]==='subscribe')[1];assert.equal(subscribed.userVisibleOnly,true);assert.equal(subscribed.applicationServerKey.length,65);
 s=setup();s.environment.Notification.permission='default';s.environment.Notification.requestPermission=async()=>{s.calls.push(['permission']);return 'denied';};
 await s.client.attach();assert.equal(s.calls.length,1);assert.match(s.elements.message.textContent,/ยังไม่ได้อนุญาต/);
 s=setup();s.environment.Notification.permission='default';s.environment.Notification.requestPermission=async()=>{s.calls.push(['permission']);return 'granted';};
 await s.client.attach();assert.equal(s.calls[0][0],'permission');
 let release;const pending=new Promise(resolve=>release=resolve);
 let pendingCalls=0;s=setup({request:async()=>{pendingCalls++;await pending;return {publicKey};}});
 const first=s.client.attach();await s.client.attach();assert.equal(s.elements.enable.disabled,true);
 assert.equal(pendingCalls,1);s.client.stop();release();await first;assert.equal(s.calls.length,0);assert.equal(s.elements.enable.handlers.click,undefined);assert.equal(s.elements.enable.disabled,true);
 s=setup();s.registration.active=null;let readyCalls=0;
 s.environment.navigator.serviceWorker.ready=Promise.resolve(s.registration).then(r=>{readyCalls++;return r;});await s.client.attach();assert.equal(readyCalls,1);
 s=setup({request:async()=>{throw Error('SECRET_ENDPOINT_AUTH_TOKEN');}});await s.client.attach();
 assert.doesNotMatch(s.elements.message.textContent,/SECRET/);assert.match(s.elements.message.textContent,/CSRF/);assert.equal(s.elements.enable.disabled,false);
 s=setup({getToken:()=>null});await s.client.attach();await s.client.detach();assert.equal(s.calls.length,0);assert.equal(s.elements.disable.disabled,true);
 s=setup({id:'../../other'});await s.client.attach();assert.equal(s.calls.length,0);
 s=setup();s.client.update({status:'COMPLETED'});await s.client.attach();assert.equal(s.calls.length,0);await s.client.detach();assert.equal(s.calls[0][2].method,'DELETE');
 const unsupported={isSecureContext:false};s=setup({environment:unsupported});await s.client.attach();assert.equal(s.calls.length,0);assert.match(s.elements.message.textContent,/HTTPS/);
 s=setup({getToken:()=>{throw Error('storage unavailable');}});assert.equal(s.elements.enable.disabled,true);
 s=setup();s.environment.Notification.permission='denied';await s.client.attach();assert.equal(s.calls.length,0);
 let resolveSubscription;s=setup();s.registration.pushManager.getSubscription=()=>new Promise(r=>resolveSubscription=r);
 const attaching=s.client.attach();while(!resolveSubscription) await Promise.resolve();s.client.stop();resolveSubscription(s.subscription);await attaching;
 assert.equal(s.calls.filter(x=>x[2]?.method==='POST').length,0);
 const nodes={},pageEvents={};let stopped=0,updated=0;
 Object.assign(context,{document:{body:{dataset:{page:'queue'}},querySelectorAll:()=>[]},location:{pathname:'/queue/8'},localStorage:{getItem:()=> 'owner-fixture'},
  addEventListener:(name,fn)=>pageEvents[name]=fn,
  CoreUI:{$:selector=>nodes[selector]??=element(),api:async()=>null,escapeHtml:String,money:String,labels:{}},
  QueueUI:{createActions:()=>({update(){updated++;}}),renderOrder(){},createTracker:options=>({stop(){stopped++;},async load(){options.onOrder({status:'COMPLETED'});}})}});
 vm.runInContext(fs.readFileSync('code/src/main/resources/static/assets/app.js','utf8'),context);
 assert.equal(updated,1);assert.equal(nodes['#enable-push'].disabled,true);assert.equal(typeof nodes['#disable-push'].handlers.click,'function');
 pageEvents.pagehide();assert.equal(stopped,1);assert.equal(nodes['#disable-push'].handlers.click,undefined);
 const html=fs.readFileSync('code/src/main/resources/templates/queue.html','utf8');
 assert.ok(html.indexOf('/assets/push-client.js')<html.indexOf('/assets/app.js'));assert.ok(html.includes('id="push-message"'));
 console.log('PASS: Push client key conversion, permission gesture/denial, existing/new subscription, active worker, owner token, per-order detach, duplicate guard, safe errors, terminal/missing access and disposal.');
})().catch(error=>{console.error(error);process.exitCode=1;});
