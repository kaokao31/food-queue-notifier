(function (root) {
 'use strict';
 function storedOrderIds(storage) {
  const ids=[];
  for(let i=0;i<storage.length;i++) {
   const key=storage.key(i),match=/^queue-token-([1-9]\d*)$/.exec(key || '');
   if(match && storage.getItem(key)) ids.push(match[1]);
  }
  return ids.sort((a,b)=>BigInt(a)>BigInt(b)?-1:BigInt(a)<BigInt(b)?1:0);
 }
 function queueDateText(value) {
  if(!value)return '';
  const date=new Date(value+'T00:00:00+07:00');
  return Number.isNaN(date.getTime())?'':date.toLocaleDateString('th-TH',{dateStyle:'medium',timeZone:'Asia/Bangkok'});
 }
 function orderedAt(value) {
  if(!value)return '';
  const date=new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(value)?value:value+'Z');
  return Number.isNaN(date.getTime())?'':date.toLocaleString('th-TH',{dateStyle:'medium',timeStyle:'short'});
 }
 function createHistory({elements,request,getStorage,escapeHtml,money,labels}) {
  const pageSize=5;let ids=[],page=0,revision=0;
  async function draw() {
   const current=++revision,totalPages=Math.max(1,Math.ceil(ids.length/pageSize));
   elements.prev.disabled=true;elements.next.disabled=true;elements.page.textContent=`หน้า ${page+1} / ${totalPages}`;
   elements.list.innerHTML=ids.length?'<p class="muted">กำลังโหลดประวัติคิว…</p>':'<p class="muted">ยังไม่มีประวัติการสั่งอาหารในเบราว์เซอร์นี้</p>';
   const rows=await Promise.all(ids.slice(page*pageSize,(page+1)*pageSize).map(async id=>{
    try {
     const token=getStorage().getItem('queue-token-'+id);
     if(!token)throw Error('ไม่พบสิทธิ์ติดตามออเดอร์ในเครื่องนี้');
     const order=await request('/api/v1/orders/'+id,{headers:{'X-Queue-Token':token}});
     return `<article class="history-row"><div class="history-heading"><strong>คิว ${escapeHtml(String(order.queue.queueNumber).padStart(3,'0'))}</strong><span class="status ${escapeHtml(order.queue.status)}">${escapeHtml(labels[order.queue.status] || order.queue.status)}</span></div><p class="muted small">บิล #${escapeHtml(id)} · วันที่คิว ${escapeHtml(queueDateText(order.queue.queueDate))}<br>สั่งเมื่อ ${escapeHtml(orderedAt(order.createdAt))}</p><p>${order.items.map(item=>escapeHtml(item.menuItemName)+' × '+escapeHtml(item.quantity)).join(', ')}</p><div class="history-heading"><strong>${money(order.totalAmount)}</strong><a class="history-link" href="/queue/${id}">ดูรายละเอียดคิว →</a></div></article>`;
    } catch(error) {return `<article class="history-row"><strong>บิล #${escapeHtml(id)}</strong><p class="error">${escapeHtml(error.message)}</p></article>`;}
   }));
   if(current!==revision)return;
   if(ids.length)elements.list.innerHTML=rows.join('');
   elements.prev.disabled=page===0;elements.next.disabled=page+1>=totalPages;
  }
  elements.open.disabled=false;elements.close.disabled=false;
  elements.open.onclick=async()=>{
   page=0;elements.dialog.showModal();
   try {ids=storedOrderIds(getStorage());await draw();}
   catch(error) {ids=[];revision++;elements.prev.disabled=true;elements.next.disabled=true;elements.page.textContent='';elements.list.textContent='เบราว์เซอร์ไม่อนุญาตให้อ่านประวัติที่เก็บในเครื่องนี้';}
  };
  elements.prev.onclick=()=>{if(!elements.prev.disabled){page--;return draw();}};
  elements.next.onclick=()=>{if(!elements.next.disabled){page++;return draw();}};
  elements.close.onclick=()=>elements.dialog.close();
  elements.dialog.addEventListener('close',()=>{revision++;});
 }
 root.HistoryUI=Object.freeze({storedOrderIds,queueDateText,createHistory});
})(globalThis);
