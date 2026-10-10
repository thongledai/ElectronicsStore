/* ==========================================================================
   TechNova Electronics — Wishlist Page
   --------------------------------------------------------------------------
   Lists every saved product with "Move to Cart" and "Remove" actions, shows
   the total value of the list, and offers bulk actions.
   Wishlist data is managed by the Wishlist object in js/main.js.
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. RENDER
   ========================================================================== */
function renderWishlist() {
  const items = Wishlist.detailed();
  const layout = document.getElementById('wishlistLayout');
  const empty = document.getElementById('wishlistEmpty');

  const isEmpty = items.length === 0;
  layout.hidden = isEmpty;
  empty.hidden = !isEmpty;

  if (isEmpty) {
    renderRecommendations();
    return;
  }

  document.getElementById('wishlistCount').innerHTML =
    `<strong>${items.length}</strong> saved item${items.length === 1 ? '' : 's'}`;

  renderValueStrip(items);

  document.getElementById('wishlistGrid').innerHTML = items
    .map((item, i) => wishlistCard(item, i % 4))
    .join('');

  observeReveals();
  renderRecommendations();
}

/**
 * A wishlist-specific card. It reuses the product-card styling but swaps the
 * quick-action rail for a "remove" button and the footer button for
 * "Move to Cart", which is the primary action on this page.
 */
function wishlistCard(product, delay) {
  const discount = calcDiscount(product.price, product.oldPrice);
  const outOfStock = product.stock === 0;
  const inCart = Cart.has(product.id);
  const href = `${ROUTES.productDetails}?id=${product.id}`;

  return `
  <div class="col-6 col-md-4 col-lg-3 reveal" data-delay="${delay}">
    <article class="product-card ${outOfStock ? 'is-out' : ''}" data-id="${product.id}">

      <div class="card-media">
        ${discount > 0 ? `<span class="badge-discount">-${discount}%</span>` : ''}
        <a href="${href}" aria-label="View ${escapeHtml(product.name)}">
          <img src="${product.images[0]}" alt="${escapeHtml(product.name)}" loading="lazy" width="600" height="600">
        </a>
        ${outOfStock ? '<div class="stock-flag"><span>Out of Stock</span></div>' : ''}

        <div class="card-actions">
          <button class="card-action-btn js-remove-wish" data-id="${product.id}" type="button"
                  aria-label="Remove ${escapeHtml(product.name)} from wishlist" title="Remove from wishlist">
            <i class="fas fa-trash-can"></i>
          </button>
          <a class="card-action-btn" href="${href}" aria-label="View details" title="View details">
            <i class="fas fa-eye"></i>
          </a>
        </div>
      </div>

      <div class="card-body">
        <span class="card-cat">${escapeHtml(product.brand)}</span>
        <h3 class="card-title"><a href="${href}">${escapeHtml(product.name)}</a></h3>

        <div class="card-rating">
          <span class="stars">${renderStars(product.rating)}</span>
          <span class="count">${product.rating}</span>
        </div>

        <div class="card-price">
          <span class="price">${formatPrice(product.price)}</span>
          ${product.oldPrice ? `<span class="price-old">${formatPrice(product.oldPrice)}</span>` : ''}
        </div>

        ${inCart ? '<p class="stock-note in"><i class="fas fa-check me-1"></i>Already in your cart</p>' : ''}

        <button class="btn ${inCart ? 'btn-ghost' : 'btn-brand'} btn-sm btn-cart js-move-cart"
                data-id="${product.id}" type="button" ${outOfStock ? 'disabled' : ''}>
          <i class="fas fa-cart-plus me-1"></i>${outOfStock ? 'Unavailable' : 'Move to Cart'}
        </button>
      </div>

    </article>
  </div>`;
}

