(function(root){
 'use strict';
 const statuses=['','WAITING','PREPARING','READY','COMPLETED','CANCELLED'];
 function card(order,{escapeHtml:e,money,labels}){
  const id=String(order.id);if(!/^[1-9][0-9]*$/.test(id))throw Error('ข้อมูลออเดอร์ไม่ถูกต้อง');
  const status=order.queue?.status;
  const advance={WAITING:'เริ่มปรุง',PREPARING:'พร้อมรับ',READY:'รับแล้ว'}[status];
  return `<article class="panel order-card"><h2>คิว ${e(String(order.queue?.queueNumber??'').padStart(3,'0'))}</h2><p>${e(labels[status]||status)} · #${id}</p><p>${e(order.queue?.queueDate||'')}</p><ul>${(order.items||[]).map(item=>`<li>${e(item.menuItemName)} × ${e(item.quantity)} · ${e(money(item.subtotal))}</li>`).join('')}</ul><strong>${e(money(order.totalAmount))}</strong><div class="actions"><button data-action="detail" data-id="${id}">รายละเอียด</button>${advance?`<button data-action="advance" data-id="${id}">${advance}</button>`:''}${['WAITING','PREPARING'].includes(status)?`<button data-action="cancel" data-id="${id}">ยกเลิก</button>`:''}${status==='WAITING'?`<button data-action="delete" data-id="${id}">ลบ</button>`:''}</div></article>`;
 }
 function createStaffQueue({request,elements,escapeHtml,money,labels,confirmAction=message=>root.confirm(message)}){
  let page=0,status='',data=null,busy=false,disposed=false,version=0;
  function controls(){
   elements.refresh.disabled=busy;elements.prev.disabled=busy||page===0;elements.next.disabled=busy||!data||page+1>=data.totalPages;
   elements.filters.querySelectorAll('[data-status]').forEach(button=>{button.disabled=busy;button.classList.toggle('active',button.dataset.status===status);});
   elements.list.querySelectorAll('[data-action]').forEach(button=>button.disabled=busy);
  }
  async function load(){
   if(busy||disposed)return;busy=true;const current=++version;controls();elements.error.textContent='';
   try{
    const result=await request(`/api/v1/orders?page=${page}&size=12${status?'&queueStatus='+encodeURIComponent(status):''}`);
    if(disposed||current!==version)return;
    if(!Array.isArray(result.content)||!Number.isInteger(result.totalPages)||result.totalPages<0)throw Error('ข้อมูลรายการคิวไม่ถูกต้อง');
    data=result;elements.list.innerHTML=result.content.length?result.content.map(order=>card(order,{escapeHtml,money,labels})).join(''):'<p class="muted">ไม่มีคิวในรายการนี้</p>';
    elements.page.textContent=`หน้า ${page+1} / ${Math.max(1,result.totalPages)}`;
   }catch(error){if(!disposed&&current===version){data=null;elements.list.innerHTML='';elements.error.textContent=error.message;}}
   finally{if(!disposed&&current===version){busy=false;controls();}}
  }
  async function action(kind,id){
   if(busy||disposed||!/^[1-9][0-9]*$/.test(String(id)))return;
   const order=data?.content.find(order=>String(order.id)===String(id));if(!order)return;
   const state=order.queue?.status;
   if(kind==='advance'&&!['WAITING','PREPARING','READY'].includes(state))return;
   if(kind==='cancel'&&!['WAITING','PREPARING'].includes(state))return;
   if(kind==='delete'&&state!=='WAITING')return;
   if(!['detail','advance','cancel','delete'].includes(kind))return;
   if(['cancel','delete'].includes(kind)&&!confirmAction(kind==='delete'?'ลบออเดอร์นี้หรือไม่?':'ยกเลิกคิวนี้หรือไม่?'))return;
   busy=true;controls();elements.error.textContent='';let reload=false;
   try{
    if(kind==='detail'){
     const detail=await request('/api/v1/orders/'+id);if(disposed)return;
     elements.detail.innerHTML=card(detail,{escapeHtml,money,labels});elements.detail.querySelectorAll('[data-action]').forEach(button=>button.remove());elements.dialog.showModal();
    }else{
     const path=kind==='delete'?'/api/v1/orders/'+id:'/api/v1/queues/'+id+'/'+kind;
     await request(path,{method:kind==='delete'?'DELETE':'PATCH'});
     // Return to page one after a mutation so removing the last row cannot strand an empty page.
     page=0;reload=true;
    }
   }catch(error){if(!disposed)elements.error.textContent=error.message;}
   finally{if(!disposed){busy=false;controls();}}
   if(reload&&!disposed)await load();
  }
  const onClick=event=>{const button=event.target.closest('[data-action]');if(button&&elements.list.contains(button))void action(button.dataset.action,button.dataset.id);};
  const onFilter=event=>{const button=event.target.closest('[data-status]');if(!busy&&button&&elements.filters.contains(button)&&statuses.includes(button.dataset.status)){status=button.dataset.status;page=0;void load();}};
  const refresh=()=>void load(),prev=()=>{if(!busy&&page>0){page--;void load();}},next=()=>{if(!busy&&data&&page+1<data.totalPages){page++;void load();}},close=()=>elements.dialog.close();
  elements.list.addEventListener('click',onClick);elements.filters.addEventListener('click',onFilter);elements.refresh.addEventListener('click',refresh);elements.prev.addEventListener('click',prev);elements.next.addEventListener('click',next);elements.close.addEventListener('click',close);
  controls();
  return {load,action,stop(){disposed=true;version++;elements.list.removeEventListener('click',onClick);elements.filters.removeEventListener('click',onFilter);elements.refresh.removeEventListener('click',refresh);elements.prev.removeEventListener('click',prev);elements.next.removeEventListener('click',next);elements.close.removeEventListener('click',close);}};
 }
 root.StaffQueueUI=Object.freeze({createStaffQueue,card});
})(globalThis);
