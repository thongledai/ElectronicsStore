/* ==========================================================================
   TechNova Electronics — Core Script (loaded on every page)
   --------------------------------------------------------------------------
   Responsibilities:
     • Storage helpers (localStorage read/write with safe fallbacks)
     • Cart & Wishlist state managers
     • Toast notification system
     • Dark / light theme toggle
     • Header behaviour: sticky shadow, live search suggestions, badge counts
     • Reusable UI builders: product card, star rating, price formatting
     • Scroll utilities: back-to-top, progress bar, reveal-on-scroll
   Every other page script depends on the helpers defined here, so main.js
   must be included BEFORE the page-specific script.
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. CONSTANTS & CONFIGURATION
   ========================================================================== */
const STORE = {
  cart: 'technova_cart',
  wishlist: 'technova_wishlist',
  theme: 'technova_theme',
  user: 'technova_user',
  recent: 'technova_recent',
  coupon: 'technova_coupon'
};

/* Application URLs. APP_URL (= ${URL}, context path + "/") is declared in fragments/header.html.
   Change a page address here and every script follows. */
const APP_URL = window.APP_URL || '/';
const ROUTES = {
  home: APP_URL + 'customer/index',
  products: APP_URL + 'customer/products',
  productDetails: APP_URL + 'customer/product-details',
  cart: APP_URL + 'customer/cart',
  wishlist: APP_URL + 'customer/wishlist',
  about: APP_URL + 'pages/about',
  contact: APP_URL + 'pages/contact',
  login: APP_URL + 'auth/login'
};

/* Business rules used by the cart calculations */
const CONFIG = {
  taxRate: 0.18,              // 18% GST
  shippingFee: 99,            // flat shipping charge
  freeShippingAbove: 5000,    // orders above this ship free
  currency: '₹',
  maxQtyPerItem: 10
};

/* Demo coupon codes — validated entirely on the frontend */
const COUPONS = {
  TECHNOVA10: { type: 'percent', value: 10, label: '10% off your order' },
  STUDENT15: { type: 'percent', value: 15, label: '15% student discount' },
  FLAT500: { type: 'flat', value: 500, label: '₹500 off', minOrder: 5000 },
  FREESHIP: { type: 'shipping', value: 0, label: 'Free shipping' }
};

/* ==========================================================================
   2. STORAGE HELPERS
   --------------------------------------------------------------------------
   Wrapped in try/catch because localStorage throws in private browsing modes
   on some browsers. Failing quietly keeps the demo usable everywhere.
   ========================================================================== */

/** Reads and parses a JSON value from localStorage. */
function storageGet(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw === null ? fallback : JSON.parse(raw);
  } catch (err) {
    console.warn('Storage read failed for', key, err);
    return fallback;
  }
}

/** Serialises and writes a value to localStorage. */
function storageSet(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch (err) {
    console.warn('Storage write failed for', key, err);
    return false;
  }
}

/* ==========================================================================
   3. FORMATTING HELPERS
   ========================================================================== */

/** Formats a number as Indian rupees, e.g. 184999 -> "₹1,84,999". */
function formatPrice(amount) {
  return CONFIG.currency + Math.round(amount).toLocaleString('en-IN');
}

/** Builds the Font Awesome star markup for a 0-5 rating. */
function renderStars(rating) {
  let html = '';
  for (let i = 1; i <= 5; i++) {
    if (rating >= i) html += '<i class="fas fa-star"></i>';
    else if (rating >= i - 0.5) html += '<i class="fas fa-star-half-stroke"></i>';
    else html += '<i class="far fa-star"></i>';
  }
  return html;
}

/** Percentage saved versus the struck-through MRP. */
function calcDiscount(price, oldPrice) {
  if (!oldPrice || oldPrice <= price) return 0;
  return Math.round(((oldPrice - price) / oldPrice) * 100);
}

/** Escapes user-supplied text before injecting it into HTML. */
function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

/* ==========================================================================
   4. TOAST NOTIFICATIONS
   --------------------------------------------------------------------------
   showToast('Added to cart', 'NovaBook Pro 16 is in your bag', 'success')
   ========================================================================== */
const TOAST_ICONS = {
  success: 'fa-check',
  danger: 'fa-xmark',
  warning: 'fa-triangle-exclamation',
  info: 'fa-circle-info'
};

