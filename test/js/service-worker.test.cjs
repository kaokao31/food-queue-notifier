const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');
const handlers={},notifications=[],opened=[];let windows=[],focused=0;
const self={location:{origin:'https://queue.example'},addEventListener:(name,fn)=>handlers[name]=fn,
 registration:{async showNotification(title,options){notifications.push({title,options});}},
 clients:{async matchAll(){return windows;},async openWindow(url){opened.push(url);}}};
const context={self,URL};vm.createContext(context);
vm.runInContext(fs.readFileSync('code/src/main/resources/static/sw.js','utf8'),context);
async function push(data){let done;handlers.push({data,waitUntil(p){done=p;}});await done;return notifications.at(-1);}
async function click(url){let done,closed=false;handlers.notificationclick({notification:{data:{url},close(){closed=true;}},waitUntil(p){done=p;}});await done;assert.equal(closed,true);}
(async()=>{
 const notice=await push({json:()=>({title:'พร้อมรับ',body:'รับอาหาร',tag:'queue-8-ready',url:'/queue/8',token:'SECRET'})});
 assert.equal(notice.options.data.url,'/queue/8');assert.equal(notice.options.tag,'queue-8-ready');assert.doesNotMatch(JSON.stringify(notice),/SECRET/);
 await push({json(){throw Error('malformed');}});assert.equal(notifications.at(-1).title,'พร้อมรับ');
 await push(null);await push({json:()=>null});
 for(const url of ['https://evil.example','//evil.example','/queue/8?token=SECRET','/queue/8#SECRET','/queue/../staff',null]){
  const result=await push({json:()=>({url})});assert.equal(result.options.data.url,'/');
  await click(url);assert.equal(opened.at(-1),'https://queue.example/');
 }
 windows=[{url:'https://queue.example/queue/8',async focus(){focused++;}}];await click('/queue/8');assert.equal(focused,1);
 windows=[];await click('/queue/9');assert.equal(opened.at(-1),'https://queue.example/queue/9');
 await click('/push-demo.html');assert.equal(opened.at(-1),'https://queue.example/push-demo.html');
 const bounded=await push({json:()=>({title:'x'.repeat(500),body:'y'.repeat(1000),tag:'INVALID'})});
 assert.equal(bounded.title.length,100);assert.equal(bounded.options.body.length,500);assert.equal(bounded.options.tag,'queue-update');
 assert.equal(handlers.fetch,undefined);
 console.log('PASS: worker notification payload, malformed data, credential-free local links, click focus/open, bounded text and no API caching.');
})().catch(error=>{console.error(error);process.exitCode=1;});
