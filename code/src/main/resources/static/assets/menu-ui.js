(function (root) {
  'use strict';
  const images = Object.freeze({
    'ข้าวกะเพราไก่': 'basil-chicken-v2.webp',
    'ข้าวผัดไข่': 'egg-fried-rice-v2.avif',
    'ข้าวไก่ทอด': 'crispy-chicken-rice-v2.jpg',
    'ชาไทยเย็น': 'thai-tea-v2.jpg',
    'น้ำมะนาว': 'limeade-v2.jpg',
    'ผัดไทย': 'pad-thai-v2.webp'
  });

  function foodPhoto(menu) {
    if (menu.imageUrl) return menu.imageUrl;
    const fallback = menu.category === 'เครื่องดื่ม' ? 'limeade-v2.jpg'
      : menu.category === 'เส้น' ? 'pad-thai-v2.webp' : 'egg-fried-rice-v2.avif';
    return '/assets/food/' + (images[menu.name] || fallback);
  }

  function createMenuRenderer({ escapeHtml, money }) {
    if (typeof escapeHtml !== 'function' || typeof money !== 'function') {
      throw new TypeError('Menu renderer requires escapeHtml and money helpers');
    }
    return function menuCard(menu, admin = false, interactive = false) {
      const id = escapeHtml(menu.id), name = escapeHtml(menu.name);
      const disabled = interactive && (admin || menu.isAvailable !== false) ? '' : ' disabled';
      const controls = admin
        ? `<button data-edit="${id}" aria-label="แก้ไข ${name}"${disabled}>แก้ไข</button>`
        : `<div class="menu-add"><input type="number" min="1" max="99" step="1" value="1" required inputmode="numeric" data-add-quantity="${id}" aria-label="จำนวน ${name}" title="จำนวนเต็ม 1–99"${disabled}><button data-add="${id}" aria-label="เพิ่ม ${name} ลงตะกร้า"${disabled}>+ เพิ่ม</button></div>`;
      return `<article class="menu-card"><div class="food-art ${menu.category === 'เครื่องดื่ม' ? 'drink' : ''}"><img src="${escapeHtml(foodPhoto(menu))}" alt="${name}" loading="lazy" decoding="async" width="480" height="320"></div><div class="menu-content"><p class="muted small">${escapeHtml(menu.category || 'เมนูแนะนำ')} · ${escapeHtml(menu.prepTimeMinutes ?? '—')} นาที</p><h3>${name}</h3>${admin ? `<span class="status">${menu.isAvailable ? 'เปิดขาย' : 'ปิดขาย'}</span>` : ''}<div class="menu-bottom"><strong class="price">${money(menu.price)}</strong>${controls}</div></div></article>`;
    };
  }

  root.MenuUI = Object.freeze({ foodPhoto, createMenuRenderer });
})(globalThis);