/** Returns the toast container, creating it on first use. */
function getToastStack() {
  let stack = document.querySelector('.toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.className = 'toast-stack';
    stack.setAttribute('aria-live', 'polite');
    stack.setAttribute('aria-atomic', 'true');
    document.body.appendChild(stack);
  }
  return stack;
}

/**
 * Displays a floating notification in the top-right corner.
 * @param {string} title  bold headline
 * @param {string} message secondary line (optional)
 * @param {string} type   success | danger | warning | info
 * @param {number} timeout auto-dismiss delay in ms
 */
function showToast(title, message = '', type = 'success', timeout = 3200) {
  const stack = getToastStack();

  const toast = document.createElement('div');
  toast.className = `toast-note is-${type}`;
  toast.setAttribute('role', 'status');
  toast.innerHTML = `
    <div class="t-icon"><i class="fas ${TOAST_ICONS[type] || TOAST_ICONS.info}"></i></div>
    <div class="flex-grow-1">
      <div class="t-title">${escapeHtml(title)}</div>
      ${message ? `<div class="t-msg">${escapeHtml(message)}</div>` : ''}
    </div>
    <button class="t-close" type="button" aria-label="Dismiss notification">
      <i class="fas fa-xmark"></i>
    </button>`;

  // Manual dismiss
  toast.querySelector('.t-close').addEventListener('click', () => dismissToast(toast));
  stack.appendChild(toast);

  // Keep at most four toasts on screen at once
  while (stack.children.length > 4) dismissToast(stack.firstElementChild);

  setTimeout(() => dismissToast(toast), timeout);
}

/** Plays the exit animation then removes the toast node. */
function dismissToast(toast) {
  if (!toast || toast.classList.contains('leaving')) return;
  toast.classList.add('leaving');
  setTimeout(() => toast.remove(), 300);
}

/* ==========================================================================
   5. CART MANAGER
   --------------------------------------------------------------------------
   The cart is stored as an array of { id, qty } objects. Product details are
   always looked up fresh from PRODUCTS so prices never go stale in storage.
   ========================================================================== */
const Cart = {

  /** Raw cart array from localStorage. */
  all() {
    const items = storageGet(STORE.cart, []);
    return Array.isArray(items) ? items : [];
  },

  /** Persists the array and refreshes any UI that depends on it. */
  save(items) {
    storageSet(STORE.cart, items);
    updateBadges();
    document.dispatchEvent(new CustomEvent('cart:changed'));
  },

  /** Cart entries joined with their full product record. */
  detailed() {
    return this.all()
      .map(item => {
        const product = getProductById(item.id);
        return product ? { ...product, qty: item.qty } : null;
      })
      .filter(Boolean);   // drop entries whose product no longer exists
  },

  /** True if a product id is already in the cart. */
  has(id) {
    return this.all().some(i => i.id === Number(id));
  },

  /** Quantity currently in the cart for a product (0 if absent). */
  qtyOf(id) {
    const item = this.all().find(i => i.id === Number(id));
    return item ? item.qty : 0;
  },

  /**
   * Adds a product, or increases its quantity if already present.
   * Respects both the product stock level and CONFIG.maxQtyPerItem.
   */
  add(id, qty = 1, silent = false) {
    const product = getProductById(id);
    if (!product) return false;

    if (product.stock === 0) {
      showToast(UiMessage.PRODUCT_OUT_OF_STOCK_TITLE, UiMessage.PRODUCT_OUT_OF_STOCK(product.name), 'warning');
      return false;
    }

    const items = this.all();
    const existing = items.find(i => i.id === product.id);
    const cap = Math.min(product.stock, CONFIG.maxQtyPerItem);

    if (existing) {
      if (existing.qty >= cap) {
        showToast(UiMessage.MAXIMUM_REACHED_TITLE, UiMessage.MAXIMUM_REACHED(cap), 'warning');
        return false;
      }
      existing.qty = Math.min(existing.qty + qty, cap);
    } else {
      items.push({ id: product.id, qty: Math.min(qty, cap) });
    }

    this.save(items);
    if (!silent) {
      showToast(UiMessage.PRODUCT_ADDED_TITLE, UiMessage.PRODUCT_ADDED(product.name, formatPrice(product.price)), 'success');
    }
    return true;
  },

  /** Sets an exact quantity; removes the line when qty drops below 1. */
  updateQty(id, qty) {
    const product = getProductById(id);
    if (!product) return;

    const cap = Math.min(product.stock, CONFIG.maxQtyPerItem);
    qty = Math.max(0, Math.min(Number(qty) || 0, cap));

    if (qty === 0) return this.remove(id, true);

    const items = this.all();
    const existing = items.find(i => i.id === Number(id));
    if (existing) existing.qty = qty;
    this.save(items);
  },

  /** Removes a product line entirely. */
  remove(id, silent = false) {
    const product = getProductById(id);
    this.save(this.all().filter(i => i.id !== Number(id)));
    if (!silent && product) {
      showToast(UiMessage.PRODUCT_REMOVED_TITLE, UiMessage.PRODUCT_REMOVED(product.name), 'info');
    }
  },

  /** Empties the cart and clears any applied coupon. */
  clear() {
    this.save([]);
    storageSet(STORE.coupon, null);
  },

  /** Total number of units (not lines) in the cart. */
  count() {
    return this.all().reduce((sum, i) => sum + i.qty, 0);
  },

  /**
   * Calculates the full money breakdown, applying any stored coupon.
   * @returns {{subtotal, discount, taxable, tax, shipping, total, savings}}
   */
  totals() {
    const items = this.detailed();
    const subtotal = items.reduce((sum, p) => sum + p.price * p.qty, 0);
    const mrpTotal = items.reduce((sum, p) => sum + (p.oldPrice || p.price) * p.qty, 0);

    const coupon = getActiveCoupon();
    let discount = 0;
    let shipping = subtotal >= CONFIG.freeShippingAbove || subtotal === 0 ? 0 : CONFIG.shippingFee;

    if (coupon) {
      if (coupon.type === 'percent') discount = subtotal * (coupon.value / 100);
      else if (coupon.type === 'flat') discount = Math.min(coupon.value, subtotal);
      else if (coupon.type === 'shipping') shipping = 0;
    }

    const taxable = subtotal - discount;
    const tax = taxable * CONFIG.taxRate;
    const total = taxable + tax + shipping;

    return {
      subtotal,
      discount,
      taxable,
      tax,
      shipping,
      total,
      savings: mrpTotal - subtotal + discount
    };
  }
};

