/* ==========================================================================
   TechNova Electronics — Shopping Cart Page (nối backend /cart)
   ========================================================================== */

'use strict';

const CART_API = '/cart';
const CHECKOUT_URL = '/customer/checkout';   // đổi nếu trang thanh toán có URL khác
const MAX_QTY = 99;

let cartItems = [];
let selectedIds = new Set();
let firstLoad = true;
let busy = false;

/* ---------- Tiện ích ---------- */
function fmtMoney(n) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
}

function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}

function notify(title, message, type) {
    if (typeof showToast === 'function') showToast(title, message, type || 'info');
    else alert(message);
}

function productLink(item) {
    return item.slug ? `/customer/products/${encodeURIComponent(item.slug)}` : '/customer/products';
}

async function api(method, url, body) {
    const res = await fetch(url, {
        method,
        credentials: 'same-origin',
        headers: {
            'Accept': 'application/json',
            ...(body ? { 'Content-Type': 'application/json' } : {})
        },
        body: body ? JSON.stringify(body) : undefined
    });

    if (res.status === 401) {
        window.location.href = '/login?error=unauthorized';
        throw new Error('UNAUTHENTICATED');
    }

    let json = null;
    try { json = await res.json(); } catch (e) { /* không có body */ }

    if (!res.ok || (json && json.success === false)) {
        throw new Error((json && json.message) || 'Có lỗi xảy ra, vui lòng thử lại.');
    }
    return json;
}

/* ---------- Chuẩn hóa dữ liệu từ CartItemDTO ---------- */
function normalizeItem(raw) {
    const stock = raw.availableStock ?? MAX_QTY;
    const qty = Number(raw.quantity ?? raw.count ?? 1);
    return {
        id: raw.id,
        slug: raw.productSlug || '',
        name: raw.productName || '',
        variant: raw.styleValues || '',
        image: raw.productImageUrl || '/images/placeholder.webp',
        price: Number(raw.unitPrice || 0),
        qty: qty,
        stock: Number(stock),
        // còn bán (nếu backend có trả cờ available) và đủ hàng
        canBuy: (raw.isAvailable ?? raw.available) !== false && Number(stock) >= qty && Number(stock) > 0
    };
}

/* ---------- Tải giỏ hàng ---------- */
async function loadCart() {
    try {
        const json = await api('GET', CART_API);
        cartItems = (Array.isArray(json && json.data) ? json.data : []).map(normalizeItem);
    } catch (e) {
        if (e.message !== 'UNAUTHENTICATED') notify('Lỗi', e.message, 'danger');
        cartItems = [];
    }
    syncSelection();
    renderCart();
    if (typeof updateBadges === 'function') updateBadges();   // cập nhật số trên header
}

/** Lần đầu: chọn sẵn mọi món mua được. Các lần sau: giữ lựa chọn, bỏ món không còn. */
function syncSelection() {
    const buyable = cartItems.filter(i => i.canBuy).map(i => i.id);
    if (firstLoad) {
        selectedIds = new Set(buyable);
        firstLoad = false;
    } else {
        selectedIds = new Set([...selectedIds].filter(id => buyable.includes(id)));
    }
}

