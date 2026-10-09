(function (root) {
 'use strict';
 if (document.body.dataset.page !== 'menu') return;
 const { $, api, escapeHtml, money, toast, labels } = root.CoreUI;
 root.HistoryUI.createHistory({request:api, getStorage:()=>localStorage, escapeHtml, money, labels,
  elements:{open:$('#open-queue-history'),close:$('#close-queue-history'),dialog:$('#queue-history-dialog'),list:$('#queue-history-list'),prev:$('#history-prev'),next:$('#history-next'),page:$('#history-page')}
 });
 let checkout;
 const basket = root.BasketUI.createBasket({escapeHtml, money, onError:toast, onChange:() => checkout?.refresh(),
  elements:{items:$('#cart-items'), total:$('#cart-total'), checkout:$('#checkout'), grid:$('#menu-grid')}
 });
 checkout = root.CheckoutUI.createCheckout({basket, request:api, button:$('#checkout'), error:$('#checkout-error'),
  saveToken:(id, token) => {localStorage.setItem('queue-token-' + id, token); localStorage.setItem('last-order', String(id));},
  navigate:url => {location.href = url;}
 });
 const browser = root.MenuUI.createMenuBrowser({
  request:api, escapeHtml, onError:toast, interactive:true, onRendered:basket.bindMenu,
  renderer:root.MenuUI.createMenuRenderer(root.CoreUI),
  elements:{grid:$('#menu-grid'), categories:$('#categories'), sort:$('#sort'), prev:$('#prev'), next:$('#next'), pageInfo:$('#page-info')}
 });
 void browser.load();
})(globalThis);