/** Returns the applied coupon object (with its code) or null. */
function getActiveCoupon() {
  const code = storageGet(STORE.coupon, null);
  if (!code || !COUPONS[code]) return null;
  return { code, ...COUPONS[code] };
}

/* ==========================================================================
   6. WISHLIST MANAGER
   --------------------------------------------------------------------------
   Stored as a plain array of product ids.
   ========================================================================== */
const Wishlist = {

  all() {
    const ids = storageGet(STORE.wishlist, []);
    return Array.isArray(ids) ? ids : [];
  },

  save(ids) {
    storageSet(STORE.wishlist, ids);
    updateBadges();
    document.dispatchEvent(new CustomEvent('wishlist:changed'));
  },

  detailed() {
    return this.all().map(id => getProductById(id)).filter(Boolean);
  },

  has(id) {
    return this.all().includes(Number(id));
  },

  add(id) {
    const product = getProductById(id);
    if (!product || this.has(id)) return false;
    this.save([...this.all(), product.id]);
    showToast(UiMessage.WISHLIST_SAVED_TITLE, UiMessage.WISHLIST_SAVED(product.name), 'success');
    return true;
  },

  remove(id, silent = false) {
    const product = getProductById(id);
    this.save(this.all().filter(i => i !== Number(id)));
    if (!silent && product) {
      showToast(UiMessage.WISHLIST_REMOVED_TITLE, UiMessage.WISHLIST_REMOVED(product.name), 'info');
    }
  },

  /** Adds if absent, removes if present. Returns the new state. */
  toggle(id) {
    if (this.has(id)) {
      this.remove(id);
      return false;
    }
    this.add(id);
    return true;
  },

  /** Moves one wishlist item into the cart. */
  moveToCart(id) {
    if (Cart.add(id)) {
      this.remove(id, true);
      return true;
    }
    return false;
  },

  count() {
    return this.all().length;
  }
};

/* ==========================================================================
   7. RECENTLY VIEWED
   ========================================================================== */