/* ---------- Hiển thị ---------- */
function renderCart() {
    const layout = document.getElementById('cartLayout');
    const empty = document.getElementById('cartEmpty');
    const isEmpty = cartItems.length === 0;

    layout.hidden = isEmpty;
    empty.hidden = !isEmpty;
    hideUnsupportedBlocks();
    if (isEmpty) return;

    const unitCount = cartItems.reduce((s, i) => s + i.qty, 0);
    document.getElementById('cartHeading').textContent =
        `Your items (${cartItems.length} product${cartItems.length === 1 ? '' : 's'}, ${unitCount} unit${unitCount === 1 ? '' : 's'})`;

    document.getElementById('cartItems').innerHTML = cartItems.map(item => {
        const maxQty = Math.max(1, Math.min(item.stock, MAX_QTY));
        const lineTotal = item.price * item.qty;
        const checked = selectedIds.has(item.id) ? 'checked' : '';
        const disabledSelect = item.canBuy ? '' : 'disabled';

        let note = '';
        if (item.stock <= 0) note = '<p class="stock-note mt-1 mb-0 text-danger">Out of stock</p>';
        else if (item.qty > item.stock) note = `<p class="stock-note mt-1 mb-0 text-danger">Only ${item.stock} left, please reduce quantity</p>`;
        else if (!item.canBuy) note = '<p class="stock-note mt-1 mb-0 text-danger">No longer available</p>';
        else if (item.stock <= 10) note = `<p class="stock-note mt-1 mb-0">Only ${item.stock} left</p>`;

        return `
      <article class="cart-item ${item.canBuy ? '' : 'opacity-75'}" data-id="${esc(item.id)}">

        <a class="ci-media" href="${productLink(item)}">
          <img src="${esc(item.image)}" alt="${esc(item.name)}" loading="lazy"
               onerror="this.onerror=null;this.src='/images/placeholder.webp';">
        </a>

        <div>
          <label class="small d-flex align-items-center gap-2 mb-1">
            <input type="checkbox" class="form-check-input mt-0 js-select" data-id="${esc(item.id)}" ${checked} ${disabledSelect}>
            Select
          </label>

          <h3 class="ci-name">
            <a href="${productLink(item)}">${esc(item.name)}</a>
          </h3>
          ${item.variant ? `<p class="small text-muted mb-1">${esc(item.variant)}</p>` : ''}

          <div class="d-flex align-items-baseline gap-2">
            <span class="price">${fmtMoney(item.price)}</span>
          </div>

          ${note}

          <div class="ci-controls">
            <div class="qty-stepper" role="group" aria-label="Quantity for ${esc(item.name)}">
              <button type="button" class="js-qty-minus" data-id="${esc(item.id)}"
                      aria-label="Decrease quantity" ${item.qty <= 1 ? 'disabled' : ''}>
                <i class="fas fa-minus"></i>
              </button>
              <input type="number" class="js-qty-input" data-id="${esc(item.id)}"
                     value="${item.qty}" min="1" max="${maxQty}" aria-label="Quantity">
              <button type="button" class="js-qty-plus" data-id="${esc(item.id)}"
                      aria-label="Increase quantity" ${item.qty >= maxQty ? 'disabled' : ''}>
                <i class="fas fa-plus"></i>
              </button>
            </div>

            <button class="btn-link-danger js-remove" data-id="${esc(item.id)}" type="button">
              <i class="fas fa-trash-can"></i> Remove
            </button>
          </div>
        </div>

        <div class="ci-total">
          <span class="small text-muted d-block d-md-none">Line total</span>
          <span class="amount">${fmtMoney(lineTotal)}</span>
          ${item.qty > 1 ? `<div class="small text-muted">${item.qty} × ${fmtMoney(item.price)}</div>` : ''}
        </div>

      </article>`;
    }).join('');

    renderSummary();
}

/** Tạm tính chỉ để hiển thị. Số tiền thật do server tính ở bước checkout. */
function renderSummary() {
    const chosen = cartItems.filter(i => selectedIds.has(i.id));
    const qty = chosen.reduce((s, i) => s + i.qty, 0);
    const subtotal = chosen.reduce((s, i) => s + i.price * i.qty, 0);

    const setText = (id, text) => {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    };
    setText('subtotalLabel', `Subtotal (${qty} item${qty === 1 ? '' : 's'})`);
    setText('subtotal', fmtMoney(subtotal));
    setText('shipping', 'Tính ở bước thanh toán');
    setText('total', fmtMoney(subtotal));

    const buyable = cartItems.filter(i => i.canBuy);
    const selectAll = document.getElementById('selectAll');
    if (selectAll) {
        selectAll.checked = buyable.length > 0 && buyable.every(i => selectedIds.has(i.id));
    }

    const checkoutBtn = document.getElementById('checkoutBtn');
    if (checkoutBtn) checkoutBtn.disabled = chosen.length === 0;
}

