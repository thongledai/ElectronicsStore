'use strict';

const itemIds = JSON.parse(sessionStorage.getItem('checkoutItemIds') || '[]');

/* ---------- State ---------- */
let lines = [];              // các món đã chọn (từ POST /checkout)
let baseSubtotal = null;     // tạm tính từ /checkout
let baseDiscount = null;     // giảm giá từ /checkout
let baseTotal = null;        // tổng thanh toán từ /checkout
let preview = null;          // OrderPreviewDTO từ /preview-order
let status = 'idle';         // 'idle' | 'loading' | 'ready' | 'error'
let lastKey = null;          // "provinceId|wardCode|voucherCode" của lần preview thành công
let placing = false;
let appliedVoucher = null;   // Dữ liệu voucher đã áp dụng thành công

let seq = 0;
let timer = null;
let controller = null;
let wardsController = null;
const DEBOUNCE_MS = 400;

/* ---------- Helpers ---------- */
const $ = id => document.getElementById(id);
const money = n => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const notify = (t, m, type) => (typeof showToast === 'function') ? showToast(t, m, type || 'info') : alert(m);
const isAbort = e => e && e.name === 'AbortError';
const PHONE_RE = /^(0|\+84)\d{9}$/;

async function api(method, url, body, signal) {
    const res = await fetch(url, {
        method,
        signal,
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}) },
        body: body ? JSON.stringify(body) : undefined
    });
    if (res.status === 401) {
        location.href = '/login?error=unauthorized';
        throw new Error('UNAUTHENTICATED');
    }
    let json = null;
    try { json = await res.json(); } catch (e) { /* không có body */ }
    if (!res.ok || (json && json.success === false)) {
        throw new Error((json && json.message) || 'Có lỗi xảy ra, vui lòng thử lại.');
    }
    return json;
}

function fail(e) {
    if (isAbort(e) || e.message === 'UNAUTHENTICATED') return;
    notify('Lỗi', e.message, 'danger');
}

/* ---------- Form ---------- */
function selectedPayment() {
    return document.querySelector('input[name="paymentMethod"]:checked');
}

/** Đã nhập đủ thông tin địa chỉ giao hàng chưa? */
function isAddressComplete() {
    const toName = $('toName') ? $('toName').value.trim() : '';
    const phone = $('phone') ? $('phone').value.trim() : '';
    const province = $('province') ? $('province').value : '';
    const ward = $('ward') ? $('ward').value : '';
    const street = $('street') ? $('street').value.trim() : '';

    return toName !== ''
        && PHONE_RE.test(phone)
        && province !== ''
        && ward !== ''
        && street !== '';
}

/** Cập nhật trạng thái ô nhập voucher (chỉ cho phép nhập khi đã điền đủ địa chỉ) */
function updateVoucherInputState() {
    const addressDone = isAddressComplete();
    const input = $('voucherInput');
    const btn = $('applyVoucherBtn');
    const hint = $('voucherAddressHint');

    if (addressDone) {
        if (input) {
            input.disabled = false;
            input.placeholder = 'Nhập mã voucher';
        }
        if (btn) btn.disabled = false;
        if (hint) hint.classList.add('d-none');
    } else {
        if (input) {
            input.disabled = true;
            input.placeholder = 'Vui lòng nhập địa chỉ nhận hàng trước...';
            input.value = '';
        }
        if (btn) btn.disabled = true;
        if (hint) hint.classList.remove('d-none');

        // Nếu địa chỉ bị thiếu hoặc xóa sau khi đã áp dụng voucher -> gỡ voucher
        if (appliedVoucher) {
            removeVoucher(false);
        }
    }
}

/** Đã đủ thông tin để gọi /preview-order và /orders chưa? */
function isFormComplete() {
    return isAddressComplete() && selectedPayment() !== null;
}

/** Body dùng chung cho /preview-order và /orders. Không gửi phí ship: server tự tính. */
function buildBody() {
    const pOpt = $('province').selectedOptions[0];
    const wOpt = $('ward').selectedOptions[0];
    return {
        cartItemIds: itemIds,
        toName: $('toName').value.trim(),
        phone: $('phone').value.trim(),
        provinceId: Number($('province').value),
        wardCode: $('ward').value,
        provinceName: pOpt ? pOpt.textContent : '',
        wardName: wOpt ? wOpt.textContent : '',
        detailAddress: $('street').value.trim(),
        voucherCode: appliedVoucher ? appliedVoucher.code : null,
        paymentMethodId: Number(selectedPayment().value),
        note: $('note').value.trim() || null
    };
}