const Recent = {
  add(id) {
    let ids = storageGet(STORE.recent, []);
    if (!Array.isArray(ids)) ids = [];
    ids = [Number(id), ...ids.filter(i => i !== Number(id))].slice(0, 8);
    storageSet(STORE.recent, ids);
  },
  detailed(excludeId) {
    return storageGet(STORE.recent, [])
      .filter(id => id !== Number(excludeId))
      .map(id => getProductById(id))
      .filter(Boolean);
  }
};

/* ==========================================================================
   8. REUSABLE UI COMPONENTS
   ========================================================================== */

/**
 * Builds the markup for a single product card.
 * Used by the home page, products listing and "related products" sections.
 * @param {object} product a record from PRODUCTS
 * @param {number} delay   0-4, staggers the reveal animation
 */
function renderProductCard(product, delay = 0) {
  const discount = calcDiscount(product.price, product.oldPrice);
  const outOfStock = product.stock === 0;
  const inWishlist = Wishlist.has(product.id);
  const href = `${ROUTES.productDetails}?id=${product.id}`;

  // Low stock nudge — a light urgency cue like real stores use
  let stockNote = '';
  if (outOfStock) {
    stockNote = '<p class="stock-note">Currently unavailable</p>';
  } else if (product.stock <= 10) {
    stockNote = `<p class="stock-note">Only ${product.stock} left in stock</p>`;
  }

  return `
  <div class="col-6 col-md-4 col-lg-3 product-col reveal" data-delay="${delay}">
    <article class="product-card ${outOfStock ? 'is-out' : ''}" data-id="${product.id}">
      <div class="card-media">
        ${product.badge ? `<span class="badge-ribbon">${escapeHtml(product.badge)}</span>` : ''}
        ${discount > 0 ? `<span class="badge-discount">-${discount}%</span>` : ''}
        <a href="${href}" aria-label="View ${escapeHtml(product.name)}">
          <img src="${product.images[0]}" alt="${escapeHtml(product.name)}" loading="lazy" width="600" height="600">
        </a>
        ${outOfStock ? '<div class="stock-flag"><span>Out of Stock</span></div>' : ''}
        <div class="card-actions">
          <button class="card-action-btn js-wishlist ${inWishlist ? 'is-active' : ''}"
                  data-id="${product.id}" type="button"
                  aria-label="${inWishlist ? 'Remove from' : 'Add to'} wishlist"
                  title="Add to wishlist">
            <i class="${inWishlist ? 'fas' : 'far'} fa-heart"></i>
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
          <span class="count">${product.rating} (${product.reviewCount.toLocaleString('en-IN')})</span>
        </div>

        <div class="card-price">
          <span class="price">${formatPrice(product.price)}</span>
          ${product.oldPrice ? `<span class="price-old">${formatPrice(product.oldPrice)}</span>` : ''}
          ${discount > 0 ? `<span class="price-off">${discount}% off</span>` : ''}
        </div>

        ${stockNote}

        <button class="btn btn-brand btn-sm btn-cart js-add-cart" data-id="${product.id}"
                type="button" ${outOfStock ? 'disabled' : ''}>
          <i class="fas fa-cart-plus me-1"></i>${outOfStock ? 'Notify Me' : 'Add to Cart'}
        </button>
      </div>
    </article>
  </div>`;
}

/** Renders an array of products into a container element. */
function renderProductGrid(products, container) {
  if (!container) return;
  container.innerHTML = products
    .map((p, i) => renderProductCard(p, i % 4))
    .join('');
  observeReveals();
}

/* ==========================================================================
   9. GLOBAL EVENT DELEGATION
   --------------------------------------------------------------------------
   A single listener on <body> handles every "Add to Cart" and wishlist heart
   on the page, including cards injected later by JavaScript.
   ========================================================================== */
