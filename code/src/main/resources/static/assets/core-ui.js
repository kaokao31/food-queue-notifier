(function (root) {
 'use strict';
 const $ = selector => document.querySelector(selector);
 const labels = Object.freeze({WAITING:'รอคิว', PREPARING:'กำลังปรุง', READY:'พร้อมรับอาหาร', COMPLETED:'รับอาหารเรียบร้อย', CANCELLED:'ยกเลิกแล้ว'});
 const money = value => '฿' + Number(value).toLocaleString('th-TH', {maximumFractionDigits:2});
 const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
 async function api(path, options = {}) {
  if (options.method && options.method !== 'GET') throw new Error('ยังไม่เปิดใช้การส่งข้อมูล');
  const response = await fetch(path, {...options, cache:'no-store'});
  if (response.status === 404) throw new Error('ระบบรายการเมนูยังไม่พร้อมใช้งาน');
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || 'โหลดข้อมูลไม่สำเร็จ (' + response.status + ')');
  if (!data) throw new Error('อ่านข้อมูลจากระบบไม่สำเร็จ');
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