/* ---------- Bước 1: POST /checkout (danh sách món + tạm tính) ---------- */
async function loadCheckout() {
    try {
        const json = await api('POST', '/checkout', { cartItemIds: itemIds });
        const d = json.data || {};
        lines = (d.items || []).map(i => {
            const qty = Number(i.quantity ?? i.count ?? 1);
            const price = Number(i.unitPrice || 0);
            const origPrice = Number(i.originalPrice || price);
            const promoPrice = i.promotionalPrice != null ? Number(i.promotionalPrice) : null;
            const hasDiscount = promoPrice !== null && promoPrice > 0 && promoPrice < origPrice;
            return {
                name: i.productName || '',
                variant: i.styleValues || '',
                image: i.productImageUrl || '/images/placeholder.webp',
                price: price,
                originalPrice: origPrice,
                hasDiscount: hasDiscount,
                qty: qty,
                subtotal: Number(i.subtotal ?? price * qty),
                unavailable: i.isAvailable === false
            };
        });
        baseSubtotal = Number(d.subtotal) || 0;
        baseDiscount = Number(d.discount) || 0;
        baseTotal = Number(d.total) || 0;
    } catch (e) {
        fail(e);
        if (e.message !== 'UNAUTHENTICATED') setTimeout(() => { location.href = '/customer/cart'; }, 1500);
        return;
    }
    render();
}

/* ---------- Bước 2: POST /preview-order (phí ship + ngày giao) ---------- */
function resetPreview() {
    clearTimeout(timer);
    if (controller) controller.abort();
    controller = null;
    ++seq;
    preview = null;
    status = 'idle';
    lastKey = null;
    render();
}

function maybeRequestPreview() {
    const voucherKey = appliedVoucher ? appliedVoucher.code : '';
    const key = $('province').value + '|' + $('ward').value + '|' + voucherKey;

    // Chưa chọn đủ tỉnh + phường: không có phí ship
    if (!$('province').value || !$('ward').value) {
        resetPreview();
        return;
    }
    // Đổi sang phường khác mà form chưa đủ: bỏ phí ship cũ
    if (!isFormComplete()) {
        if (key !== lastKey) resetPreview();
        return;
    }
    // Địa chỉ và voucher không đổi và đã có phí ship: không gọi lại (đỡ gọi GHN)
    if (key === lastKey && status === 'ready') return;

    clearTimeout(timer);
    if (controller) controller.abort();
    const mySeq = ++seq;
    status = 'loading';
    render();
    timer = setTimeout(() => fetchPreview(mySeq, key), DEBOUNCE_MS);
}

async function fetchPreview(mySeq, key) {
    const c = new AbortController();
    controller = c;
    try {
        const json = await api('POST', '/preview-order', buildBody(), c.signal);
        if (mySeq !== seq) return;
        preview = json.data;
        lastKey = key;
        status = 'ready';

        // Đồng bộ voucher với kết quả từ preview-order
        if (preview.voucherCode && appliedVoucher) {
            if (preview.voucherDiscount != null) {
                appliedVoucher.discountAmount = Number(preview.voucherDiscount);
            }
        } else if (appliedVoucher && !preview.voucherCode) {
            appliedVoucher = null;
            $('voucherInputGroup').classList.remove('d-none');
            $('voucherAppliedBox').classList.add('d-none');
            updateVoucherInputState();
            notify('Cảnh báo', 'Mã giảm giá không áp dụng được cho đơn hàng này', 'warning');
        }
    } catch (e) {
        if (isAbort(e) || mySeq !== seq) return;
        fail(e);
        preview = null;
        lastKey = null;
        status = 'error';
    }
    render();
}

