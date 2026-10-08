(function (root) {
 'use strict';
 function createBasketModel() {
  const lines = new Map();
  function quantity(value) {
   if (!Number.isInteger(value) || value < 1 || value > 99) throw new Error('จำนวนอาหารต้องเป็นจำนวนเต็ม 1–99');
  }
  function add(menu, amount) {
   quantity(amount);
   if (menu.isAvailable === false) throw new Error('เมนูนี้ปิดขายแล้ว');
   if (!Number.isSafeInteger(menu.id) || menu.id <= 0 || !Number.isFinite(Number(menu.price)) || Number(menu.price) < 0) throw new Error('ข้อมูลเมนูไม่ถูกต้อง');
   const existing = lines.get(menu.id), total = (existing?.quantity || 0) + amount;
   quantity(total);
   if (existing) existing.quantity = total;
   else lines.set(menu.id, {menuItemId:menu.id, name:menu.name, price:Number(menu.price), cents:Math.round(Number(menu.price) * 100), quantity:total});
  }
  function setQuantity(id, amount) {
   quantity(amount); const line = lines.get(id);
   if (!line) throw new Error('ไม่พบรายการในตะกร้า');
   line.quantity = amount;
  }
  function remove(id) {lines.delete(id);}
  const getItems = () => [...lines.values()].map(({cents, ...line}) => ({...line}));
  const total = () => [...lines.values()].reduce((sum, line) => sum + line.cents * line.quantity, 0) / 100;
  return Object.freeze({add, setQuantity, remove, getItems, total});
 }
 function createBasket({elements, escapeHtml, money, onError = () => {}, onChange = () => {}}) {
  const model = createBasketModel();
  function summary() {
   elements.total.textContent = money(model.total());
   elements.checkout.disabled = true;
   onChange(isValid());
  }
  function draw() {
   elements.items.innerHTML = model.getItems().map(line => {
    const id = escapeHtml(line.menuItemId), name = escapeHtml(line.name);
    return `<div class="cart-row"><div><h3>${name}</h3><span class="small muted">${money(line.price)}</span><button data-remove="${id}" aria-label="ลบ ${name} จากตะกร้า">ลบ</button></div><div class="qty"><button data-minus="${id}" aria-label="ลดจำนวน ${name}">−</button><input type="number" min="1" max="99" step="1" value="${line.quantity}" required inputmode="numeric" data-quantity="${id}" aria-label="จำนวนในตะกร้า ${name}" title="จำนวนเต็ม 1–99"><button data-plus="${id}" aria-label="เพิ่มจำนวน ${name}">+</button></div></div>`;
   }).join('') || '<p class="muted">เลือกเมนูที่ชอบเพื่อเริ่มสั่ง</p>';
   elements.items.querySelectorAll('[data-quantity]').forEach(input => {
    input.oninput = () => {
     if (input.checkValidity()) model.setQuantity(Number(input.dataset.quantity), input.valueAsNumber);
     summary();
    };
    input.onchange = () => input.reportValidity();
   });
   elements.items.querySelectorAll('button').forEach(button => {button.onclick = () => {
    const id = Number(button.dataset.remove || button.dataset.minus || button.dataset.plus);
    const line = model.getItems().find(item => item.menuItemId === id);
    try {
     if (button.dataset.remove || (button.dataset.minus && line.quantity === 1)) model.remove(id);
     else model.setQuantity(id, line.quantity + (button.dataset.minus ? -1 : 1));
     draw();
    } catch (error) {onError(error.message);}
   };});
   summary();
  }
  function bindMenu(menus) {
   elements.grid.querySelectorAll('[data-add]').forEach(button => {button.onclick = () => {
    const menu = menus.find(item => item.id === Number(button.dataset.add));
    const input = button.closest('.menu-card').querySelector('[data-add-quantity]');
    if (!input.reportValidity()) return;
    try {model.add(menu, input.valueAsNumber); draw();} catch (error) {onError(error.message);}
   };});
  }
  const isValid = () => model.getItems().length > 0 && [...elements.items.querySelectorAll('[data-quantity]')].every(input => input.checkValidity());
  draw();
  return Object.freeze({bindMenu, getItems:model.getItems, isValid});
 }
 root.BasketUI = Object.freeze({createBasketModel, createBasket});
})(globalThis);
