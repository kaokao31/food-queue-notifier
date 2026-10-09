/* Notification worker only: no caching of orders, tokens or API responses. */
'use strict';
function localTarget(value) {
 return typeof value==='string' && (value==='/push-demo.html' || /^\/queue\/[1-9][0-9]{0,18}$/.test(value))?value:'/';
}
self.addEventListener('push',event=>{
 let data={};
 try { const parsed=event.data?.json(); if(parsed && typeof parsed==='object') data=parsed; } catch (_) {}
 const title=typeof data.title==='string' && data.title?data.title.slice(0,100):'พร้อมรับ';
 const body=typeof data.body==='string'?data.body.slice(0,500):'มีการอัปเดตคิว กรุณาเปิดหน้าเว็บเพื่อตรวจสอบ';
 const tag=typeof data.tag==='string' && /^queue-[1-9][0-9]{0,18}-ready$/.test(data.tag)?data.tag:'queue-update';
 event.waitUntil(self.registration.showNotification(title,{body,tag,data:{url:localTarget(data.url)}}));
});
self.addEventListener('notificationclick',event=>{
 event.notification.close();
 const target=new URL(localTarget(event.notification.data?.url),self.location.origin).href;
 event.waitUntil((async()=>{
  const windows=await self.clients.matchAll({type:'window',includeUncontrolled:true});
  for(const window of windows) if(window.url===target && typeof window.focus==='function') return window.focus();
  return self.clients.openWindow(target);
 })());
});
