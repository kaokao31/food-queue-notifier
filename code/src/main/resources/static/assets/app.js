(function (root) {
 'use strict';
 if(document.body.dataset.page==='staff-menu'){
  const {$,api,toast}=root.CoreUI;
  const editor=root.StaffMenuUI.createEditor({request:api,notify:toast,renderer:root.MenuUI.createMenuRenderer(root.CoreUI),photo:root.MenuUI.foodPhoto,
   elements:{form:$('#menu-form'),dialog:$('#menu-dialog'),file:$('#menu-image'),preview:$('#menu-image-preview'),newMenu:$('#new-menu'),deleteMenu:$('#delete-menu'),title:$('#form-title'),formError:$('#form-error'),error:$('#menu-error'),grid:$('#admin-menu'),prev:$('#prev'),next:$('#next'),page:$('#page-info'),close:$('#close-menu-dialog')}});
  root.addEventListener('pagehide',()=>editor.stop(),{once:true});void editor.load();return;
 }

 if(document.body.dataset.page==='staff'){
  const {$,api,escapeHtml,money,labels}=root.CoreUI;
  const staff=root.StaffQueueUI.createStaffQueue({request:api,escapeHtml,money,labels,
   elements:{refresh:$('#refresh'),filters:$('#status-filters'),list:$('#staff-orders'),prev:$('#prev'),next:$('#next'),page:$('#page-info'),error:$('#staff-error'),dialog:$('#staff-detail-dialog'),detail:$('#staff-detail-content'),close:$('#close-staff-detail')}});
  root.addEventListener('pagehide',()=>staff.stop(),{once:true});void staff.load();return;
 }

 if(document.body.dataset.page==='queue'){
  const { $, api, escapeHtml, money, labels }=root.CoreUI;
  const id=location.pathname.split('/').filter(Boolean).at(-1);
  let tracker;
  const actions=root.QueueUI.createActions({id,getToken:()=>localStorage.getItem('queue-token-'+id),request:api,escapeHtml,money,confirmCancel:message=>root.confirm(message),
   onBusy:busy=>{if(busy)tracker?.stop();else startTracking();},
   elements:{edit:$('#edit-order'),cancel:$('#cancel-order'),dialog:$('#edit-order-dialog'),fields:$('#edit-order-fields'),save:$('#save-order'),close:$('#close-edit-order'),error:$('#order-action-error'),dialogError:$('#edit-order-error')}
  });
  function startTracking(){
   tracker?.stop();
   tracker=root.QueueUI.createTracker({id,getToken:()=>localStorage.getItem('queue-token-'+id),request:api,
    onError:message=>{$('#queue-error').textContent=message;},onOrder:order=>actions.update(order),
    render:order=>root.QueueUI.renderOrder(order,{escapeHtml,money,labels,elements:{number:$('#queue-number'),status:$('#queue-status'),date:$('#queue-date'),help:$('#queue-help'),items:$('#order-items'),total:$('#order-total'),steps:document.querySelectorAll('[data-state]')}})
   });void tracker.load();
  }
  root.addEventListener('pagehide',()=>tracker?.stop(),{once:true});startTracking();return;
 }
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
