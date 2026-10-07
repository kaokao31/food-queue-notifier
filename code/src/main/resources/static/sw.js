self.addEventListener("push", (event) => {
    const data = event.data ? event.data.json() : {};

    const title = data.title || "แจ้งเตือนคิวอาหาร";

    const options = {
        body: data.body || "สถานะออเดอร์ของคุณมีการอัปเดต",
        tag: data.tag || "queue-notification",
        data: {
            url: data.url || "/"
        }
    };

    event.waitUntil(
        self.registration.showNotification(title, options)
    );
});

self.addEventListener("notificationclick", (event) => {
    event.notification.close();

    // เปิดได้เฉพาะหน้าภายในเว็บไซต์ของเรา
    const target = new URL(
        event.notification.data?.url || "/",
        self.location.origin
    );

    if (target.origin !== self.location.origin) {
        return;
    }

    event.waitUntil(
        clients.openWindow(target.href)
    );
});