/* ---------- Hiển thị ---------- */
function renderItems() {
    const box = $('checkoutItems');
    if (!lines.length) {
        box.innerHTML = '<p class="text-muted small mb-0">Đang tải sản phẩm...</p>';
        return;
    }
    box.innerHTML = lines.map(l => `
      <div class="d-flex gap-3 align-items-center">
        <img src="${esc(l.image)}" alt="" width="64" height="64"
             class="rounded" style="object-fit:cover"
             onerror="this.onerror=null;this.src='/images/placeholder.webp';">
        <div class="flex-grow-1">
          <div class="fw-semibold">${esc(l.name)}</div>
          ${l.variant ? `<div class="small text-muted">${esc(l.variant)}</div>` : ''}
          <div class="small d-flex align-items-baseline gap-1">
            <span class="fw-semibold ${l.hasDiscount ? 'text-danger' : ''}">${money(l.price)}</span>
            ${l.hasDiscount ? `<del class="text-muted small">${money(l.originalPrice)}</del>` : ''}
            <span class="text-muted">× ${l.qty}</span>
          </div>
          ${l.unavailable ? '<div class="small text-danger">Không đủ hàng hoặc ngừng bán</div>' : ''}
        </div>
        <div class="fw-bold">${money(l.subtotal)}</div>
      </div>`).join('');
}

function formatEta(v) {
    if (!v) return '—';
    // số (epoch giây) hoặc chuỗi ISO -> dd/mm/yyyy; chuỗi đã định dạng sẵn thì giữ nguyên
    const n = /^\d+$/.test(String(v)) ? Number(v) * (String(v).length <= 10 ? 1000 : 1) : Date.parse(v);
    if (!Number.isFinite(n)) return String(v);
    return new Date(n).toLocaleDateString('vi-VN');
}

function render() {
    renderItems();

    const ready = status === 'ready' && preview;
    const hasUnavailable = lines.some(l => l.unavailable);

    // 1. Tổng tiền hàng (subtotal - tính theo giá gốc)
    const currentSubtotal = ready ? preview.subtotal : baseSubtotal;
    $('subtotal').textContent = currentSubtotal !== null ? money(currentSubtotal) : '—';

    // 2. Phí vận chuyển
    $('shipping').textContent = {
        idle: 'Nhập đủ địa chỉ để tính',
        loading: 'Đang tính...',
        error: 'Không tính được',
        ready: ready ? money(preview.shippingFee) : '—'
    }[status];

    // 3. Giảm giá sản phẩm (từ giá khuyến mãi phân loại)
    const prodDiscount = (ready && preview)
        ? Math.max(0, (Number(preview.discount) || 0) - (Number(preview.voucherDiscount) || 0))
        : (Number(baseDiscount) || 0);
    $('discount').textContent = prodDiscount > 0 ? '−' + money(prodDiscount) : money(0);

    // 4. Voucher giảm giá (ưu tiên số tiền chính xác từ preview nếu có, hoặc từ kết quả API check)
    let vDiscount = 0;
    if (ready && preview && preview.voucherDiscount != null) {
        vDiscount = Number(preview.voucherDiscount) || 0;
    } else if (appliedVoucher) {
        vDiscount = Number(appliedVoucher.discountAmount) || 0;
    }

    const voucherRow = $('voucherDiscountRow');
    if (voucherRow) {
        if (appliedVoucher || vDiscount > 0) {
            voucherRow.classList.remove('d-none');
            $('voucherDiscount').textContent = vDiscount > 0 ? '−' + money(vDiscount) : money(0);
        } else {
            voucherRow.classList.add('d-none');
        }
    }

    // 5. Ngày giao dự kiến
    $('eta').textContent = ready ? formatEta(preview.expectedDeliveryTime) : '—';

    // 6. Tổng thanh toán = (Tổng tiền hàng - Giảm giá sản phẩm) + Phí vận chuyển - Giảm giá voucher
    const sub = Number(currentSubtotal) || 0;
    const goodsAfterDiscount = Math.max(0, sub - prodDiscount);
    const shipFee = (ready && preview && preview.shippingFee != null) ? Number(preview.shippingFee) : 0;
    const grandTotal = Math.max(0, goodsAfterDiscount + shipFee - vDiscount);

    if (currentSubtotal !== null) {
        $('total').textContent = money(grandTotal);
    } else {
        $('total').textContent = '—';
    }

    const warn = $('warn');
    if (status === 'error') {
        warn.hidden = false;
        warn.textContent = 'Không tính được phí vận chuyển. Vui lòng kiểm tra địa chỉ hoặc thử lại.';
    } else if (hasUnavailable) {
        warn.hidden = false;
        warn.textContent = 'Có sản phẩm không đủ hàng, vui lòng quay lại giỏ hàng để chỉnh.';
    } else {
        warn.hidden = true;
        warn.textContent = '';
    }

    $('placeOrderBtn').disabled = placing || !ready || hasUnavailable;
}

