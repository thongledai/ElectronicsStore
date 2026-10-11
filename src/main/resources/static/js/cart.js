/* ==========================================================================
   TechNova Electronics — Shopping Cart Page
   --------------------------------------------------------------------------
   Renders the cart lines, handles quantity changes and removals, applies
   coupons and calculates the subtotal / GST / shipping / grand total.
   Cart data itself lives in the Cart manager in js/main.js (localStorage).
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. RENDER CART LINES
   ========================================================================== */
function renderCart() {
  const items = Cart.detailed();
  const layout = document.getElementById('cartLayout');
  const empty = document.getElementById('cartEmpty');

  // Toggle between the empty state and the full layout
  const isEmpty = items.length === 0;
  layout.hidden = isEmpty;
  empty.hidden = !isEmpty;

  if (isEmpty) {
    renderSuggestions();
    return;
  }

  const unitCount = Cart.count();
  document.getElementById('cartHeading').textContent =
    `Your items (${items.length} product${items.length === 1 ? '' : 's'}, ${unitCount} unit${unitCount === 1 ? '' : 's'})`;

  document.getElementById('cartItems').innerHTML = items.map(item => {
    const lineTotal = item.price * item.qty;
    const maxQty = Math.min(item.stock, CONFIG.maxQtyPerItem);
    const inWishlist = Wishlist.has(item.id);

    return `
      <article class="cart-item" data-id="${item.id}">

        <a class="ci-media" href="${ROUTES.productDetails}?id=${item.id}">
          <img src="${item.images[0]}" alt="${escapeHtml(item.name)}" loading="lazy">
        </a>

        <div>
          <p class="ci-cat mb-0">${escapeHtml(item.brand)} · ${getCategoryName(item.category)}</p>
          <h3 class="ci-name">
            <a href="${ROUTES.productDetails}?id=${item.id}">${escapeHtml(item.name)}</a>
          </h3>

          <div class="d-flex align-items-baseline gap-2">
            <span class="price">${formatPrice(item.price)}</span>
            ${item.oldPrice ? `<span class="price-old small">${formatPrice(item.oldPrice)}</span>` : ''}
          </div>

          ${item.stock <= 10 ? `<p class="stock-note mt-1 mb-0">Only ${item.stock} left</p>` : ''}

          <div class="ci-controls">
            <div class="qty-stepper" role="group" aria-label="Quantity for ${escapeHtml(item.name)}">
              <button type="button" class="js-qty-minus" data-id="${item.id}"
                      aria-label="Decrease quantity" ${item.qty <= 1 ? 'disabled' : ''}>
                <i class="fas fa-minus"></i>
              </button>
              <input type="number" class="js-qty-input" data-id="${item.id}"
                     value="${item.qty}" min="1" max="${maxQty}" aria-label="Quantity">
              <button type="button" class="js-qty-plus" data-id="${item.id}"
                      aria-label="Increase quantity" ${item.qty >= maxQty ? 'disabled' : ''}>
                <i class="fas fa-plus"></i>
              </button>
            </div>

            <button class="btn-link-danger js-remove" data-id="${item.id}" type="button">
              <i class="fas fa-trash-can"></i> Remove
            </button>

            <button class="btn-link-danger js-save-later" data-id="${item.id}" type="button">
              <i class="${inWishlist ? 'fas' : 'far'} fa-heart"></i>
              ${inWishlist ? 'In wishlist' : 'Save for later'}
            </button>
          </div>
        </div>

        <div class="ci-total">
          <span class="small text-muted d-block d-md-none">Line total</span>
          <span class="amount">${formatPrice(lineTotal)}</span>
          ${item.qty > 1 ? `<div class="small text-muted">${item.qty} × ${formatPrice(item.price)}</div>` : ''}
        </div>

      </article>`;
  }).join('');

  renderSummary();
  renderSuggestions();
}

/* ==========================================================================
   2. ORDER SUMMARY
   ========================================================================== */
