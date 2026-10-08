(function (root) {
 'use strict';
 function createCheckout({basket, request, button, error, saveToken, navigate}) {
  let busy = false, created = false;
  function refresh() {button.disabled = busy || created || !basket.isValid();}
  async function submit() {
   if (busy || created) return false;
   if (!basket.isValid()) {error.textContent = 'กรุณาตรวจจำนวนอาหารในตะกร้า';refresh();return false;}
   busy = true; error.textContent = ''; refresh();
   let order;
   try {
    const items = basket.getItems().map(item => ({menuItemId:item.menuItemId, quantity:item.quantity}));
    order = await request('/api/v1/orders', {method:'POST', body:JSON.stringify({items})});
    if (!Number.isSafeInteger(order?.id) || order.id <= 0 || typeof order.queueToken !== 'string' || !order.queueToken) throw new Error('ข้อมูลออเดอร์จากระบบไม่ครบถ้วน');
    created = true;
    saveToken(order.id, order.queueToken);
    navigate('/queue/' + order.id);
    return true;
   } catch (failure) {
    error.textContent = created
     ? 'สร้างบิล #' + order.id + ' แล้ว แต่เก็บสิทธิ์ติดตามหรือเปิดหน้าคิวไม่สำเร็จ กรุณาติดต่อพนักงานและอย่าสั่งซ้ำ'
     : failure.message + ' หากส่งคำขอไปแล้ว ออเดอร์อาจถูกสร้าง กรุณาตรวจสอบกับพนักงานก่อนสั่งซ้ำ';
    return false;
   } finally {busy = false; refresh();}
  }
  button.onclick = () => {void submit();}; refresh();
  return Object.freeze({refresh, submit});
 }
 root.CheckoutUI = Object.freeze({createCheckout});
})(globalThis);