/* ---------- Dropdown địa chỉ ---------- */
function fillSelect(select, list, placeholder) {
    select.innerHTML = `<option value="">${placeholder}</option>` +
        list.map(o => `<option value="${esc(o.id)}">${esc(o.name)}</option>`).join('');
}

async function loadProvinces() {
    try {
        const json = await api('GET', '/address/provinces');
        fillSelect($('province'), json.data || [], '-- Chọn tỉnh/thành --');
    } catch (e) { fail(e); }
}

async function onProvinceChange() {
    const provinceId = $('province').value;
    const ward = $('ward');

    if (wardsController) wardsController.abort();
    wardsController = null;

    fillSelect(ward, [], '-- Chọn phường/xã --');
    ward.disabled = true;
    resetPreview();                         // đổi tỉnh thì phí ship cũ không còn đúng
    updateVoucherInputState();
    if (!provinceId) return;

    const c = new AbortController();
    wardsController = c;
    try {
        const json = await api('GET', '/address/wards?provinceId=' + encodeURIComponent(provinceId), null, c.signal);
        if (c !== wardsController) return;
        fillSelect(ward, json.data || [], '-- Chọn phường/xã --');
        ward.disabled = false;
        updateVoucherInputState();
    } catch (e) { fail(e); }
}

/* ---------- Phương thức thanh toán ---------- */
const PAYMENT_LABELS = {
    COD: 'Thanh toán khi nhận hàng (COD)',
    BANK_QR: 'Chuyển khoản QR (VietQR)',
    E_WALLET: 'Ví điện tử'
};

async function loadPaymentMethods() {
    try {
        const json = await api('GET', '/payment-methods');
        const list = json.data || [];
        $('paymentMethods').innerHTML = list.length
            ? list.map((p, idx) => `
              <label class="d-flex align-items-center gap-2">
                <input type="radio" class="form-check-input mt-0" name="paymentMethod"
                       value="${esc(p.id)}" ${idx === 0 ? 'checked' : ''}>
                <span>${esc(PAYMENT_LABELS[p.name] || p.name)}</span>
              </label>`).join('')
            : '<p class="text-danger small mb-0">Chưa có phương thức thanh toán.</p>';
    } catch (e) {
        $('paymentMethods').innerHTML = '<p class="text-danger small mb-0">Không tải được phương thức thanh toán.</p>';
        fail(e);
    }
}

