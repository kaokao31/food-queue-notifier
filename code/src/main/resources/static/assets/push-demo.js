(function(){
 'use strict';
 const result=document.getElementById('result'),buttons=Array.from(document.querySelectorAll('button'));
 let busy=false;
 async function request(url,options={}){
  const response=await fetch(url,{credentials:'same-origin',...options});
  if(!response.ok)throw Error('คำขอไม่สำเร็จ ('+response.status+') ตรวจสิทธิ์ STAFF และการตั้งค่า Push');
  return response.status===204?null:response.json();
 }
 async function mutate(path,method,body){
  const csrf=await request('/api/v1/csrf');
  return request('/api/v1/push-demo/'+path,{method,headers:{[csrf.headerName]:csrf.token,'Content-Type':'application/json'},
   ...(body?{body:JSON.stringify(body)}:{})});
 }
 async function action(run){if(busy)return;busy=true;buttons.forEach(b=>b.disabled=true);
  try{await run();}catch(error){result.textContent=error.message;}
  finally{busy=false;buttons.forEach(b=>b.disabled=false);}
 }
 document.getElementById('register').addEventListener('click',()=>action(async()=>{
  if(!window.isSecureContext || !('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window))
   throw Error('เบราว์เซอร์หรือการเชื่อมต่อนี้ไม่รองรับ Web Push');
  const permission=await Notification.requestPermission(); // Invoked from this explicit click.
  if(permission!=='granted')throw Error('ยังไม่ได้อนุญาตการแจ้งเตือน');
  const config=await request('/api/v1/push/public-key');
  await navigator.serviceWorker.register('/sw.js');const worker=await navigator.serviceWorker.ready;
  const key=atob(config.publicKey.replace(/-/g,'+').replace(/_/g,'/')+'='.repeat((4-config.publicKey.length%4)%4));
  const subscription=await worker.pushManager.getSubscription() || await worker.pushManager.subscribe({userVisibleOnly:true,
   applicationServerKey:Uint8Array.from(key,c=>c.charCodeAt(0))});
  await mutate('subscription','POST',subscription.toJSON());result.textContent='สมัครรับข้อความทดสอบแล้ว';
 }));
 document.getElementById('send').addEventListener('click',()=>action(async()=>{
  const response=await mutate('send','POST');result.textContent='Provider ตอบรับ ('+response.providerStatus+') ยังไม่ยืนยันว่าอุปกรณ์แสดงข้อความ';
 }));
 document.getElementById('remove').addEventListener('click',()=>action(async()=>{
  await mutate('subscription','DELETE');result.textContent='ลบข้อมูล demo ใน session นี้แล้ว';
 }));
})();
