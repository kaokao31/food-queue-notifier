"use strict";
const qrForm=document.querySelector("#qr-form"),qrInput=document.querySelector("#qr-url"),qrError=document.querySelector("#qr-error");
if(location.protocol==="https:")qrInput.value=location.origin+"/";
qrForm.onsubmit=async e=>{e.preventDefault();qrError.textContent="";try{const value=qrInput.value.trim(),response=await fetch("/staff/qr.png?url="+encodeURIComponent(value),{cache:"no-store"});if(!response.ok){const error=await response.json().catch(()=>({message:"สร้าง QR ไม่สำเร็จ"}));throw Error(error.message);}const image=document.querySelector("#qr-image");if(image.dataset.objectUrl)URL.revokeObjectURL(image.dataset.objectUrl);const blobUrl=URL.createObjectURL(await response.blob());image.src=blobUrl;image.dataset.objectUrl=blobUrl;document.querySelector("#qr-link").textContent=value;document.querySelector("#qr-card").hidden=false;}catch(error){qrError.textContent=error.message;}};
document.querySelector("#print-qr").onclick=()=>window.print();
