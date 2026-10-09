(function (root) {
 'use strict';
 function keyBytes(value) {
  if(typeof value !== 'string' || !/^[A-Za-z0-9_-]{87}=?$/.test(value)) throw new Error('Public key ไม่ถูกต้อง');
  const raw=root.atob(value.replace(/-/g,'+').replace(/_/g,'/')+'='.repeat((4-value.length%4)%4));
  const bytes=Uint8Array.from(raw,c=>c.charCodeAt(0));
  if(bytes.length!==65 || bytes[0]!==4) throw new Error('Public key ไม่ถูกต้อง');
  return bytes;
 }
 function createClient({id,getToken,request,elements,environment=root}) {
  const {enable,disable,message}=elements;
  let busy=false,disposed=false,terminal=false;
  const supported=environment.isSecureContext===true && !!environment.navigator?.serviceWorker &&
   !!environment.Notification && !!environment.PushManager;
  function token() { try { return getToken(); } catch (_) { return null; } }
  function access() { return /^[1-9][0-9]{0,18}$/.test(String(id)) && !!token(); }
  function say(value) { if(!disposed) message.textContent=value; }
  function refresh() {
   if(disposed) return;
   enable.disabled=busy || terminal || !access() || !supported || environment.Notification.permission==='denied';
   disable.disabled=busy || !access();
  }
  function options(method,body) {
   const ownerToken=token();
   if(!ownerToken) throw new Error('ไม่พบสิทธิ์ของออเดอร์ในเครื่องนี้');
   return {method,headers:{'X-Queue-Token':ownerToken},...(body?{body:JSON.stringify(body)}:{})};
  }
  async function attach() {
   if(disposed || busy || terminal || !supported || !access()) return;
   if(environment.Notification.permission==='denied') { say('เบราว์เซอร์ปิดสิทธิ์แจ้งเตือนอยู่ คุณยังติดตามคิวในหน้านี้ได้'); return; }
   busy=true;refresh();say('กำลังเชื่อมการแจ้งเตือน…');
   try {
    // Invoke permission directly from the user gesture, before any network await.
    const permission=environment.Notification.permission==='granted'?'granted':await environment.Notification.requestPermission();
    if(disposed) return;
    if(permission!=='granted') { say('ยังไม่ได้อนุญาตแจ้งเตือน คุณยังติดตามคิวในหน้านี้ได้'); return; }
    const config=await request('/api/v1/push/public-key');
    if(disposed || terminal) return;
    const applicationServerKey=keyBytes(config?.publicKey);
    let registration=await environment.navigator.serviceWorker.register('/sw.js',{scope:'/'});
    if(disposed || terminal) return;
    if(!registration.active) registration=await environment.navigator.serviceWorker.ready;
    if(disposed || terminal) return;
    if(!registration.pushManager) throw new Error('เบราว์เซอร์นี้ยังไม่พร้อมใช้ Push');
    let subscription=await registration.pushManager.getSubscription();
    if(disposed || terminal) return;
    if(!subscription) subscription=await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey});
    if(disposed || terminal) return;
    const data=subscription.toJSON();
    await request('/api/v1/orders/'+id+'/subscription',options('POST',{endpoint:data.endpoint,keys:data.keys}));
    if(!disposed) say('เชื่อมการแจ้งเตือนกับออเดอร์นี้แล้ว การรับแจ้งเตือนขึ้นกับระบบส่งและเบราว์เซอร์');
   } catch (_) {
    // Do not render raw provider errors or subscription credentials.
    say('เชื่อมแจ้งเตือนยังไม่สำเร็จ ระบบสิทธิ์/CSRF หรือ Push อาจยังไม่พร้อม คุณยังติดตามคิวในหน้านี้ได้');
   } finally { busy=false;refresh(); }
  }
  async function detach() {
   if(disposed || busy || !access()) return;
   busy=true;refresh();say('กำลังปิดแจ้งเตือนของออเดอร์นี้…');
   try {
    await request('/api/v1/orders/'+id+'/subscription',options('DELETE'));
    say('ปิดแจ้งเตือนของออเดอร์นี้แล้ว ออเดอร์อื่นไม่เปลี่ยนแปลง');
    // The browser subscription may be shared by other orders: do not unsubscribe it.
   } catch (_) { say('ยังปิดแจ้งเตือนไม่สำเร็จ กรุณาลองใหม่เมื่อระบบสิทธิ์พร้อม'); }
   finally { busy=false;refresh(); }
  }
  const onEnable=()=>{void attach();},onDisable=()=>{void detach();};
  enable.addEventListener('click',onEnable);disable.addEventListener('click',onDisable);
  say(!access()?'ไม่พบสิทธิ์ของออเดอร์ในเครื่องนี้':!supported?'เบราว์เซอร์นี้ใช้ Push ไม่ได้ หรือไม่ได้เปิดผ่าน HTTPS/localhost คุณยังติดตามคิวในหน้านี้ได้':
   environment.Notification.permission==='denied'?'เบราว์เซอร์ปิดสิทธิ์แจ้งเตือนอยู่ คุณยังติดตามคิวในหน้านี้ได้':'กดเปิด/เชื่อมแจ้งเตือนเพื่อสมัครกับออเดอร์นี้ ยังไม่ได้ตรวจสถานะการสมัครเดิม');
  refresh();
  return {attach,detach,update(order){terminal=['COMPLETED','CANCELLED'].includes(order.status);refresh();},
   stop(){disposed=true;enable.removeEventListener('click',onEnable);disable.removeEventListener('click',onDisable);enable.disabled=true;disable.disabled=true;}};
 }
 root.PushClient={keyBytes,createClient};
})(globalThis);
