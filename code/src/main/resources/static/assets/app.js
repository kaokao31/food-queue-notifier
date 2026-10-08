'use strict';
const $=s=>document.querySelector(s), labels={WAITING:'รอคิว',PREPARING:'กำลังปรุง',READY:'พร้อมรับอาหาร',COMPLETED:'รับอาหารเรียบร้อย',CANCELLED:'ยกเลิกแล้ว'};
const money=x=>'฿'+Number(x).toLocaleString('th-TH',{minimumFractionDigits:0,maximumFractionDigits:2});
const escapeHtml=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let csrf=null;
async function api(path,options={}){
 const headers={...(options.body&&!(options.body instanceof FormData)?{'Content-Type':'application/json'}:{}),...options.headers};
 if(options.method&&options.method!=='GET'){
  if(!csrf){const r=await fetch('/api/v1/csrf',{cache:'no-store'});if(!r.ok)throw Error('โหลดสิทธิ์การส่งคำขอไม่สำเร็จ');csrf=await r.json();}
  headers[csrf.headerName]=csrf.token;
 }
 const response=await fetch(path,{...options,headers,cache:'no-store'});
 const data=response.status===204?null:await response.json().catch(()=>({message:'อ่านข้อมูลจากระบบไม่สำเร็จ'}));
 if(!response.ok){if(response.status===403)csrf=null;throw Error(data?.message||'คำขอไม่สำเร็จ ('+response.status+')');}
 return data;
}
function toast(message){$('#toast').textContent=message;$('#toast').style.display='block';setTimeout(()=>$('#toast').style.display='none',4500);}
function bindPager(state,load){$('#prev').onclick=()=>{state.page--;load().catch(e=>toast(e.message));};$('#next').onclick=()=>{state.page++;load().catch(e=>toast(e.message));};}
function pager(data){$('#prev').disabled=data.page===0;$('#next').disabled=data.page+1>=data.totalPages;$('#page-info').textContent=`หน้า ${data.page+1} / ${Math.max(1,data.totalPages)}`;}
function foodPhoto(m){
 if(m.imageUrl)return m.imageUrl;
 const images={'ข้าวกะเพราไก่':'basil-chicken-v2.webp','ข้าวผัดไข่':'egg-fried-rice-v2.avif','ข้าวไก่ทอด':'crispy-chicken-rice-v2.jpg','ชาไทยเย็น':'thai-tea-v2.jpg','น้ำมะนาว':'limeade-v2.jpg','ผัดไทย':'pad-thai-v2.webp'};
 const fallback=m.category==='เครื่องดื่ม'?'limeade-v2.jpg':m.category==='เส้น'?'pad-thai-v2.webp':'egg-fried-rice-v2.avif';
 return '/assets/food/'+(images[m.name]||fallback);
}
function menuCard(m,admin=false){return `<article class="menu-card"><div class="food-art ${m.category==='เครื่องดื่ม'?'drink':''}" ><img src="${foodPhoto(m)}" alt="${escapeHtml(m.name)}" loading="lazy" decoding="async" width="480" height="320"></div><div class="menu-content"><p class="muted small">${escapeHtml(m.category||'เมนูแนะนำ')} · ${m.prepTimeMinutes??'—'} นาที</p><h3>${escapeHtml(m.name)}</h3>${admin?`<span class="status">${m.isAvailable?'เปิดขาย':'ปิดขาย'}</span>`:''}<div class="menu-bottom"><strong class="price">${money(m.price)}</strong>${admin?`<button data-edit="${m.id}" aria-label="แก้ไข ${escapeHtml(m.name)}">แก้ไข</button>`:`<div class="menu-add"><input type="number" min="1" max="99" step="1" value="1" required inputmode="numeric" data-add-quantity="${m.id}" aria-label="จำนวน ${escapeHtml(m.name)}" title="จำนวนเต็ม 1–99"><button data-add="${m.id}" aria-label="เพิ่ม ${escapeHtml(m.name)} ลงตะกร้า">+ เพิ่ม</button></div>`}</div></div></article>`;}