function initGlobalActions() {
  document.body.addEventListener('click', e => {

    // --- Add to cart ---------------------------------------------------
    const cartBtn = e.target.closest('.js-add-cart');
    if (cartBtn && !cartBtn.disabled) {
      e.preventDefault();
      const id = cartBtn.dataset.id;
      const product = getProductById(id);

      if (product && product.stock === 0) {
        showToast(UiMessage.BACK_IN_STOCK_TITLE, UiMessage.BACK_IN_STOCK(product.name), 'info');
        return;
      }
      // Optional quantity comes from a stepper on the details page
      const qtyInput = document.querySelector('#qtyInput');
      const qty = cartBtn.dataset.useQty === 'true' && qtyInput ? Number(qtyInput.value) : 1;
      Cart.add(id, qty);
      return;
    }

    // --- Toggle wishlist -----------------------------------------------
    const wishBtn = e.target.closest('.js-wishlist');
    if (wishBtn) {
      e.preventDefault();
      const id = wishBtn.dataset.id;
      const nowSaved = Wishlist.toggle(id);

      // Sync every heart button for this product across the page
      document.querySelectorAll(`.js-wishlist[data-id="${id}"]`).forEach(btn => {
        btn.classList.toggle('is-active', nowSaved);
        const icon = btn.querySelector('i');
        if (icon) {
          icon.className = `${nowSaved ? 'fas' : 'far'} fa-heart`;
          icon.classList.add('heart-pop');
          setTimeout(() => icon.classList.remove('heart-pop'), 420);
        }
        btn.setAttribute('aria-label', `${nowSaved ? 'Remove from' : 'Add to'} wishlist`);
      });
      return;
    }

    // --- Move a wishlist item into the cart -----------------------------
    const moveBtn = e.target.closest('.js-move-cart');
    if (moveBtn) {
      e.preventDefault();
      Wishlist.moveToCart(moveBtn.dataset.id);
    }
  });
}

/* ==========================================================================
   10. HEADER
   ========================================================================== */

/** Refreshes the cart / wishlist count bubbles in the header. */
function updateBadges() {
  const map = [
    { selector: '.js-cart-count', value: Cart.count() },
    { selector: '.js-wishlist-count', value: Wishlist.count() }
  ];

  map.forEach(({ selector, value }) => {
    document.querySelectorAll(selector).forEach(el => {
      const previous = el.textContent.trim();
      el.textContent = value;
      el.dataset.count = value;
      // Bump animation only when the number actually changed
      if (previous !== String(value) && value > 0) {
        el.classList.remove('bump');
        void el.offsetWidth;        // force reflow so the animation replays
        el.classList.add('bump');
      }
    });
  });
}

/** Marks the current page's nav link as active. */
function markActiveNav() {
  const current = location.pathname.replace(/\/+$/, '');
  document.querySelectorAll('.navbar-nav .nav-link').forEach(link => {
    const target = link.getAttribute('href');
    if (!target || target.startsWith('#')) return;
    if (new URL(link.href, location.origin).pathname.replace(/\/+$/, '') === current) {
      link.classList.add('active');
      link.setAttribute('aria-current', 'page');
    }
  });
}

/** Adds a shadow to the header once the page is scrolled. */
function initStickyHeader() {
  const header = document.querySelector('.site-header');
  if (!header) return;
  const onScroll = () => header.classList.toggle('is-stuck', window.scrollY > 20);
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
}

/**
 * Live search in the header: shows up to six matching products in a dropdown
 * and forwards the full query to products.html on submit.
 */
function initHeaderSearch() {
  document.querySelectorAll('.header-search').forEach(wrap => {
    const input = wrap.querySelector('input');
    const panel = wrap.querySelector('.search-suggestions');
    const form = wrap.closest('form') || wrap.querySelector('form');
    if (!input || !panel) return;

    const closePanel = () => panel.classList.remove('show');
    const vnd = v => Number(v || 0).toLocaleString('vi-VN') + ' ₫';
    let timer = null;
    let seq = 0;

    // Gợi ý sản phẩm thật từ database (tối đa 6 kết quả)
    input.addEventListener('input', () => {
      const query = input.value.trim();
      clearTimeout(timer);
      if (query.length < 2) return closePanel();

      timer = setTimeout(async () => {
        const mySeq = ++seq;
        try {
          const res = await fetch(`/api/products?q=${encodeURIComponent(query)}&size=6`, {
            headers: { 'Accept': 'application/json' }
          }).then(r => r.json());
          if (mySeq !== seq) return; // đã có truy vấn mới hơn

          const items = (res && res.success && res.data && res.data.content) ? res.data.content : [];
          if (!items.length) {
            panel.innerHTML = `
              <div class="suggestion-item text-muted">
                <i class="fas fa-magnifying-glass"></i>
                <span class="s-name">Không có sản phẩm phù hợp "${escapeHtml(query)}"</span>
              </div>`;
          } else {
            panel.innerHTML = items.map(p => `
              <a class="suggestion-item" href="${APP_URL}customer/products/${encodeURIComponent(p.slug)}">
                <img src="${escapeHtml(p.thumbnailUrl || '/images/placeholder.webp')}" alt="${escapeHtml(p.name)}" loading="lazy"
                     onerror="this.onerror=null;this.src='/images/placeholder.webp';">
                <span>
                  <span class="s-name d-block">${escapeHtml(p.name)}</span>
                  <span class="s-cat">${escapeHtml(p.categoryName || '')} · ${vnd(p.effectivePrice != null ? p.effectivePrice : p.minPrice)}</span>
                </span>
              </a>`).join('');
          }
          panel.classList.add('show');
        } catch (e) {
          closePanel();
        }
      }, 300);
    });

    // Nhấn Enter / Tìm: chuyển tới trang sản phẩm với từ khóa
    if (form) {
      form.addEventListener('submit', e => {
        e.preventDefault();
        const query = input.value.trim();
        location.href = query
          ? `${ROUTES.products}?search=${encodeURIComponent(query)}`
          : ROUTES.products;
      });
    }

    input.addEventListener('keydown', e => { if (e.key === 'Escape') closePanel(); });
    document.addEventListener('click', e => { if (!wrap.contains(e.target)) closePanel(); });
  });
}

