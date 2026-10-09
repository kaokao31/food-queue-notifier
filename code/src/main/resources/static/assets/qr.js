(function(root){
 'use strict';
 function validateUrl(value){
  if(!value||value.length>1000)throw Error('ใส่ URL HTTPS ของหน้าเมนู');let url;try{url=new URL(value);}catch{throw Error('URL ไม่ถูกต้อง');}
  if(url.protocol!=='https:'||url.username||url.password||url.search||url.hash||url.pathname!=='/')throw Error('ใช้ URL HTTPS หน้าเมนู โดยไม่ใส่รหัสหรือพารามิเตอร์');return url.href;
 }
 function createQr({elements,request,createUrl=blob=>URL.createObjectURL(blob),revokeUrl=url=>URL.revokeObjectURL(url),print=()=>root.print(),decode=async image=>{if(image.decode)await image.decode();}}){
  let busy=false,disposed=false,objectUrl=null,ready=false;
  function release(){if(objectUrl){revokeUrl(objectUrl);objectUrl=null;}ready=false;elements.print.disabled=true;elements.card.hidden=true;elements.image.removeAttribute('src');}
  async function generate(){
   if(busy||disposed)return;release();elements.error.textContent='';let url;try{url=validateUrl(elements.input.value.trim());}catch(error){elements.error.textContent=error.message;return;}
   busy=true;elements.generate.disabled=true;elements.input.disabled=true;
   try{
    const blob=await request('/staff/qr.png?url='+encodeURIComponent(url));if(disposed)return;
    objectUrl=createUrl(blob);elements.image.src=objectUrl;await decode(elements.image);if(disposed)return;
    elements.link.textContent=url;elements.card.hidden=false;ready=true;elements.print.disabled=false;
   }catch(error){if(!disposed){release();elements.error.textContent=error.message;}}
   finally{if(!disposed){busy=false;elements.generate.disabled=false;elements.input.disabled=false;}}
  }
  const submit=event=>{event.preventDefault();void generate();},changed=()=>{if(!busy)release();},printing=()=>{if(ready&&!busy&&!disposed)print();};
  elements.form.addEventListener('submit',submit);elements.input.addEventListener('input',changed);elements.print.addEventListener('click',printing);elements.generate.disabled=false;
  return {generate,stop(){disposed=true;release();elements.form.removeEventListener('submit',submit);elements.input.removeEventListener('input',changed);elements.print.removeEventListener('click',printing);}};
 }
 root.QrUI=Object.freeze({validateUrl,createQr});
 if(typeof document!=='undefined'&&document.body.dataset.page==='qr'){
  const $=selector=>document.querySelector(selector);const elements={form:$('#qr-form'),input:$('#qr-url'),error:$('#qr-error'),generate:$('#generate-qr'),print:$('#print-qr'),image:$('#qr-image'),link:$('#qr-link'),card:$('#qr-card')};
  if(location.protocol==='https:')elements.input.value=location.origin+'/';
  const qr=createQr({elements,request:async url=>{const response=await fetch(url,{cache:'no-store'});if(!response.ok){const error=await response.json().catch(()=>null);throw Error(error?.message||'สร้าง QR ไม่สำเร็จ');}if(!response.headers.get('Content-Type')?.startsWith('image/png'))throw Error('ระบบไม่ได้ส่งภาพ QR');return response.blob();}});
  root.addEventListener('pagehide',()=>qr.stop(),{once:true});
 }
})(globalThis);