function queueDateText(value){return value?new Date(value+'T00:00:00+07:00').toLocaleDateString('th-TH',{dateStyle:'medium',timeZone:'Asia/Bangkok'}):'';}
function setupQueueHistory(){
 const dialog=$('#queue-history-dialog'),list=$('#queue-history-list'),prev=$('#history-prev'),next=$('#history-next');
 const pageSize=5;let ids=[],page=0,revision=0;
 function storedIds(){
  const found=[];
  for(let i=0;i<localStorage.length;i++){
   const key=localStorage.key(i),match=/^queue-token-([1-9]\d*)$/.exec(key||'');
   if(match&&localStorage.getItem(key))found.push(match[1]);
  }
  return found.sort((a,b)=>BigInt(a)>BigInt(b)?-1:BigInt(a)<BigInt(b)?1:0);
 }
 function orderedAt(value){
  if(!value)return '';
  const date=new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(value)?value:value+'Z');
  return Number.isNaN(date.getTime())?'':date.toLocaleString('th-TH',{dateStyle:'medium',timeStyle:'short'});
 }
 async function draw(){
  const current=++revision,totalPages=Math.max(1,Math.ceil(ids.length/pageSize));
  prev.disabled=true;next.disabled=true;$('#history-page').textContent=`หน้า ${page+1} / ${totalPages}`;
  list.innerHTML=ids.length?'<p class="muted">กำลังโหลดประวัติคิว…</p>':'<p class="muted">ยังไม่มีประวัติการสั่งอาหารในเบราว์เซอร์นี้</p>';
  const rows=await Promise.all(ids.slice(page*pageSize,(page+1)*pageSize).map(async id=>{
   try{
    const token=localStorage.getItem('queue-token-'+id);
    if(!token)throw Error('ไม่พบสิทธิ์ติดตามออเดอร์ในเครื่องนี้');
    const order=await api('/api/v1/orders/'+id,{headers:{'X-Queue-Token':token}});
    return `<article class="history-row"><div class="history-heading"><strong>คิว ${escapeHtml(String(order.queue.queueNumber).padStart(3,'0'))}</strong><span class="status ${escapeHtml(order.queue.status)}">${escapeHtml(labels[order.queue.status]||order.queue.status)}</span></div><p class="muted small">บิล #${escapeHtml(id)} · วันที่คิว ${escapeHtml(queueDateText(order.queue.queueDate))}<br>สั่งเมื่อ ${escapeHtml(orderedAt(order.createdAt))}</p><p>${order.items.map(item=>escapeHtml(item.menuItemName)+' × '+item.quantity).join(', ')}</p><div class="history-heading"><strong>${money(order.totalAmount)}</strong><a class="history-link" href="/queue/${id}">ดูรายละเอียดคิว →</a></div></article>`;
   }catch(e){return `<article class="history-row"><strong>บิล #${escapeHtml(id)}</strong><p class="error">${escapeHtml(e.message)}</p></article>`;}
  }));
  if(current!==revision)return;
  if(ids.length)list.innerHTML=rows.join('');
  prev.disabled=page===0;next.disabled=page+1>=totalPages;
 }
 $('#open-queue-history').onclick=()=>{
  page=0;dialog.showModal();
  try{ids=storedIds();draw().catch(e=>{list.textContent=e.message;});}
  catch(e){ids=[];prev.disabled=true;next.disabled=true;$('#history-page').textContent='';list.textContent='เบราว์เซอร์ไม่อนุญาตให้อ่านประวัติที่เก็บในเครื่องนี้';}
 };
 prev.onclick=()=>{page--;draw();};next.onclick=()=>{page++;draw();};
 $('#close-queue-history').onclick=()=>dialog.close();
 dialog.addEventListener('close',()=>{revision++;});
}
async function menuPage(){
 setupQueueHistory();
 let menus=[],category='ทั้งหมด';const state={page:0},cart=new Map();
 function draw(){
  let visible=category==='ทั้งหมด'?menus:menus.filter(m=>m.category===category);
  $('#menu-grid').innerHTML=visible.map(m=>menuCard(m)).join('')||'<p class="empty">ไม่มีเมนูในหมวดนี้</p>';
  $('#menu-grid').querySelectorAll('[data-add]').forEach(b=>b.onclick=()=>{
   const m=menus.find(x=>x.id===Number(b.dataset.add)),input=b.closest('.menu-card').querySelector('[data-add-quantity]');
   if(!input.reportValidity())return;
   const quantity=input.valueAsNumber,line=cart.get(m.id)||{...m,quantity:0};
   if(line.quantity+quantity>99){toast('จำนวนรวมของเมนูนี้ต้องไม่เกิน 99');return;}
   line.quantity+=quantity;cart.set(m.id,line);drawCart();
  });
 }
 function updateCartSummary(){
  $('#cart-total').textContent=money([...cart.values()].reduce((total,m)=>total+m.price*m.quantity,0));
  $('#checkout').disabled=!cart.size||[...$('#cart-items').querySelectorAll('[data-quantity]')].some(input=>!input.checkValidity());
 }
 function drawCart(){
  $('#cart-items').innerHTML=[...cart.values()].map(m=>`<div class="cart-row"><div><h3>${escapeHtml(m.name)}</h3><span class="small muted">${money(m.price)}</span></div><div class="qty"><button data-minus="${m.id}" aria-label="ลดจำนวน ${escapeHtml(m.name)}">−</button><input type="number" min="1" max="99" step="1" value="${m.quantity}" required inputmode="numeric" data-quantity="${m.id}" aria-label="จำนวนในตะกร้า ${escapeHtml(m.name)}" title="จำนวนเต็ม 1–99"><button data-plus="${m.id}" aria-label="เพิ่มจำนวน ${escapeHtml(m.name)}">+</button></div></div>`).join('')||'<p class="muted">เลือกเมนูที่ชอบเพื่อเริ่มสั่ง</p>';
  $('#cart-items').querySelectorAll('[data-quantity]').forEach(input=>{
   input.oninput=()=>{
    if(input.checkValidity())cart.get(Number(input.dataset.quantity)).quantity=input.valueAsNumber;
    updateCartSummary();
   };
   input.onchange=()=>input.reportValidity();
  });
  $('#cart-items').querySelectorAll('button').forEach(b=>b.onclick=()=>{
   const id=Number(b.dataset.minus||b.dataset.plus),m=cart.get(id);
   m.quantity+=b.dataset.minus?-1:1;
   if(!m.quantity)cart.delete(id);else m.quantity=Math.min(m.quantity,99);
   drawCart();
  });
  updateCartSummary();
 }
 async function load(){const d=await api('/api/v1/menu-items?size=8&page='+state.page+'&sort='+$('#sort').value);menus=d.content;category='ทั้งหมด';const categories=['ทั้งหมด',...new Set(menus.map(m=>m.category).filter(Boolean))];$('#categories').innerHTML=categories.map(c=>`<button class="${c===category?'active':''}">${escapeHtml(c)}</button>`).join('');$('#categories').querySelectorAll('button').forEach(b=>b.onclick=()=>{category=b.textContent;$('#categories .active')?.classList.remove('active');b.classList.add('active');draw();});draw();pager(d);}
 $('#sort').onchange=()=>{state.page=0;load().catch(e=>toast(e.message));};bindPager(state,load);
 $('#checkout').onclick=async()=>{if([...$('#cart-items').querySelectorAll('[data-quantity]')].some(input=>!input.reportValidity()))return;const b=$('#checkout');b.disabled=true;$('#checkout-error').textContent='';try{const o=await api('/api/v1/orders',{method:'POST',body:JSON.stringify({items:[...cart.values()].map(m=>({menuItemId:m.id,quantity:m.quantity}))})});
  localStorage.setItem('queue-token-'+o.id,o.queueToken);localStorage.setItem('last-order',String(o.id));location.href='/queue/'+o.id;
 }catch(e){$('#checkout-error').textContent=e.message+' หากเครือข่ายขาดหลังส่ง ออเดอร์อาจถูกสร้างแล้ว โปรดตรวจสอบกับพนักงานก่อนสั่งซ้ำ';b.disabled=false;}};
 await load();
}
function keyBytes(base64){return Uint8Array.from(atob((base64+'='.repeat((4-base64.length%4)%4)).replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));}
async function queuePage(){
 const id=location.pathname.split('/').pop(),token=localStorage.getItem('queue-token-'+id),headers={'X-Queue-Token':token||''};let order,busy=false;
 if(!token){$('#queue-error').textContent='เครื่องนี้ไม่มีสิทธิ์ติดตามคิว กรุณาใช้เครื่องที่สั่งอาหารหรือติดต่อพนักงาน';$('#subscribe').disabled=true;return;}
 async function load(){order=await api('/api/v1/orders/'+id,{headers});const q=order.queue;$('#queue-number').textContent=String(q.queueNumber).padStart(3,'0');$('#queue-date').textContent='วันที่ '+queueDateText(q.queueDate);$('#queue-status').textContent=labels[q.status];$('#queue-status').className='status '+q.status;$('#queue-help').textContent=({WAITING:'รับออเดอร์แล้ว เราจะเริ่มปรุงตามลำดับคิว',PREPARING:'กำลังเตรียมมื้ออร่อยของคุณ',READY:'เชิญรับอาหารที่จุดรับของได้เลย',COMPLETED:'ขอบคุณที่ใช้บริการ แล้วพบกันมื้อหน้า',CANCELLED:'ออเดอร์นี้ถูกยกเลิกแล้ว'})[q.status];
 const steps=['WAITING','PREPARING','READY','COMPLETED'];document.querySelectorAll('.steps li').forEach(l=>l.classList.toggle('active',q.status!=='CANCELLED'&&steps.indexOf(l.dataset.state)<=steps.indexOf(q.status)));
 $('#order-items').innerHTML=order.items.map(i=>`<div class="cart-row"><span>${escapeHtml(i.menuItemName)} × ${i.quantity}</span><strong>${money(i.subtotal)}</strong></div>`).join('');$('#order-total').textContent=money(order.totalAmount);$('#edit-order').hidden=q.status!=='WAITING';$('#cancel-order').hidden=!['WAITING','PREPARING'].includes(q.status);$('#subscribe').hidden=order.pushEnabled;$('#unsubscribe').hidden=!order.pushEnabled;$('#subscribe').disabled=['COMPLETED','CANCELLED'].includes(q.status);
 if(order.pushEnabled)$('#push-status').textContent='เปิดแจ้งเตือนสำหรับออเดอร์นี้แล้ว';$('#queue-error').textContent='';}
 $('#subscribe').onclick=async()=>{const b=$('#subscribe');b.disabled=true;try{
 if(!window.isSecureContext||!('serviceWorker'in navigator)||!('PushManager'in window))throw Error('เครื่องนี้ไม่รองรับ Push ใช้หน้าติดตามคิวแทนได้');
 const permission=await Notification.requestPermission();if(permission!=='granted')throw Error('ยังไม่ได้อนุญาตแจ้งเตือน คุณยังดูสถานะบนหน้านี้ได้');
 await navigator.serviceWorker.register('/sw.js');const registration=await navigator.serviceWorker.ready;const key=await api('/api/v1/push/public-key');const subscription=await registration.pushManager.getSubscription()||await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:keyBytes(key.publicKey)});
 await api('/api/v1/orders/'+id+'/push-subscription',{method:'PUT',headers,body:JSON.stringify(subscription)});await load();toast('เปิดรับแจ้งเตือนแล้ว');
 }catch(e){$('#push-status').textContent=e.message;b.disabled=false;}};
 $('#unsubscribe').onclick=async()=>{try{await api('/api/v1/orders/'+id+'/push-subscription',{method:'DELETE',headers});await load();$('#push-status').textContent='ปิดแจ้งเตือนเฉพาะออเดอร์นี้แล้ว';}catch(e){toast(e.message);}};
 $('#cancel-order').onclick=async()=>{if(!confirm('ยกเลิกออเดอร์นี้ใช่ไหม?'))return;try{await api('/api/v1/queues/'+id+'/cancel',{method:'PATCH',headers});await load();}catch(e){toast(e.message);}};
 $('#edit-order').onclick=()=>{const dialog=document.createElement('dialog');dialog.innerHTML='<h2>แก้จำนวนอาหาร</h2><p class="muted small">ราคาออเดอร์จะคำนวณใหม่จากราคาเมนูปัจจุบัน</p><form>'+order.items.map(i=>`<label class="edit-line">${escapeHtml(i.menuItemName)}<input type="number" min="0" max="99" value="${i.quantity}" data-id="${i.menuItemId}" required></label>`).join('')+'<p class="error"></p><div class="actions"><button type="button" data-close>ยกเลิก</button><button class="primary">บันทึก</button></div></form>';document.body.append(dialog);dialog.showModal();dialog.querySelector('[data-close]').onclick=()=>dialog.close();dialog.onclose=()=>dialog.remove();dialog.querySelector('form').onsubmit=async e=>{e.preventDefault();const items=[...dialog.querySelectorAll('input')].map(i=>({menuItemId:Number(i.dataset.id),quantity:Number(i.value)})).filter(i=>i.quantity>0);try{await api('/api/v1/orders/'+id,{method:'PUT',headers,body:JSON.stringify({items})});dialog.close();await load();}catch(err){dialog.querySelector('.error').textContent=err.message;}};};
 await load();setInterval(async()=>{if(document.hidden||busy||!order||['COMPLETED','CANCELLED'].includes(order.queue.status))return;busy=true;try{await load();}catch(e){$('#queue-error').textContent=e.message;}finally{busy=false;}},5000);
}
async function staffPage(){const state={page:0,status:''};let busy=false;
 async function load(){const d=await api('/api/v1/orders?size=12&page='+state.page+(state.status?'&queueStatus='+state.status:''));$('#staff-orders').innerHTML=d.content.map(o=>`<article class="panel order-card"><div class="order-heading"><h2>#${String(o.queue.queueNumber).padStart(3,'0')}</h2><span class="status ${o.queue.status}">${labels[o.queue.status]}</span></div><p class="muted small">วันที่คิว ${escapeHtml(queueDateText(o.queue.queueDate))} · ออเดอร์ ${o.id} · ${new Date(o.createdAt+'Z').toLocaleTimeString('th-TH',{hour:'2-digit',minute:'2-digit'})} · ${o.pushEnabled?'เปิด Push':'ไม่ได้เปิด Push'}</p><ul>${o.items.map(i=>`<li>${escapeHtml(i.menuItemName)} × ${i.quantity}</li>`).join('')}</ul><div class="total"><span>รวม</span><strong>${money(o.totalAmount)}</strong></div><div class="actions">${['WAITING','PREPARING','READY'].includes(o.queue.status)?`<button class="primary" data-advance="${o.id}">${({WAITING:'เริ่มปรุง',PREPARING:'อาหารพร้อมแล้ว',READY:'รับอาหารแล้ว'})[o.queue.status]}</button>`:''}${['WAITING','PREPARING'].includes(o.queue.status)?`<button data-cancel="${o.id}" class="danger">ยกเลิก</button>`:''}${o.queue.status==='WAITING'?`<button data-delete="${o.id}" class="danger">ลบ</button>`:''}<button data-log="${o.id}">ผลแจ้งเตือน</button><button data-detail="${o.id}">รายละเอียด</button></div></article>`).join('')||'<div class="empty">ยังไม่มีออเดอร์ในสถานะนี้</div>';pager(d);
 $('#staff-orders').querySelectorAll('button').forEach(b=>b.onclick=async()=>{b.disabled=true;try{const id=b.dataset.advance||b.dataset.cancel||b.dataset.delete||b.dataset.log||b.dataset.detail;
 if(b.dataset.advance)await api('/api/v1/queues/'+id+'/advance',{method:'PATCH'});
 if(b.dataset.cancel&&confirm('ยกเลิกออเดอร์นี้?'))await api('/api/v1/queues/'+id+'/cancel',{method:'PATCH'});
 if(b.dataset.delete&&confirm('ลบออเดอร์และคิวนี้ถาวร?'))await api('/api/v1/orders/'+id,{method:'DELETE'});
 if(b.dataset.detail){const o=d.content.find(x=>x.id===Number(id));alert(o.items.map(i=>`${i.menuItemName} × ${i.quantity} = ${money(i.subtotal)}`).join('\n')+'\nรวม '+money(o.totalAmount));}
 if(b.dataset.log){const logs=await api('/api/v1/notifications?queueId='+id);$('#logs-content').innerHTML=logs.map(l=>`<div class="log-row"><b>${escapeHtml(l.deliveryStatus)}</b> ${l.httpStatus?'HTTP '+l.httpStatus:''}<br>${escapeHtml(l.message)}<p class="small muted">ACCEPTED = ผู้ให้บริการรับคำขอ ยังไม่ยืนยันการแสดงบนเครื่อง</p></div>`).join('')||'<p>ยังไม่มีประวัติส่ง อาจยังไม่ READY หรือไม่ได้เปิด Push</p>';$('#logs-dialog').showModal();}
 await load();}catch(e){toast(e.message);}finally{b.disabled=false;}});}
 $('#status-filters').querySelectorAll('button').forEach(b=>b.onclick=()=>{state.status=b.dataset.status;state.page=0;$('#status-filters .active')?.classList.remove('active');b.classList.add('active');load().catch(e=>toast(e.message));});$('#refresh').onclick=()=>load().catch(e=>toast(e.message));bindPager(state,load);await load();setInterval(async()=>{if(document.hidden||busy)return;busy=true;try{await load();}catch(e){toast(e.message);}finally{busy=false;}},10000);
}
async function staffMenuPage(){
 const state={page:0};let menus=[],previewUrl=null,busy=false;
 const dialog=$('#menu-dialog'),form=$('#menu-form'),fileInput=$('#menu-image'),preview=$('#menu-image-preview'),deleteButton=$('#delete-menu');
 function releasePreview(){if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}}
 function selectedFile(){
  const file=fileInput.files[0];
  if(file&&(!['image/jpeg','image/png'].includes(file.type)||file.size>2*1024*1024))throw Error('เลือกภาพ JPG หรือ PNG ขนาดไม่เกิน 2 MB');
  return file;
 }
 function edit(m){
  releasePreview();form.reset();form.elements.id.value=m?.id||'';
  if(m){for(const k of ['name','category','price','prepTimeMinutes'])form.elements[k].value=m[k]??'';form.elements.isAvailable.checked=m.isAvailable;}
  preview.hidden=!m;if(m)preview.src=foodPhoto(m);else preview.removeAttribute('src');
  deleteButton.hidden=!m;$('#form-title').textContent=m?'แก้ไขเมนู':'เพิ่มเมนู';$('#form-error').textContent='';dialog.showModal();
 }
 fileInput.onchange=()=>{
  releasePreview();$('#form-error').textContent='';
  try{const file=selectedFile();if(file){previewUrl=URL.createObjectURL(file);preview.src=previewUrl;preview.hidden=false;}else{const current=menus.find(m=>m.id===Number(form.elements.id.value));preview.hidden=!current;if(current)preview.src=foodPhoto(current);}}
  catch(e){fileInput.value='';$('#form-error').textContent=e.message;const current=menus.find(m=>m.id===Number(form.elements.id.value));preview.hidden=!current;if(current)preview.src=foodPhoto(current);}
 };
 dialog.addEventListener('close',releasePreview);
 async function load(){
  const d=await api('/api/v1/menu-items?includeUnavailable=true&size=12&page='+state.page);menus=d.content;
  $('#admin-menu').innerHTML=menus.map(m=>menuCard(m,true)).join('')||'<p class="empty">ยังไม่มีเมนู เพิ่มเมนูแรกได้เลย</p>';pager(d);
  $('#admin-menu').querySelectorAll('[data-edit]').forEach(b=>b.onclick=()=>edit(menus.find(m=>m.id===Number(b.dataset.edit))));
 }
 function setBusy(value){busy=value;form.querySelectorAll('button').forEach(b=>b.disabled=value);}
 deleteButton.onclick=async()=>{
  const id=form.elements.id.value;if(!id||busy||!confirm('ลบเมนูนี้ถาวรใช่ไหม?'))return;
  setBusy(true);$('#form-error').textContent='';
  try{await api('/api/v1/menu-items/'+id,{method:'DELETE'});dialog.close();if(menus.length===1&&state.page>0)state.page--;await load();toast('ลบเมนูแล้ว');}
  catch(e){$('#form-error').textContent=e.message;}finally{setBusy(false);}
 };
 $('#new-menu').onclick=()=>edit(null);
 form.onsubmit=async e=>{
  e.preventDefault();if(busy)return;$('#form-error').textContent='';
  const id=form.elements.id.value,body={name:form.elements.name.value.trim(),category:form.elements.category.value.trim()||null,price:Number(form.elements.price.value),prepTimeMinutes:form.elements.prepTimeMinutes.value?Number(form.elements.prepTimeMinutes.value):null,isAvailable:form.elements.isAvailable.checked};
  let file;try{file=selectedFile();}catch(err){$('#form-error').textContent=err.message;return;}
  setBusy(true);
  try{
   if(file){const data=new FormData();data.append('menu',new Blob([JSON.stringify(body)],{type:'application/json'}));data.append('file',file);await api('/api/v1/menu-items'+(id?'/'+id:'')+'/with-image',{method:id?'PUT':'POST',body:data});}
   else await api('/api/v1/menu-items'+(id?'/'+id:''),{method:id?'PUT':'POST',body:JSON.stringify(body)});
   dialog.close();await load();toast('บันทึกเมนูแล้ว');
  }catch(err){$('#form-error').textContent=err.message;}finally{setBusy(false);}
 };
 bindPager(state,load);await load();
}
document.addEventListener('DOMContentLoaded',()=>{const action={menu:menuPage,queue:queuePage,staff:staffPage,'staff-menu':staffMenuPage}[document.body.dataset.page];if(action)action().catch(e=>toast(e.message));});