/* ---------- Bước 3: Voucher ---------- */
async function applyVoucher() {
    if (!isAddressComplete()) {
        notify('Cảnh báo', 'Vui lòng nhập đầy đủ địa chỉ giao hàng trước khi áp dụng mã giảm giá.', 'warning');
        updateVoucherInputState();
        return;
    }

    const input = $('voucherInput');
    const msg = $('voucherMsg');
    const btn = $('applyVoucherBtn');
    if (!input || !btn) return;

    const code = input.value.trim().toUpperCase();
    if (!code) {
        if (msg) {
            msg.textContent = 'Vui lòng nhập mã giảm giá';
            msg.classList.remove('d-none');
        }
        return;
    }

    if (msg) {
        msg.classList.add('d-none');
        msg.textContent = '';
    }
    btn.disabled = true;
    const oldBtnText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang kiểm tra...';

    try {
        const payload = {
            code: code,
            cartItemIds: itemIds,
            shippingFee: (status === 'ready' && preview && preview.shippingFee != null) ? Number(preview.shippingFee) : null
        };
        const res = await api('POST', '/api/promotions/check', payload);
        const data = res.data;

        if (data && data.valid) {
            appliedVoucher = data;
            $('voucherInputGroup').classList.add('d-none');
            $('voucherAppliedBox').classList.remove('d-none');
            $('appliedVoucherCode').textContent = data.code;
            $('appliedVoucherDesc').textContent = data.description || (
                data.type === 'PRODUCT_PERCENT'
                    ? `Giảm ${data.discountPercent}% giá trị đơn hàng (tối đa ${money(data.maxDiscountAmount)})`
                    : `Giảm ${data.discountPercent}% phí vận chuyển`
            );
            input.value = '';
            notify('Thành công', 'Áp dụng mã giảm giá thành công!', 'success');
            render();
            if (isFormComplete()) {
                lastKey = null;
                maybeRequestPreview();
            }
        } else {
            if (msg) {
                msg.textContent = (data && data.message) || 'Mã giảm giá không hợp lệ';
                msg.classList.remove('d-none');
            }
        }
    } catch (e) {
        if (msg) {
            msg.textContent = e.message || 'Không thể kiểm tra mã giảm giá';
            msg.classList.remove('d-none');
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = oldBtnText;
        updateVoucherInputState();
        render();
    }
}

function removeVoucher(shouldNotify = true) {
    appliedVoucher = null;
    $('voucherInputGroup').classList.remove('d-none');
    $('voucherAppliedBox').classList.add('d-none');
    $('appliedVoucherCode').textContent = '';
    $('appliedVoucherDesc').textContent = '';
    const msg = $('voucherMsg');
    if (msg) {
        msg.classList.add('d-none');
        msg.textContent = '';
    }
    updateVoucherInputState();
    render();
    if (isFormComplete()) {
        lastKey = null;
        maybeRequestPreview();
    }
    if (shouldNotify) {
        notify('Thông báo', 'Đã bỏ mã giảm giá', 'info');
    }
}

/* ---------- Bước 4: POST /orders ---------- */
function validateForm() {
    if (!$('toName').value.trim()) return 'Vui lòng nhập họ tên người nhận';
    if (!PHONE_RE.test($('phone').value.trim())) return 'Số điện thoại không hợp lệ';
    if (!$('province').value) return 'Vui lòng chọn tỉnh/thành';
    if (!$('ward').value) return 'Vui lòng chọn phường/xã';
    if (!$('street').value.trim()) return 'Vui lòng nhập số nhà, tên đường';
    if (!selectedPayment()) return 'Vui lòng chọn phương thức thanh toán';
    if (status !== 'ready') return 'Vui lòng chờ tính xong phí vận chuyển';
    return null;
}

async function placeOrder() {
    if (placing) return;
    const error = validateForm();
    if (error) { notify('Thiếu thông tin', error, 'warning'); return; }

    placing = true;
    render();
    try {
        const json = await api('POST', '/orders', buildBody());
        sessionStorage.removeItem('checkoutItemIds');
        if (json.data.paymentMethod === 'BANK_QR' && window.PaymentQr) {
            // Đơn chuyển khoản: hiện QR (dữ liệu do backend cấp), xong thì về trang đơn hàng
            window.PaymentQr.open(json.data.id, { onClose: () => { location.href = '/customer/orders'; } });
        } else {
            $('orderRef').textContent = json.data.id;
            new bootstrap.Modal($('orderModal')).show();
        }
    } catch (e) {
        fail(e);
        placing = false;
        // Lỗi có thể do hết hàng hoặc giá/phí ship đổi: tải lại món và tính lại
        await loadCheckout();
        lastKey = null;
        maybeRequestPreview();
    }
}

/* ---------- Khởi tạo ---------- */
document.addEventListener('DOMContentLoaded', async () => {
    if (itemIds.length === 0) { location.href = '/customer/cart'; return; }

    $('province').addEventListener('change', onProvinceChange);
    $('ward').addEventListener('change', () => {
        maybeRequestPreview();
        updateVoucherInputState();
    });
    ['toName', 'phone', 'street'].forEach(id => $(id).addEventListener('input', () => {
        maybeRequestPreview();
        updateVoucherInputState();
    }));
    $('paymentMethods').addEventListener('change', maybeRequestPreview);
    $('placeOrderBtn').addEventListener('click', placeOrder);

    // Voucher event listeners
    $('applyVoucherBtn')?.addEventListener('click', applyVoucher);
    $('voucherInput')?.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();
            applyVoucher();
        }
    });
    $('removeVoucherBtn')?.addEventListener('click', () => removeVoucher(true));

    updateVoucherInputState();
    render();
    await Promise.all([loadCheckout(), loadProvinces(), loadPaymentMethods()]);
    updateVoucherInputState();
});