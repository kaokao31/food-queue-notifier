(function (root) {
 'use strict';
 if (document.body.dataset.page !== 'menu') return;
 const { $, api, escapeHtml, toast } = root.CoreUI;
 const browser = root.MenuUI.createMenuBrowser({
  request:api, escapeHtml, onError:toast,
  renderer:root.MenuUI.createMenuRenderer(root.CoreUI),
  elements:{grid:$('#menu-grid'), categories:$('#categories'), sort:$('#sort'), prev:$('#prev'), next:$('#next'), pageInfo:$('#page-info')}
 });
 void browser.load();
})(globalThis);
