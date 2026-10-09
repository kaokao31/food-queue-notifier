(function(root){
 'use strict';
 function validateFile(file){
  if(file&&(!['image/jpeg','image/png'].includes(file.type)||file.size<1||file.size>2*1024*1024))throw Error('เลือกภาพ JPG หรือ PNG ขนาดไม่เกิน 2 MB');return file;
 }
 function requestBody(fields){
  const name=fields.name.value.trim(),category=fields.category.value.trim(),price=fields.price.value.trim(),prep=fields.prepTimeMinutes.value.trim();
  if(!name||name.length>100||category.length>50||!/^\d{1,8}(?:\.\d{1,2})?$/.test(price)||prep&&(!/^\d+$/.test(prep)||Number(prep)>240))throw Error('ตรวจชื่อเมนู ราคา และเวลาปรุงอีกครั้ง');
  return {name,category:category||null,price:Number(price),prepTimeMinutes:prep===''?null:Number(prep),isAvailable:fields.isAvailable.checked};
 }
 function multipart(body,file){const data=new FormData();data.append('menu',new Blob([JSON.stringify(body)],{type:'application/json'}));data.append('file',file);return data;}
 function createEditor({request,elements,renderer,photo,confirmDelete=message=>root.confirm(message),notify=()=>{},createUrl=file=>URL.createObjectURL(file),revokeUrl=url=>URL.revokeObjectURL(url),makeMultipart=multipart}){
  const {form,dialog,file,preview}=elements;const fields=form.elements;
  let page=0,data=null,busy=false,disposed=false,previewUrl=null,currentMenu=null;const listeners=[];
  function listen(node,event,handler){node.addEventListener(event,handler);listeners.push(()=>node.removeEventListener(event,handler));}
  function release(){if(previewUrl){revokeUrl(previewUrl);previewUrl=null;}}
  function showExisting(){preview.hidden=!currentMenu;if(currentMenu)preview.src=photo(currentMenu);else preview.removeAttribute('src');}
  function controls(){
   form.querySelectorAll('button,input').forEach(node=>node.disabled=busy);
   elements.newMenu.disabled=busy;elements.prev.disabled=busy||page===0;elements.next.disabled=busy||!data||page+1>=data.totalPages;
   elements.grid.querySelectorAll('[data-edit]').forEach(node=>node.disabled=busy);
  }
  async function load(){
   if(busy||disposed)return;busy=true;controls();elements.error.textContent='';
   try{
    const result=await request(`/api/v1/menu-items?includeUnavailable=true&size=12&page=${page}&sort=id,asc`);if(disposed)return;
    if(!Array.isArray(result.content)||!Number.isInteger(result.totalPages)||result.totalPages<0)throw Error('ข้อมูลรายการเมนูไม่ถูกต้อง');
    data=result;elements.grid.innerHTML=result.content.map(menu=>renderer(menu,true,true)).join('')||'<p class="empty">ยังไม่มีเมนู</p>';elements.page.textContent=`หน้า ${page+1} / ${Math.max(1,result.totalPages)}`;
   }catch(error){if(!disposed){data=null;elements.grid.innerHTML='';elements.error.textContent=error.message;}}
   finally{if(!disposed){busy=false;controls();}}
  }
  function edit(menu){
   if(busy||disposed)return;release();currentMenu=menu;form.reset();file.value='';fields.id.value=menu?String(menu.id):'';
   if(menu){for(const key of ['name','category','price','prepTimeMinutes'])fields[key].value=String(menu[key]??'');fields.isAvailable.checked=menu.isAvailable;}
   elements.deleteMenu.hidden=!menu;elements.title.textContent=menu?'แก้ไขเมนู':'เพิ่มเมนู';elements.formError.textContent='';showExisting();dialog.showModal();
  }
  function changeFile(){
   release();elements.formError.textContent='';try{const selected=validateFile(file.files[0]);if(selected){previewUrl=createUrl(selected);preview.src=previewUrl;preview.hidden=false;}else showExisting();}
   catch(error){file.value='';elements.formError.textContent=error.message;showExisting();}
  }
  async function save(){
   if(busy||disposed)return;elements.formError.textContent='';if(!form.reportValidity())return;
   const id=fields.id.value;if(id&&!/^[1-9][0-9]*$/.test(id)){elements.formError.textContent='รหัสเมนูไม่ถูกต้อง';return;}
   let body,selected;try{body=requestBody(fields);selected=validateFile(file.files[0]);}catch(error){elements.formError.textContent=error.message;return;}
   busy=true;controls();let success=false;
   try{
    const url='/api/v1/menu-items'+(id?'/'+id:'')+(selected?'/with-image':'');
    await request(url,{method:id?'PUT':'POST',body:selected?makeMultipart(body,selected):JSON.stringify(body)});
    if(!disposed){dialog.close();release();page=0;success=true;notify('บันทึกเมนูแล้ว');}
   }catch(error){if(!disposed)elements.formError.textContent=error.message;}
   finally{if(!disposed){busy=false;controls();}}
   if(success)await load();
  }
  async function remove(){
   const id=fields.id.value;if(busy||disposed||!/^[1-9][0-9]*$/.test(id)||!confirmDelete('ลบเมนูนี้ถาวรหรือไม่?'))return;
   busy=true;controls();elements.formError.textContent='';let success=false;
   try{await request('/api/v1/menu-items/'+id,{method:'DELETE'});if(!disposed){dialog.close();release();page=0;success=true;notify('ลบเมนูแล้ว');}}
   catch(error){if(!disposed)elements.formError.textContent=error.message;}
   finally{if(!disposed){busy=false;controls();}}
   if(success)await load();
  }
  listen(elements.newMenu,'click',()=>edit(null));listen(elements.grid,'click',event=>{const button=event.target.closest('[data-edit]');if(button&&elements.grid.contains(button)){const menu=data?.content.find(menu=>String(menu.id)===button.dataset.edit);if(menu)edit(menu);}});
  listen(file,'change',changeFile);listen(form,'submit',event=>{event.preventDefault();void save();});listen(elements.deleteMenu,'click',()=>void remove());
  listen(elements.close,'click',()=>{if(!busy)dialog.close();});listen(dialog,'cancel',event=>{if(busy)event.preventDefault();});listen(dialog,'close',release);
  listen(elements.prev,'click',()=>{if(!busy&&page>0){page--;void load();}});listen(elements.next,'click',()=>{if(!busy&&data&&page+1<data.totalPages){page++;void load();}});controls();
  return {load,edit,save,remove,changeFile,stop(){disposed=true;release();listeners.forEach(remove=>remove());}};
 }
 root.StaffMenuUI=Object.freeze({createEditor,requestBody,validateFile,multipart});
})(globalThis);
