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
 root.QueueUI=Object.freeze({createTracker,renderOrder});
})(globalThis);
