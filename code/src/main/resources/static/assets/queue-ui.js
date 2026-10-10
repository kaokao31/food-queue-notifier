(function(root){
 'use strict';
 function createTracker({id,getToken,request,render,onError,schedule=setTimeout,cancel=clearTimeout,onOrder=()=>{}}){
  let stopped=false,timer;
  async function load(){
   if(stopped)return;
   try{
    if(!/^[1-9]\d*$/.test(String(id)))throw Error('เลขออเดอร์ไม่ถูกต้อง');
    const token=getToken();
    if(!token)throw Error('ไม่พบสิทธิ์ติดตามคิวในเบราว์เซอร์นี้');
    const order=await request('/api/v1/orders/'+id,{headers:{'X-Queue-Token':token}});
    if(stopped)return;
    render(order);onError('');onOrder(order);
    if(['COMPLETED','CANCELLED'].includes(order.queue.status))return;
   }catch(error){if(stopped)return;onError(error.message);}
   if(!stopped)timer=schedule(load,5000);
  }
  return {load,stop(){stopped=true;cancel(timer);}};
 }
 function renderOrder(order,{elements,escapeHtml,money,labels}){
  const queue=order.queue;
  elements.number.textContent=String(queue.queueNumber).padStart(3,'0');
  elements.status.textContent=labels[queue.status] || queue.status;
  elements.status.className='status '+queue.status;
  elements.date.textContent=queue.queueDate ? 'วันที่คิว '+queue.queueDate : '';
  const help={WAITING:'รอร้านรับออเดอร์',PREPARING:'ร้านกำลังเตรียมอาหาร',READY:'อาหารพร้อมแล้ว กรุณารับที่ร้าน',COMPLETED:'รับอาหารเรียบร้อยแล้ว',CANCELLED:'ออเดอร์นี้ยกเลิกแล้ว'};
  elements.help.textContent=help[queue.status] || '';
  for(const step of elements.steps)step.classList.toggle('active',step.dataset.state===queue.status);
  elements.items.innerHTML=order.items.map(item=>`<div class="cart-item"><strong>${escapeHtml(item.menuItemName)}</strong><span>× ${escapeHtml(item.quantity)}</span><span>${money(item.subtotal)}</span></div>`).join('');
  elements.total.textContent=money(order.totalAmount);
 }

 function createActions({id,getToken,request,elements,escapeHtml,money,onBusy=()=>{},confirmCancel}){
  let order=null,busy=false,editing=false;
  function showError(message){elements.error.textContent=message;elements.dialogError.textContent=message;}
  function refresh(){
   elements.edit.disabled=busy || order?.queue.status!=='WAITING';
   elements.cancel.disabled=busy || !['WAITING','PREPARING'].includes(order?.queue.status);
   elements.save.disabled=busy || order?.queue.status!=='WAITING';elements.close.disabled=busy;
  }
  function headers(){const token=getToken();if(!token)throw Error('ไม่พบสิทธิ์ติดตามคิวในเบราว์เซอร์นี้');return {'X-Queue-Token':token};}
  async function mutate(path,options){
   if(busy)return;
   busy=true;refresh();showError('');onBusy(true);
   try{await request(path,{...options,headers:headers()});return true;}
   catch(error){showError(error.message);return false;}
   finally{busy=false;refresh();onBusy(false);}
  }
  elements.edit.onclick=()=>{
   if(elements.edit.disabled)return;
   elements.fields.innerHTML=order.items.map(item=>`<label class="cart-item"><span>${escapeHtml(item.menuItemName)} (${money(item.unitPrice)})</span><input type="number" min="0" max="99" step="1" value="${escapeHtml(item.quantity)}" data-menu-id="${escapeHtml(item.menuItemId)}" required aria-label="จำนวน ${escapeHtml(item.menuItemName)}"></label>`).join('');
   showError('');editing=true;elements.dialog.showModal();refresh();
  };
  elements.save.onclick=async()=>{
   if(elements.save.disabled || !editing)return;
   const inputs=Array.from(elements.fields.querySelectorAll('input'));
   if(!inputs.length || inputs.some(input=>!input.reportValidity()))return;
   const items=inputs.map(input=>({menuItemId:Number(input.dataset.menuId),quantity:Number(input.value)}));
   if(items.some(item=>!Number.isSafeInteger(item.menuItemId)||item.menuItemId<1||!Number.isInteger(item.quantity)||item.quantity<0||item.quantity>99)){showError('จำนวนอาหารต้องเป็นจำนวนเต็ม 0–99');return;}
   const selected=items.filter(item=>item.quantity>0);
   if(!selected.length){showError('ต้องมีอาหารอย่างน้อยหนึ่งรายการ หากไม่ต้องการให้ยกเลิกออเดอร์');return;}
   if(await mutate('/api/v1/orders/'+id,{method:'PUT',body:JSON.stringify({items:selected})})){editing=false;elements.dialog.close();}
  };
  elements.cancel.onclick=async()=>{
   if(elements.cancel.disabled || !confirmCancel('ยกเลิกออเดอร์นี้ใช่ไหม?'))return;
   await mutate('/api/v1/queues/'+id+'/cancel',{method:'PATCH'});
  };
  elements.close.onclick=()=>{if(!busy){editing=false;elements.dialog.close();}};
  elements.dialog.addEventListener('cancel',event=>{if(busy)event.preventDefault();});
  elements.dialog.addEventListener('close',()=>{editing=false;});
  refresh();
  return {update(next){order=next;refresh();},isBusy:()=>busy};
 }
 root.QueueUI=Object.freeze({createTracker,renderOrder,createActions});
})(globalThis);
