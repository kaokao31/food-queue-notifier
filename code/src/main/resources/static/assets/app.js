(function (root) {
 'use strict';
 if (document.body.dataset.page !== 'menu') return;
 const { $, api, escapeHtml, money, toast } = root.CoreUI;
 const basket = root.BasketUI.createBasket({escapeHtml, money, onError:toast,
  elements:{items:$('#cart-items'), total:$('#cart-total'), checkout:$('#checkout'), grid:$('#menu-grid')}
 });
 const browser = root.MenuUI.createMenuBrowser({
  request:api, escapeHtml, onError:toast, interactive:true, onRendered:basket.bindMenu,
  renderer:root.MenuUI.createMenuRenderer(root.CoreUI),
  elements:{grid:$('#menu-grid'), categories:$('#categories'), sort:$('#sort'), prev:$('#prev'), next:$('#next'), pageInfo:$('#page-info')}
 });
 void browser.load();
})(globalThis);
