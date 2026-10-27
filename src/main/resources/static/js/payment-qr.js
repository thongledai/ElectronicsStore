'use strict';

/**
 * Hộp thoại thanh toán chuyển khoản VietQR.
 *   PaymentQr.open(orderId, { onClose })
 * - Dữ liệu (số tiền, mã tham chiếu, QR) lấy hoàn toàn từ backend: GET /orders/{id}/payment.
 * - Trình duyệt KHÔNG có quyền xác nhận thanh toán: chỉ hỏi lại trạng thái do backend ghi nhận
 *   sau khi webhook ngân hàng/SePay được xác minh.
 */
(function () {
    const POLL_MS = 4000;
    let modalEl = null;
    let modal = null;
    let pollTimer = null;
    let tickTimer = null;
    let currentId = null;
    let onCloseCb = null;
    let remaining = 0;

    const money = n => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
    const esc = s => String(s ?? '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    function ensureModal() {
        if (modalEl) return;
        modalEl = document.createElement('div');
        modalEl.className = 'modal fade';
        modalEl.id = 'paymentQrModal';
        modalEl.tabIndex = -1;
        modalEl.setAttribute('aria-hidden', 'true');
        modalEl.innerHTML = `
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title">Thanh toán chuyển khoản</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
              </div>
              <div class="modal-body" id="paymentQrBody"></div>
            </div>
          </div>`;
        document.body.appendChild(modalEl);
        modal = new bootstrap.Modal(modalEl);
        modalEl.addEventListener('hidden.bs.modal', () => {
            stopTimers();
            const cb = onCloseCb;
            onCloseCb = null;
            if (typeof cb === 'function') cb();
        });
    }

    function stopTimers() {
        clearInterval(pollTimer); pollTimer = null;
        clearInterval(tickTimer); tickTimer = null;
    }

    async function fetchPayment() {
        const res = await fetch('/orders/' + encodeURIComponent(currentId) + '/payment', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        });
        let json = null;
        try { json = await res.json(); } catch (e) { /* không có body */ }
        if (!res.ok || !json || json.success === false) {
            throw new Error((json && json.message) || 'Không tải được thông tin thanh toán.');
        }
        return json.data;
    }

    function copyBtn(value) {
        return `<button type="button" class="btn btn-sm btn-outline-secondary ms-2 js-copy" data-v="${esc(value)}">Sao chép</button>`;
    }

    function row(label, valueHtml) {
        return `<div class="d-flex justify-content-between align-items-center py-1 border-bottom">
                  <span class="text-muted small">${label}</span>
                  <span class="fw-semibold text-end">${valueHtml}</span></div>`;
    }

    function render(p) {
        const body = document.getElementById('paymentQrBody');
        const st = p.paymentStatus;

        if (st === 'PAID') {
            stopTimers();
            body.innerHTML = `
              <div class="text-center py-3">
                <div class="display-6 text-success mb-2"><i class="fas fa-circle-check"></i></div>
                <h5 class="mb-1">Đã nhận được thanh toán</h5>
                <p class="text-muted mb-3">Cảm ơn bạn! Đơn ${esc(p.orderCode)} (${money(p.amount)}) đã được xác nhận thanh toán
                và đang chờ cửa hàng xử lý.</p>
                <a href="/customer/orders" class="btn btn-brand">Xem đơn hàng của tôi</a>
              </div>`;
            return;
        }
        if (st === 'EXPIRED' || st === 'CANCELLED') {
            stopTimers();
            body.innerHTML = `
              <div class="text-center py-3">
                <h5 class="mb-2">${st === 'EXPIRED' ? 'Đơn đã quá hạn thanh toán' : 'Đơn đã bị hủy'}</h5>
                <p class="text-muted">Vui lòng không chuyển khoản cho đơn này. Nếu đã lỡ chuyển, hãy liên hệ cửa hàng kèm mã
                <strong>${esc(p.transferContent)}</strong> để được hỗ trợ.</p>
                <a href="/customer/products" class="btn btn-brand">Tiếp tục mua sắm</a>
              </div>`;
            return;
        }
        if (st === 'NEEDS_RECONCILIATION') {
            stopTimers();
            body.innerHTML = `
              <div class="text-center py-3">
                <h5 class="mb-2">Đang đối soát thanh toán</h5>
                <p class="text-muted">Cửa hàng đã nhận được giao dịch nhưng chưa khớp tự động (ví dụ số tiền khác tổng đơn).
                Nhân viên sẽ liên hệ bạn. Mã tham chiếu: <strong>${esc(p.transferContent)}</strong></p>
                <a href="/customer/orders" class="btn btn-brand">Xem đơn hàng của tôi</a>
              </div>`;
            return;
        }

        // UNPAID: hiển thị QR + thông tin chuyển khoản
        const account = p.accountName ? row('Chủ tài khoản', esc(p.accountName)) : '';
        body.innerHTML = `
          <div class="text-center">
            ${p.qrImageUrl
                ? `<img src="${esc(p.qrImageUrl)}" alt="Mã VietQR thanh toán đơn hàng" class="img-fluid mb-2" style="max-height:360px"
                       onerror="this.replaceWith(Object.assign(document.createElement('p'),{className:'text-danger small',textContent:'Không tải được ảnh QR. Vui lòng chuyển khoản thủ công theo thông tin bên dưới.'}))">`
                : '<p class="text-danger">Đơn đã hết hạn thanh toán.</p>'}
            <div class="small text-muted mb-2">Mở app ngân hàng, quét mã QR hoặc chuyển khoản thủ công.</div>
          </div>
          ${row('Ngân hàng', esc(p.bankName))}
          ${row('Số tài khoản', esc(p.accountNumber) + copyBtn(p.accountNumber))}
          ${account}
          ${row('Số tiền', `<span class="text-danger">${money(p.amount)}</span>` + copyBtn(String(Math.round(Number(p.amount)))))}
          ${row('Nội dung CK', `<span class="font-monospace">${esc(p.transferContent)}</span>` + copyBtn(p.transferContent))}
          <div class="alert alert-warning small mt-3 mb-2">
            Vui lòng chuyển <strong>đúng số tiền</strong> và <strong>đúng nội dung</strong> để hệ thống tự xác nhận.
          </div>
          <div class="d-flex justify-content-between align-items-center small">
            <span><span class="spinner-border spinner-border-sm me-1"></span>Đang chờ nhận tiền...</span>
            <span>Còn lại: <strong id="paymentQrCountdown">--:--</strong></span>
          </div>`;

        body.querySelectorAll('.js-copy').forEach(b => b.addEventListener('click', () => {
            const v = b.getAttribute('data-v') || '';
            (navigator.clipboard ? navigator.clipboard.writeText(v) : Promise.reject())
                .then(() => { b.textContent = 'Đã chép'; setTimeout(() => (b.textContent = 'Sao chép'), 1500); })
                .catch(() => { /* trình duyệt chặn clipboard: khách tự chép */ });
        }));

        remaining = Number(p.secondsRemaining) || 0;
        updateCountdown();
        clearInterval(tickTimer);
        tickTimer = setInterval(() => { remaining = Math.max(0, remaining - 1); updateCountdown(); }, 1000);
    }

    function updateCountdown() {
        const el = document.getElementById('paymentQrCountdown');
        if (!el) return;
        const m = String(Math.floor(remaining / 60)).padStart(2, '0');
        const s = String(remaining % 60).padStart(2, '0');
        el.textContent = m + ':' + s;
    }

    async function refresh() {
        try {
            render(await fetchPayment());
        } catch (e) {
            const body = document.getElementById('paymentQrBody');
            if (body && !body.innerHTML.trim()) {
                body.innerHTML = `<p class="text-danger mb-0">${esc(e.message)}</p>`;
            }
        }
    }

    window.PaymentQr = {
        open(orderId, opts) {
            ensureModal();
            stopTimers();
            currentId = orderId;
            onCloseCb = opts && opts.onClose;
            document.getElementById('paymentQrBody').innerHTML =
                '<div class="text-center py-4"><div class="spinner-border"></div></div>';
            modal.show();
            refresh();
            // Chỉ hỏi lại trạng thái do backend ghi nhận; không bao giờ tự đánh dấu đã thanh toán ở trình duyệt
            pollTimer = setInterval(refresh, POLL_MS);
        }
    };
})();