/** Totals strip above the grid. */
function renderValueStrip(items) {
  const total = items.reduce((sum, p) => sum + p.price, 0);
  const mrp = items.reduce((sum, p) => sum + (p.oldPrice || p.price), 0);
  const savings = mrp - total;

  document.getElementById('wishlistValue').textContent = formatPrice(total);

  const mrpEl = document.getElementById('wishlistMrp');
  const savingsEl = document.getElementById('wishlistSavings');

  if (savings > 0) {
    mrpEl.textContent = formatPrice(mrp);
    savingsEl.textContent = `Save ${formatPrice(savings)}`;
    savingsEl.hidden = false;
  } else {
    mrpEl.textContent = '';
    savingsEl.hidden = true;
  }
}

/* ==========================================================================
   2. RECOMMENDATIONS
   ========================================================================== */
function renderRecommendations() {
  const grid = document.getElementById('recommendGrid');
  const section = document.getElementById('recommendSection');
  if (!grid) return;

  const saved = Wishlist.all();

  // Prefer products from the categories the customer already likes
  const likedCategories = [...new Set(Wishlist.detailed().map(p => p.category))];

  let pool = PRODUCTS.filter(p => !saved.includes(p.id) && p.stock > 0);
  if (likedCategories.length) {
    const sameCategory = pool.filter(p => likedCategories.includes(p.category));
    if (sameCategory.length >= 4) pool = sameCategory;
  }

  const picks = pool
    .sort((a, b) => (b.bestSeller - a.bestSeller) || (b.rating - a.rating))
    .slice(0, 4);

  if (!picks.length) {
    section.hidden = true;
    return;
  }

  section.hidden = false;
  renderProductGrid(picks, grid);
}

/* ==========================================================================
   3. EVENTS
   ========================================================================== */
function bindWishlistEvents() {

  // Remove a single item (delegated so it works after re-renders)
  document.getElementById('wishlistGrid').addEventListener('click', e => {
    const btn = e.target.closest('.js-remove-wish');
    if (!btn) return;

    const card = btn.closest('.col-6, .col-md-4, .col-lg-3');
    if (card) {
      card.style.transition = 'opacity .25s ease, transform .25s ease';
      card.style.opacity = '0';
      card.style.transform = 'scale(.94)';
    }
    setTimeout(() => Wishlist.remove(btn.dataset.id), 240);
  });

  // --- Move everything to the cart -------------------------------------
  document.getElementById('moveAllBtn').addEventListener('click', () => {
    const items = Wishlist.detailed();
    if (!items.length) return;

    let moved = 0;
    let skipped = 0;

    items.forEach(item => {
      if (item.stock === 0) {
        skipped++;
        return;
      }
      // silent: true so we don't fire one toast per product
      if (Cart.add(item.id, 1, true)) {
        Wishlist.remove(item.id, true);
        moved++;
      } else {
        skipped++;
      }
    });

    if (moved) {
      const message = skipped
        ? `${moved} moved to cart · ${skipped} skipped (out of stock or at max quantity).`
        : `${moved} item${moved === 1 ? '' : 's'} moved to your cart.`;
      showToast(UiMessage.WISHLIST_MOVED_TITLE, message, 'success', 4000);
    } else {
      showToast(UiMessage.WISHLIST_UNAVAILABLE_TITLE, UiMessage.WISHLIST_UNAVAILABLE, 'warning');
    }
  });

  // --- Clear the wishlist ------------------------------------------------
  document.getElementById('clearWishlistBtn').addEventListener('click', () => {
    if (!Wishlist.count()) return;
    if (confirm('Remove all items from your wishlist?')) {
      Wishlist.save([]);
      showToast(UiMessage.WISHLIST_CLEARED_TITLE, UiMessage.WISHLIST_CLEARED, 'info');
    }
  });
}

/* ==========================================================================
   4. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  renderWishlist();
  bindWishlistEvents();

  // Keep the page in sync when items are added or removed elsewhere
  document.addEventListener('wishlist:changed', renderWishlist);
  document.addEventListener('cart:changed', renderWishlist);
});
