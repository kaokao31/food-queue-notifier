(function (root) {
 'use strict';
 const $ = selector => document.querySelector(selector);
 const labels = Object.freeze({WAITING:'รอคิว', PREPARING:'กำลังปรุง', READY:'พร้อมรับอาหาร', COMPLETED:'รับอาหารเรียบร้อย', CANCELLED:'ยกเลิกแล้ว'});
 const money = value => '฿' + Number(value).toLocaleString('th-TH', {maximumFractionDigits:2});
 const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
 let csrf = null, csrfRequest = null;
 async function getCsrf() {
  if (csrf) return csrf;
  if (!csrfRequest) csrfRequest = (async () => {
   const response = await fetch('/api/v1/csrf', {cache:'no-store'});
   if (!response.ok) throw new Error('ระบบสิทธิ์การส่งคำขอยังไม่พร้อมใช้งาน');
   const data = await response.json();
   if (typeof data.headerName !== 'string' || !data.headerName || typeof data.token !== 'string' || !data.token) throw new Error('ข้อมูลสิทธิ์การส่งคำขอไม่ถูกต้อง');
   csrf = data; return data;
  })();
  try {return await csrfRequest;} finally {csrfRequest = null;}
 }
 async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = {...options.headers};
  if (options.body && !(typeof FormData !== 'undefined' && options.body instanceof FormData)) headers['Content-Type'] ??= 'application/json';
  if (!['GET','HEAD','OPTIONS'].includes(method)) {
   const security = await getCsrf(); headers[security.headerName] = security.token;
  }
  const response = await fetch(path, {...options, method, headers, cache:'no-store'});
  if (response.status === 403) csrf = null;
  if (response.status === 404 && path.startsWith('/api/v1/menu-items')) throw new Error('ระบบรายการเมนูยังไม่พร้อมใช้งาน');
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || 'ส่งคำขอไม่สำเร็จ (' + response.status + ')');
  if (response.status !== 204 && !data) throw new Error('อ่านข้อมูลจากระบบไม่สำเร็จ');
  return data;
 }
 function toast(message) {
  const element = $('#toast'); if (!element) return;
  element.textContent = message; element.style.display = 'block';
  setTimeout(() => {element.style.display = 'none';}, 4500);
 }
 function pager(data, busy = false) {
  $('#prev').disabled = busy || data.page === 0;
  $('#next').disabled = busy || data.page + 1 >= data.totalPages;
  $('#page-info').textContent = `หน้า ${data.page + 1} / ${Math.max(1, data.totalPages)}`;
 }
 root.CoreUI = Object.freeze({$, labels, money, escapeHtml, api, toast, pager});
})(globalThis);