/** Điền menu danh mục trên navbar từ database (/api/categories). */
function initCategoryMenu() {
  const menus = document.querySelectorAll('.js-category-menu');
  if (!menus.length) return;

  const viewAll = `
      <li><hr class="dropdown-divider"></li>
      <li><a class="dropdown-item" href="${ROUTES.products}"><i class="fas fa-grip"></i>Xem tất cả sản phẩm</a></li>`;
  menus.forEach(menu => { menu.innerHTML = viewAll; });

  fetch('/api/categories', { headers: { 'Accept': 'application/json' } })
    .then(r => r.json())
    .then(res => {
      const list = (res && res.success && Array.isArray(res.data)) ? res.data : [];
      const html = list.map(c => `
      <li>
        <a class="dropdown-item" href="${ROUTES.products}?category=${encodeURIComponent(c.slug)}">
          <i class="fas fa-tag"></i>${escapeHtml(c.name)}
        </a>
      </li>`).join('') + viewAll;
      menus.forEach(menu => { menu.innerHTML = html; });
    })
    .catch(() => { /* giữ lại mục "Xem tất cả sản phẩm" */ });
}

/* ==========================================================================
   11. THEME (DARK / LIGHT MODE)
   ========================================================================== */

/** Applies a theme and updates every toggle button's icon. */
function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  storageSet(STORE.theme, theme);

  document.querySelectorAll('.js-theme-toggle').forEach(btn => {
    const icon = btn.querySelector('i');
    if (icon) icon.className = theme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    btn.setAttribute('aria-label', theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
    btn.setAttribute('title', theme === 'dark' ? 'Light mode' : 'Dark mode');
  });

  // Keep the browser UI colour in sync on mobile
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute('content', theme === 'dark' ? '#0a1220' : '#0b1b34');
}

function initTheme() {
  // Saved preference wins; otherwise follow the operating system setting
  const saved = storageGet(STORE.theme, null);
  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  applyTheme(saved || (prefersDark ? 'dark' : 'light'));

  document.querySelectorAll('.js-theme-toggle').forEach(btn => {
    btn.addEventListener('click', () => {
      const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      applyTheme(next);
      showToast(next === 'dark' ? 'Dark mode on' : 'Light mode on', 'Your preference has been saved.', 'info', 2000);
    });
  });
}

/* Apply the stored theme immediately (before DOMContentLoaded) so the page
   never flashes white for dark-mode users. */
(function preloadTheme() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORE.theme));
    if (saved) document.documentElement.setAttribute('data-theme', saved);
    else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      document.documentElement.setAttribute('data-theme', 'dark');
    }
  } catch (err) { /* storage unavailable — fall back to the light theme */ }
})();

/* ==========================================================================
   12. SCROLL UTILITIES
   ========================================================================== */

/** Back-to-top button: appears after 400px, smooth-scrolls on click. */
function initBackToTop() {
  const btn = document.querySelector('.back-to-top');
  if (!btn) return;

  window.addEventListener('scroll', () => {
    btn.classList.toggle('show', window.scrollY > 400);
  }, { passive: true });

  btn.addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });
}

