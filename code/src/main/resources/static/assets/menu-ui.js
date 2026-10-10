(function (root) {
  'use strict';

  function foodPhoto(menu) {
    if (menu.imageUrl) return menu.imageUrl;
    const fallback = menu.category === 'เครื่องดื่ม' ? 'limeade-v2.jpg'
      : menu.category === 'เส้น' ? 'pad-thai-v2.webp' : 'egg-fried-rice-v2.avif';
    return '/assets/food/' + fallback;
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


  function createMenuBrowser({ request, elements, renderer, escapeHtml, onError = () => {}, interactive = false, onRendered = () => {} }) {
    const state = {page:0, category:'ทั้งหมด', menus:[], totalPages:0, busy:false};
    let revision = 0;
    function pager() {
      elements.prev.disabled = state.busy || state.page === 0;
      elements.next.disabled = state.busy || state.page + 1 >= state.totalPages;
      elements.pageInfo.textContent = `หน้า ${state.page + 1} / ${Math.max(1, state.totalPages)}`;
    }
    function draw() {
      const visible = state.category === 'ทั้งหมด' ? state.menus : state.menus.filter(menu => menu.category === state.category);
      elements.grid.innerHTML = visible.map(menu => renderer(menu, false, interactive)).join('') || '<p class="empty">ไม่มีเมนูในหมวดนี้</p>';
      onRendered(visible);
    }
    function categories() {
      const names = ['ทั้งหมด', ...new Set(state.menus.map(menu => menu.category).filter(Boolean))];
      if (!names.includes(state.category)) state.category = 'ทั้งหมด';
      elements.categories.innerHTML = names.map(name => `<button class="${name === state.category ? 'active' : ''}">${escapeHtml(name)}</button>`).join('');
      elements.categories.querySelectorAll('button').forEach(button => {
        button.onclick = () => {state.category = button.textContent; categories(); draw();};
      });
    }
    async function load() {
      const current = ++revision;
      state.busy = true; elements.sort.disabled = true; pager();
      elements.grid.textContent = 'กำลังโหลดเมนู…';
      try {
        const data = await request('/api/v1/menu-items?size=8&page=' + state.page + '&sort=' + encodeURIComponent(elements.sort.value));
        if (current !== revision) return;
        if (!Array.isArray(data.content) || !Number.isInteger(data.page) || data.page < 0 || !Number.isInteger(data.totalPages) || data.totalPages < 0) throw new Error('รูปแบบข้อมูลเมนูไม่ถูกต้อง');
        state.page = data.page; state.totalPages = data.totalPages; state.menus = data.content;
        categories(); draw();
      } catch (error) {
        if (current !== revision) return;
        state.menus = []; state.totalPages = 0;
        elements.categories.innerHTML = ''; elements.grid.textContent = error.message;
        onError(error.message);
      } finally {
        if (current === revision) {state.busy = false; elements.sort.disabled = false; pager();}
      }
    }
    elements.prev.onclick = () => {if (!state.busy && state.page > 0) {state.page--; void load();}};
    elements.next.onclick = () => {if (!state.busy && state.page + 1 < state.totalPages) {state.page++; void load();}};
    elements.sort.onchange = () => {state.page = 0; void load();};
    return Object.freeze({load, getMenus: () => [...state.menus]});
  }

  root.MenuUI = Object.freeze({ foodPhoto, createMenuRenderer, createMenuBrowser });
})(globalThis);
