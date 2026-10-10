(function(root){
 'use strict';
 const statuses={PENDING:'รอผลยืนยัน',PREVIEW:'ตัวอย่างในระบบ',ACCEPTED:'ผู้ให้บริการรับคำขอ',FAILED:'ส่งไม่สำเร็จ',LEGACY:'ข้อมูลเดิม'};
 const messages={PENDING:'ยังไม่มีผลยืนยัน ไม่มีการลองซ้ำอัตโนมัติ',PREVIEW:'ไม่ได้ส่งไปยังผู้ให้บริการ',ACCEPTED:'ยังไม่ยืนยันว่าอุปกรณ์แสดงแจ้งเตือน',FAILED:'ไม่มีการลองซ้ำอัตโนมัติ',LEGACY:'ไม่ได้ยืนยันการส่งในระบบปัจจุบัน'};
 function render(logs,escapeHtml){
  if(!Array.isArray(logs)) throw Error('ข้อมูลผลแจ้งเตือนไม่ถูกต้อง');
  if(!logs.length) return '<p class="muted">ยังไม่มีบันทึกการแจ้งเตือนของออเดอร์นี้</p>';
  const e=value=>escapeHtml(String(value??'—'));
  return logs.map(log=>{
   if(!log || typeof log!=='object') throw Error('ข้อมูลผลแจ้งเตือนไม่ถูกต้อง');
   const status=Object.hasOwn(statuses,log.deliveryStatus)?log.deliveryStatus:'LEGACY';
   const channel=['PUSH','CONSOLE','LINE','SMS'].includes(log.channel)?log.channel:'UNKNOWN';
   const http=Number.isInteger(log.httpStatus)&&log.httpStatus>=100&&log.httpStatus<=599?log.httpStatus:'—';
   return `<article class="panel"><strong>${e(statuses[status])}</strong><p>${e(messages[status])}</p><p>ช่องทาง: ${e(channel)} · HTTP: ${e(http)}</p><p class="small">เวลาจองส่ง (UTC): ${e(log.attemptedAt)}<br>เวลาผู้ให้บริการรับคำขอ (UTC): ${e(log.sentAt)}</p></article>`;
  }).join('');
 }
 function createViewer({request,elements,escapeHtml}){
  let version=0,disposed=false,busy=false;
  function controls(){if(!disposed) elements.open.disabled=busy;}
  async function open(value=elements.id.value){
   if(disposed||busy) return;
   const id=String(value).trim();elements.error.textContent='';
   if(!/^[1-9][0-9]{0,18}$/.test(id)){elements.error.textContent='ใส่เลขออเดอร์เป็นจำนวนเต็มบวก';return;}
   const current=++version;busy=true;controls();elements.content.innerHTML='';elements.title.textContent='ผลแจ้งเตือนออเดอร์ #'+id;
   try{
    const result=await request('/api/v1/orders/'+id+'/notifications');
    if(disposed||current!==version)return;
    elements.content.innerHTML=render(result,escapeHtml);if(!elements.dialog.open) elements.dialog.showModal();
   }catch(_){if(!disposed&&current===version)elements.error.textContent='อ่านผลแจ้งเตือนไม่สำเร็จ ต้องเป็นพนักงานที่เข้าสู่ระบบและระบบสิทธิ์ต้องพร้อม กรุณาตรวจเลขออเดอร์แล้วลองใหม่';}
   finally{if(!disposed&&current===version){busy=false;controls();}}
  }
  const submit=event=>{event.preventDefault();void open();};
  const invalidate=()=>{version++;busy=false;controls();};
  const close=()=>{invalidate();elements.dialog.close();};
  elements.form.addEventListener('submit',submit);elements.close.addEventListener('click',close);elements.dialog.addEventListener('cancel',invalidate);
  controls();
  return {open,stop(){disposed=true;version++;elements.form.removeEventListener('submit',submit);elements.close.removeEventListener('click',close);elements.dialog.removeEventListener('cancel',invalidate);elements.open.disabled=true;}};
 }
 root.NotificationLogUI=Object.freeze({createViewer,render});
})(globalThis);