/** Thin gradient bar showing how far down the page the user has scrolled. */
function initScrollProgress() {
  const bar = document.querySelector('.scroll-progress');
  if (!bar) return;

  window.addEventListener('scroll', () => {
    const max = document.documentElement.scrollHeight - window.innerHeight;
    const pct = max > 0 ? (window.scrollY / max) * 100 : 0;
    bar.style.width = pct + '%';
  }, { passive: true });
}

/**
 * Reveal-on-scroll using IntersectionObserver.
 * Safe to call repeatedly — already-revealed elements are skipped.
 */
let revealObserver = null;
function observeReveals() {
  const targets = document.querySelectorAll('.reveal:not(.revealed)');
  if (!targets.length) return;

  // Browsers without IntersectionObserver simply show everything
  if (!('IntersectionObserver' in window)) {
    targets.forEach(el => el.classList.add('revealed'));
    return;
  }

  if (!revealObserver) {
    revealObserver = new IntersectionObserver((entries, observer) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });
  }

  targets.forEach(el => revealObserver.observe(el));
}

/* ==========================================================================
   13. NEWSLETTER (footer + home section)
   ========================================================================== */
function initNewsletter() {
  document.querySelectorAll('.js-newsletter').forEach(form => {
    form.addEventListener('submit', e => {
      e.preventDefault();
      const input = form.querySelector('input[type="email"]');
      const email = input.value.trim();

      if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) {
        showToast(UiMessage.EMAIL_INVALID_TITLE, UiMessage.EMAIL_INVALID, 'danger');
        input.focus();
        return;
      }

      showToast(UiMessage.NEWSLETTER_SUCCESS_TITLE, UiMessage.NEWSLETTER_SUCCESS, 'success');
      form.reset();
    });
  });
}

/* ==========================================================================
   14. MISC
   ========================================================================== */

/** Stamps the current year into any [data-year] element (footer copyright). */
function initYear() {
  document.querySelectorAll('[data-year]').forEach(el => {
    el.textContent = new Date().getFullYear();
  });
}

/** Reflects the demo login state in the header account label. */
function initAccountLabel() {
  const user = storageGet(STORE.user, null);
  const token = localStorage.getItem('technova_jwt');
  const isLoggedIn = !!(token || (user && user.email));

  document.querySelectorAll('.js-account-label').forEach(el => {
    el.textContent = isLoggedIn && user && user.name ? user.name.split(' ')[0] : 'Sign In';
  });

  document.querySelectorAll('.js-account-greeting').forEach(el => {
    if (isLoggedIn && user && user.name) {
      el.textContent = 'Hello, ' + user.name.split(' ')[0];
    } else {
      el.textContent = 'My Account';
    }
  });

  document.querySelectorAll('.js-auth-guest').forEach(el => {
    el.style.display = isLoggedIn ? 'none' : 'block';
  });

  document.querySelectorAll('.js-auth-user').forEach(el => {
    el.style.display = isLoggedIn ? 'block' : 'none';
  });

  if (new URLSearchParams(location.search).get('logout') === 'true') {
    localStorage.removeItem('technova_jwt');
    localStorage.removeItem('technova_role');
    storageSet(STORE.user, null);
    document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
  }

  document.querySelectorAll('form[action*="/logout"], .js-logout-form').forEach(form => {
    form.addEventListener('submit', () => {
      localStorage.removeItem('technova_jwt');
      localStorage.removeItem('technova_role');
      storageSet(STORE.user, null);
      document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    });
  });
}

/** Reads a query-string parameter from the current URL. */
function getParam(name) {
  return new URLSearchParams(location.search).get(name);
}

/** Enables Bootstrap tooltips wherever they are used. */
function initTooltips() {
  if (typeof bootstrap === 'undefined') return;
  document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));
}

/* ==========================================================================
   15. BOOTSTRAP THE PAGE
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initCategoryMenu();
  initHeaderSearch();
  initStickyHeader();
  markActiveNav();
  initGlobalActions();
  initBackToTop();
  initScrollProgress();
  initNewsletter();
  initYear();
  initAccountLabel();
  initTooltips();
  updateBadges();
  observeReveals();
});

/* Keep multiple open tabs in sync — storage events fire in the other tabs. */
window.addEventListener('storage', e => {
  if (e.key === STORE.cart || e.key === STORE.wishlist) updateBadges();
});
