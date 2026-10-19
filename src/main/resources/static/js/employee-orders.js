'use strict';

/* ---------- Helpers ---------- */
const notify = (t, m, type) => (typeof showToast === 'function') ? showToast(t, m, type || 'info') : alert(m);

// Nếu bật CSRF, layout cần có <meta name="_csrf"> và <meta name="_csrf_header">
function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    return token && header ? { [header]: token } : {};
}

async function postJson(url) {
    const res = await fetch(url, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json', ...csrfHeaders() }
    });
    if (res.status === 401) { location.href = '/login?error=unauthorized'; throw new Error('UNAUTHENTICATED'); }
    if (res.status === 403) throw new Error('Bạn không có quyền thực hiện thao tác này.');
    let json = null;
    try { json = await res.json(); } catch (e) { /* không có body */ }
    if (!res.ok || (json && json.success === false)) {
        throw new Error((json && json.message) || 'Có lỗi xảy ra, vui lòng thử lại.');
    }
    return json;
}

/* ---------- Nhãn trạng thái ---------- */
// Khóa phải khớp với order_status.name trong DB. Class màu: chỉnh theo CSS dự án.
const ORDER_STATUS = {
    NOT_PROCESSED: { text: 'Chờ xác nhận',   css: 'status-warning' },
    CONFIRMED:     { text: 'Đã xác nhận',    css: 'status-info' },
    SHIPPING:      { text: 'Đang giao hàng', css: 'status-info' },
    DELIVERED:     { text: 'Đã giao hàng',   css: 'status-success' },
    CANCELLED:     { text: 'Đã hủy',         css: 'status-danger' },
    RETURNED:      { text: 'Đã hoàn hàng',   css: 'status-danger' }
};
const STATUS_CLASSES = ['status-neutral', 'status-warning', 'status-info', 'status-success', 'status-danger'];

function paintOrderStatus(el, status) {
    const s = ORDER_STATUS[status] || { text: status || 'Đang cập nhật', css: 'status-neutral' };
    el.textContent = s.text;
    el.classList.remove(...STATUS_CLASSES);
    el.classList.add(s.css);
    el.dataset.orderStatus = status;
}

function paintAll() {
    document.querySelectorAll('.order-status-label').forEach(el => paintOrderStatus(el, el.dataset.orderStatus));
    document.querySelectorAll('.ghn-status-label').forEach(el => {
        const v = el.dataset.ghnStatus;
        el.textContent = v ? v : 'Chưa có thông tin';
    });
}

/* ---------- Xác nhận đơn ---------- */
let currentRow = null;
let confirming = false;

function openConfirm(row) {
    currentRow = row;
    document.getElementById('confirmOrderCode').textContent = row.dataset.orderCode;
    document.getElementById('confirmOrderCustomer').textContent = row.dataset.customer;
    document.getElementById('confirmOrderTotal').textContent = row.dataset.total + ' ₫';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('confirmOrderModal')).show();
}

async function submitConfirm() {
    if (confirming || !currentRow) return;
    const row = currentRow;
    const btn = document.getElementById('confirmOrderSubmit');
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('confirmOrderModal'));

    confirming = true;
    btn.disabled = true;
    btn.textContent = 'Đang xác nhận...';
    try {
        const json = await postJson(`/api/employee/orders/${encodeURIComponent(row.dataset.orderId)}/confirm`);
        modal.hide();
        notify('Thành công', (json && json.message) || 'Đã xác nhận đơn hàng', 'success');
        afterConfirmed(row);
    } catch (e) {
        if (e.message !== 'UNAUTHENTICATED') {
            notify('Không xác nhận được', e.message, 'danger');
            // Đơn có thể đã được người khác xử lý: tải lại để thấy trạng thái mới nhất
            modal.hide();
            setTimeout(() => location.reload(), 1500);
        }
    } finally {
        confirming = false;
        btn.disabled = false;
        btn.textContent = 'Xác nhận đơn';
        currentRow = null;
    }
}

function afterConfirmed(row) {
    const filter = new URLSearchParams(location.search).get('status');
    if (filter === 'NOT_PROCESSED') {
        // Đang xem danh sách chờ xác nhận: đơn này đã rời khỏi danh sách
        row.remove();
        if (!document.querySelector('#ordersTable tbody tr[data-order-id]')) location.reload();
        return;
    }
    paintOrderStatus(row.querySelector('.order-status-label'), 'CONFIRMED');
    row.querySelector('.js-confirm-order')?.remove();
}

/* ---------- Khởi tạo ---------- */
document.addEventListener('DOMContentLoaded', () => {
    paintAll();
    document.getElementById('ordersTable')?.addEventListener('click', ev => {
        const btn = ev.target.closest('.js-confirm-order');
        if (btn) openConfirm(btn.closest('tr'));
    });
    document.getElementById('confirmOrderSubmit')?.addEventListener('click', submitConfirm);
});