function renderSummary() {
  const totals = Cart.totals();
  const coupon = getActiveCoupon();
  const unitCount = Cart.count();

  document.getElementById('subtotalLabel').textContent = `Subtotal (${unitCount} item${unitCount === 1 ? '' : 's'})`;
  document.getElementById('subtotal').textContent = formatPrice(totals.subtotal);
  document.getElementById('tax').textContent = formatPrice(totals.tax);
  document.getElementById('total').textContent = formatPrice(totals.total);

  // Discount row only appears when a coupon is active
  const discountRow = document.getElementById('discountRow');
  discountRow.hidden = totals.discount <= 0;
  document.getElementById('discountAmount').textContent = `−${formatPrice(totals.discount)}`;

  // Shipping shows "FREE" in green rather than ₹0
  const shippingRow = document.getElementById('shippingRow');
  const shippingCell = document.getElementById('shipping');
  if (totals.shipping === 0) {
    shippingCell.textContent = 'FREE';
    shippingRow.classList.add('free');
  } else {
    shippingCell.textContent = formatPrice(totals.shipping);
    shippingRow.classList.remove('free');
  }

  // Total savings note
  const savingsNote = document.getElementById('savingsNote');
  if (totals.savings > 0) {
    savingsNote.hidden = false;
    savingsNote.innerHTML = `<i class="fas fa-tags me-1"></i>You're saving <strong>${formatPrice(totals.savings)}</strong> on this order.`;
  } else {
    savingsNote.hidden = true;
  }

  renderShipMeter(totals.subtotal);
  renderCouponState(coupon);
}

/** Progress bar showing how far the user is from free shipping. */
function renderShipMeter(subtotal) {
  const fill = document.getElementById('shipFill');
  const text = document.getElementById('shipText');
  const threshold = CONFIG.freeShippingAbove;

  const pct = Math.min(100, (subtotal / threshold) * 100);
  fill.style.width = pct + '%';

  if (subtotal >= threshold) {
    text.innerHTML = '<i class="fas fa-circle-check text-success me-1"></i>You\'ve unlocked <strong>free delivery</strong>!';
  } else {
    const remaining = threshold - subtotal;
    text.innerHTML = `Add <strong>${formatPrice(remaining)}</strong> more to get free delivery.`;
  }
}

/** Swaps between the coupon input and the "applied" confirmation strip. */
function renderCouponState(coupon) {
  const area = document.getElementById('couponArea');

  if (coupon) {
    area.innerHTML = `
      <div class="coupon-applied mb-3">
        <span><i class="fas fa-ticket me-2"></i><strong>${escapeHtml(coupon.code)}</strong> — ${escapeHtml(coupon.label)}</span>
        <button class="btn-link-danger" id="removeCouponBtn" type="button" aria-label="Remove coupon">
          <i class="fas fa-xmark"></i>
        </button>
      </div>`;

    document.getElementById('removeCouponBtn').addEventListener('click', () => {
      storageSet(STORE.coupon, null);
      renderSummary();
      showToast(UiMessage.COUPON_REMOVED_TITLE, UiMessage.COUPON_REMOVED, 'info');
    });
  } else {
    area.innerHTML = `
      <div class="coupon-box">
        <input type="text" class="form-control" id="couponInput" placeholder="Coupon code" aria-label="Coupon code">
        <button class="btn btn-brand" id="applyCouponBtn" type="button">Apply</button>
      </div>
      <p class="small text-muted mb-3">
        Try <code>TECHNOVA10</code>, <code>STUDENT15</code>, <code>FLAT500</code> or <code>FREESHIP</code>
      </p>`;

    bindCouponInput();
  }
}

/** Validates and applies a coupon code entered by the user. */
function bindCouponInput() {
  const btn = document.getElementById('applyCouponBtn');
  const input = document.getElementById('couponInput');
  if (!btn || !input) return;

  const apply = () => {
    const code = input.value.trim().toUpperCase();

    if (!code) {
      showToast(UiMessage.COUPON_CODE_REQUIRED_TITLE, UiMessage.COUPON_CODE_REQUIRED, 'warning');
      input.focus();
      return;
    }

    const coupon = COUPONS[code];
    if (!coupon) {
      showToast(UiMessage.COUPON_INVALID_TITLE, UiMessage.COUPON_INVALID(code), 'danger');
      input.classList.add('is-invalid');
      setTimeout(() => input.classList.remove('is-invalid'), 1800);
      return;
    }

    // Some coupons require a minimum order value
    const subtotal = Cart.totals().subtotal;
    if (coupon.minOrder && subtotal < coupon.minOrder) {
      showToast(UiMessage.COUPON_MINIMUM_TITLE, UiMessage.COUPON_MINIMUM(code, formatPrice(coupon.minOrder)), 'warning', 4000);
      return;
    }

    storageSet(STORE.coupon, code);
    renderSummary();
    showToast(UiMessage.COUPON_APPLIED_TITLE, UiMessage.COUPON_APPLIED(code, coupon.label), 'success');
  };

  btn.addEventListener('click', apply);
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter') { e.preventDefault(); apply(); }
  });
}

