'use strict';

(function () {
    const $ = id => document.getElementById(id);
    const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0);
    const esc = value => String(value ?? '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    let currentTopUpId = null;
    let pollTimer = null;

    async function api(method, url, body) {
        const response = await fetch(url, {
            method,
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}) },
            body: body ? JSON.stringify(body) : undefined
        });
        let json = null;
        try { json = await response.json(); } catch (e) { /* no JSON body */ }
        if (response.status === 401) {
            location.href = '/login?error=unauthorized';
            throw new Error('UNAUTHENTICATED');
        }
        if (!response.ok || !json || json.success === false) {
            throw new Error((json && json.message) || 'Có lỗi xảy ra, vui lòng thử lại.');
        }
        return json.data;
    }

    function notify(message, type) {
        if (typeof showToast === 'function') showToast(type === 'danger' ? 'Lỗi' : 'Thông báo', message, type || 'info');
        else alert(message);
    }

    async function loadWallet() {
        const summary = await api('GET', '/api/wallet');
        $('walletBalance').textContent = money(summary.balance);
        const rows = summary.transactions || [];
        $('walletTransactions').innerHTML = rows.length ? rows.map(t => {
            const type = t.type === 'WALLET_TOP_UP' ? 'Nạp ví'
                : (t.type === 'ORDER_PAYMENT' ? 'Thanh toán đơn hàng' : (t.moneyIn ? 'Tiền vào' : 'Tiền ra'));
            const sign = t.moneyIn ? '+' : '−';
            const cls = t.moneyIn ? 'text-success' : 'text-danger';
            const time = t.createdAt ? new Date(t.createdAt).toLocaleString('vi-VN') : '—';
            return '<tr><td class="small">' + esc(time) + '</td><td>' + esc(type) + '</td><td class="small font-monospace">' +
                esc(t.reference || '—') + '</td><td class="text-end fw-semibold ' + cls + '">' + sign + money(t.amount) + '</td></tr>';
        }).join('') : '<tr><td colspan="4" class="text-muted small">Chưa có giao dịch ví.</td></tr>';
    }

    function stopPolling() {
        if (pollTimer) clearInterval(pollTimer);
        pollTimer = null;
    }

    function renderTopUp(data) {
        const status = data.status;
        let statusText = status === 'PAID' ? 'Đã nạp tiền thành công'
            : status === 'AMOUNT_MISMATCH' ? 'Số tiền nhận được chưa khớp'
                : status === 'EXPIRED' ? 'Yêu cầu nạp tiền đã hết hạn' : 'Đang chờ nhận tiền';
        const statusClass = status === 'PAID' ? 'text-success'
            : (status === 'AMOUNT_MISMATCH' || status === 'EXPIRED' ? 'text-danger' : 'text-primary');
        let qr = '';
        if (data.qrImageUrl) {
            qr = '<div class="text-center mb-3"><img src="' + esc(data.qrImageUrl) +
                '" class="img-fluid" alt="VietQR nạp ví" style="max-height:300px"></div>';
        }
        let account = '<div class="summary-row"><span>Ngân hàng</span><strong>' + esc(data.bankName) + '</strong></div>' +
            '<div class="summary-row"><span>Số tài khoản</span><strong>' + esc(data.accountNumber) + '</strong></div>';
        if (data.accountName) account += '<div class="summary-row"><span>Chủ tài khoản</span><strong>' + esc(data.accountName) + '</strong></div>';
        const statusNote = status === 'PAID'
            ? '<div class="alert alert-success small mt-3 mb-0">Đã cộng ' + money(data.receivedAmount) + ' vào ví.</div>'
            : status === 'AMOUNT_MISMATCH'
                ? '<div class="alert alert-warning small mt-3 mb-0">Hệ thống chưa cộng tiền vì số tiền chuyển chưa đúng yêu cầu. Yêu cầu vẫn có thể nhận giao dịch đúng trước khi hết hạn.</div>'
                : status === 'EXPIRED'
                    ? '<div class="alert alert-danger small mt-3 mb-0">Không chuyển tiền theo mã này nữa. Nếu đã chuyển, vui lòng liên hệ cửa hàng để đối soát.</div>'
                    : '<div class="alert alert-warning small mt-3 mb-0">Tự nhập đúng ' + money(data.requestedAmount) +
                        ' trên ứng dụng ngân hàng. Không được sửa nội dung chuyển khoản.</div>';
        $('topUpPanel').innerHTML = qr +
            '<div class="d-flex justify-content-between align-items-center mb-2"><span>Trạng thái</span><strong class="' + statusClass + '">' + esc(statusText) + '</strong></div>' +
            '<div class="summary-row"><span>Số tiền yêu cầu</span><strong>' + money(data.requestedAmount) + '</strong></div>' +
            '<div class="summary-row"><span>Nội dung chuyển khoản</span><strong class="font-monospace">' + esc(data.transferContent) + '</strong></div>' +
            account + statusNote;
        if (status === 'PAID' || status === 'EXPIRED') stopPolling();
    }

    async function refreshTopUp() {
        if (!currentTopUpId) return;
        try {
            const data = await api('GET', '/api/wallet/top-ups/' + encodeURIComponent(currentTopUpId));
            renderTopUp(data);
            if (data.status === 'PAID') await loadWallet();
        } catch (e) {
            if (e.message !== 'UNAUTHENTICATED') notify(e.message, 'danger');
        }
    }

    async function createTopUp() {
        const amount = Number($('topUpAmount').value);
        if (!Number.isFinite(amount) || amount <= 0 || !Number.isInteger(amount)) {
            notify('Vui lòng nhập số tiền VND là số nguyên lớn hơn 0.', 'warning');
            return;
        }
        $('createTopUpBtn').disabled = true;
        try {
            const data = await api('POST', '/api/wallet/top-ups', { amount });
            currentTopUpId = data.id;
            stopPolling();
            renderTopUp(data);
            if (data.status === 'PENDING' || data.status === 'AMOUNT_MISMATCH') {
                pollTimer = setInterval(refreshTopUp, 4000);
            }
        } catch (e) {
            if (e.message !== 'UNAUTHENTICATED') notify(e.message, 'danger');
        } finally {
            $('createTopUpBtn').disabled = false;
        }
    }

    document.addEventListener('DOMContentLoaded', async () => {
        $('createTopUpBtn').addEventListener('click', createTopUp);
        $('refreshWalletBtn').addEventListener('click', () =>
            loadWallet().catch(e => { if (e.message !== 'UNAUTHENTICATED') notify(e.message, 'danger'); }));
        try { await loadWallet(); }
        catch (e) { if (e.message !== 'UNAUTHENTICATED') notify(e.message, 'danger'); }
    });
})();