/** Ẩn các khối của bản demo mà backend chưa có. */
function hideUnsupportedBlocks() {
    ['couponArea', 'discountRow', 'shipMeter', 'savingsNote', 'suggestSection'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.hidden = true;
    });
    const tax = document.getElementById('tax');
    if (tax && tax.parentElement) tax.parentElement.hidden = true;
}

/* ---------- Thao tác ---------- */
async function runAction(action, okTitle, okMessage) {
    if (busy) return;
    busy = true;
    try {
        await action();
        if (okMessage) notify(okTitle, okMessage, 'info');
    } catch (e) {
        if (e.message !== 'UNAUTHENTICATED') notify('Lỗi', e.message, 'danger');
    } finally {
        busy = false;
        await loadCart();    // luôn tải lại để hiển thị đúng dữ liệu server
    }
}

function updateQty(itemId, value) {
    const item = cartItems.find(i => String(i.id) === String(itemId));
    if (!item) return;

    let qty = Math.floor(Number(value));
    if (!Number.isFinite(qty) || qty < 1) qty = 1;
    if (qty > MAX_QTY) qty = MAX_QTY;
    if (qty === item.qty) { renderCart(); return; }

    runAction(() => api('PUT', `${CART_API}/items/${encodeURIComponent(itemId)}`, { count: qty }));
}

function removeItem(itemId) {
    runAction(() => api('DELETE', `${CART_API}/items/${encodeURIComponent(itemId)}`),
        'Đã xóa', 'Đã xóa sản phẩm khỏi giỏ hàng');
}

function clearCart() {
    runAction(() => api('DELETE', CART_API), 'Đã xóa', 'Đã xóa toàn bộ giỏ hàng');
}

/* ---------- Sự kiện ---------- */
function bindCartEvents() {
    const list = document.getElementById('cartItems');

    list.addEventListener('click', e => {
        const minus = e.target.closest('.js-qty-minus');
        const plus = e.target.closest('.js-qty-plus');
        if (minus || plus) {
            const btn = minus || plus;
            const item = cartItems.find(i => String(i.id) === String(btn.dataset.id));
            if (item) updateQty(item.id, item.qty + (plus ? 1 : -1));
            return;
        }

        const removeBtn = e.target.closest('.js-remove');
        if (removeBtn) {
            const row = removeBtn.closest('.cart-item');
            if (row) row.classList.add('removing');
            const id = removeBtn.dataset.id;
            setTimeout(() => removeItem(id), 280);
        }
    });

    list.addEventListener('change', e => {
        const sel = e.target.closest('.js-select');
        if (sel) {
            if (sel.checked) selectedIds.add(sel.dataset.id);
            else selectedIds.delete(sel.dataset.id);
            renderSummary();
            return;
        }
        const input = e.target.closest('.js-qty-input');
        if (input) updateQty(input.dataset.id, input.value);
    });

    const selectAll = document.getElementById('selectAll');
    if (selectAll) {
        selectAll.addEventListener('change', e => {
            selectedIds = e.target.checked
                ? new Set(cartItems.filter(i => i.canBuy).map(i => i.id))
                : new Set();
            renderCart();
        });
    }

    const clearBtn = document.getElementById('clearCartBtn');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (cartItems.length && confirm('Remove all items from your cart?')) clearCart();
        });
    }

    const checkoutBtn = document.getElementById('checkoutBtn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', () => {
            if (selectedIds.size === 0) {
                notify('Chưa chọn sản phẩm', 'Vui lòng chọn ít nhất một sản phẩm', 'warning');
                return;
            }
            sessionStorage.setItem('checkoutItemIds', JSON.stringify([...selectedIds]));
            window.location.href = CHECKOUT_URL;
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    bindCartEvents();
    loadCart();
});