/* ==========================================================================
   3. SUGGESTED PRODUCTS
   --------------------------------------------------------------------------
   Suggests best sellers the customer does not already have in their cart.
   ========================================================================== */
function renderSuggestions() {
  const grid = document.getElementById('suggestGrid');
  const section = document.getElementById('suggestSection');
  if (!grid) return;

  const inCart = Cart.all().map(i => i.id);
  const suggestions = PRODUCTS
    .filter(p => !inCart.includes(p.id) && p.stock > 0)
    .sort((a, b) => (b.bestSeller - a.bestSeller) || (b.rating - a.rating))
    .slice(0, 4);

  if (!suggestions.length) {
    section.hidden = true;
    return;
  }

  section.hidden = false;
  renderProductGrid(suggestions, grid);
}

/* ==========================================================================
   4. EVENT HANDLERS
   ========================================================================== */
function bindCartEvents() {
  const list = document.getElementById('cartItems');

  // Delegated handling for every control inside a cart line
  list.addEventListener('click', e => {

    // --- Quantity minus / plus ---
    const minus = e.target.closest('.js-qty-minus');
    const plus = e.target.closest('.js-qty-plus');
    if (minus || plus) {
      const btn = minus || plus;
      const id = btn.dataset.id;
      const current = Cart.qtyOf(id);
      Cart.updateQty(id, current + (plus ? 1 : -1));
      return;
    }

    // --- Remove line (with a short slide-out animation) ---
    const removeBtn = e.target.closest('.js-remove');
    if (removeBtn) {
      const id = removeBtn.dataset.id;
      const row = removeBtn.closest('.cart-item');
      row.classList.add('removing');
      setTimeout(() => Cart.remove(id), 280);
      return;
    }

    // --- Save for later: copy into the wishlist, then drop from the cart ---
    const saveBtn = e.target.closest('.js-save-later');
    if (saveBtn) {
      const id = saveBtn.dataset.id;
      if (!Wishlist.has(id)) Wishlist.add(id);
      const row = saveBtn.closest('.cart-item');
      row.classList.add('removing');
      setTimeout(() => Cart.remove(id, true), 280);
    }
  });

  // Typed quantity values
  list.addEventListener('change', e => {
    const input = e.target.closest('.js-qty-input');
    if (!input) return;
    Cart.updateQty(input.dataset.id, input.value);
  });

  // --- Clear the whole cart ---
  document.getElementById('clearCartBtn').addEventListener('click', () => {
    if (!Cart.count()) return;
    if (confirm('Remove all items from your cart?')) {
      Cart.clear();
      showToast(UiMessage.CART_CLEARED_TITLE, UiMessage.CART_CLEARED, 'info');
    }
  });

  // --- Demo checkout ---
  document.getElementById('checkoutBtn').addEventListener('click', () => {
    if (!Cart.count()) {
      showToast(UiMessage.CART_EMPTY_TITLE, UiMessage.CART_EMPTY, 'warning');
      return;
    }

    // Generate a plausible-looking demo order reference
    const ref = 'TN-' + String(Date.now()).slice(-6);
    document.getElementById('orderRef').textContent = ref;

    const modal = new bootstrap.Modal(document.getElementById('checkoutModal'));
    modal.show();

    // Empty the cart once the confirmation is dismissed
    document.getElementById('checkoutModal').addEventListener('hidden.bs.modal', () => {
      Cart.clear();
      showToast(UiMessage.ORDER_PLACED_TITLE, UiMessage.ORDER_PLACED(ref), 'success', 5000);
    }, { once: true });
  });
}

/* ==========================================================================
   5. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  renderCart();
  bindCartEvents();

  // Re-render whenever the cart changes from anywhere on the page
  document.addEventListener('cart:changed', renderCart);
  document.addEventListener('wishlist:changed', renderCart);
});
