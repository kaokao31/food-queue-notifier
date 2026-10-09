const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('code/src/main/resources/static/assets/push-demo.js','utf8');
function fixture(permission='granted'){
 const calls=[],nodes={},callbacks={};for(const id of ['result','register','send','remove'])nodes[id]={disabled:false,textContent:'',addEventListener:(name,cb)=>callbacks[id]=cb};
 const subscription={toJSON:()=>({endpoint:'test-only',keys:{p256dh:'test',auth:'test'}})};
 const window={isSecureContext:true,PushManager:function(){},Notification:{}};
 const context={window,document:{getElementById:id=>nodes[id],querySelectorAll:()=>[nodes.register,nodes.send,nodes.remove]},
  Notification:{requestPermission:async()=>permission},navigator:{serviceWorker:{register:async()=>calls.push({url:'worker'}),
   ready:Promise.resolve({pushManager:{getSubscription:async()=>subscription,subscribe:async()=>{throw Error('Must reuse existing subscription');}}})}},
  Uint8Array,atob:s=>Buffer.from(s,'base64').toString('binary'),
  fetch:async(url,options)=>{calls.push({url,...options});return {ok:true,status:url.endsWith('subscription')?204:200,
   json:async()=>url.endsWith('public-key')?{publicKey:'BA'}:url.endsWith('csrf')?{headerName:'X-CSRF-TOKEN',token:'test-csrf'}:{providerStatus:201}};}};
 vm.runInNewContext(source,context);return {calls,nodes,callbacks,context};
}
(async()=>{
 const f=fixture();await f.callbacks.register();
 const register=f.calls.find(c=>c.url==='/api/v1/push-demo/subscription');assert.equal(register.method,'POST');
 assert.equal(register.headers['X-CSRF-TOKEN'],'test-csrf');assert.equal(register.credentials,'same-origin');
 assert.equal(JSON.parse(register.body).endpoint,'test-only');
 await f.callbacks.send();assert.match(f.nodes.result.textContent,/201/);
 await f.callbacks.remove();assert.equal(f.calls.filter(c=>c.url.endsWith('subscription')).at(-1).method,'DELETE');
 const denied=fixture('denied');await denied.callbacks.register();assert.equal(denied.calls.length,0);assert.match(denied.nodes.result.textContent,/อนุญาต/);
 const insecure=fixture();insecure.context.window.isSecureContext=false;await insecure.callbacks.register();assert.equal(insecure.calls.length,0);
 const failed=fixture();failed.context.fetch=async()=>({ok:false,status:503});await failed.callbacks.send();
 assert.match(failed.nodes.result.textContent,/503/);assert.equal(failed.nodes.send.disabled,false);
 console.log('PASS: demo permission/secure context, subscription reuse, session/CSRF, send result, demo-only detach and safe failure recovery.');
})().catch(error=>{console.error(error);process.exitCode=1;});
