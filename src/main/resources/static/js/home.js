/* ==========================================================================
   TechNova Electronics — Home Page Script
   --------------------------------------------------------------------------
   Hiển thị DANH MỤC và SẢN PHẨM THẬT từ database (qua /api/categories và
   /api/products) thay cho dữ liệu mẫu trong data.js.
   Phụ thuộc: js/main.js (APP_URL, ROUTES, escapeHtml, renderStars, observeReveals)
   ========================================================================== */

'use strict';

const HOME_PLACEHOLDER = '/images/placeholder.webp';

function homeEsc(value) {
  return typeof escapeHtml === 'function'
    ? escapeHtml(value)
    : String(value == null ? '' : value).replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
}

function homeVnd(value) {
  return Number(value || 0).toLocaleString('vi-VN') + ' ₫';
}

function homeProductUrl(product) {
  return `${APP_URL}customer/products/${encodeURIComponent(product.slug)}`;
}

/** Gọi API và trả về phần "data"; lỗi thì trả về giá trị mặc định. */
async function homeFetch(url, fallback) {
  try {
    const res = await fetch(url, { headers: { 'Accept': 'application/json' } }).then(r => r.json());
    return (res && res.success && res.data != null) ? res.data : fallback;
  } catch (e) {
    console.warn('Không tải được dữ liệu từ', url, e);
    return fallback;
  }
}

function homeDiscountPercent(product) {
  if (product.minPromotionalPrice != null && product.minPrice > 0
    && product.minPromotionalPrice < product.minPrice) {
    return Math.round(((product.minPrice - product.minPromotionalPrice) * 100) / product.minPrice);
  }
  return 0;
}

/* ==========================================================================
   Card sản phẩm (cùng giao diện với trang /customer/products)
   ========================================================================== */
function homeProductCard(p, delay = 0) {
  const discount = homeDiscountPercent(p);
  const outOfStock = p.totalStock != null && p.totalStock === 0;
  const href = homeProductUrl(p);
  const price = p.effectivePrice != null ? p.effectivePrice : p.minPrice;
  const rating = Number(p.rating != null ? p.rating : 5);
  const img = p.thumbnailUrl || HOME_PLACEHOLDER;

  let stockNote = '';
  if (outOfStock) {
    stockNote = '<p class="stock-note text-danger">Tạm hết hàng</p>';
  } else if (p.totalStock != null && p.totalStock > 0 && p.totalStock <= 5) {
    stockNote = `<p class="stock-note text-warning">Chỉ còn ${p.totalStock} sản phẩm</p>`;
  }

  return `
  <div class="col-6 col-md-4 col-lg-3 product-col reveal" data-delay="${delay}">
    <article class="product-card ${outOfStock ? 'is-out' : ''}" data-id="${p.id}">
      <div class="card-media">
        ${discount > 0 ? `<span class="badge-discount">-${discount}%</span>` : ''}
        <a href="${href}" aria-label="Xem ${homeEsc(p.name)}">
          <img src="${homeEsc(img)}" alt="${homeEsc(p.name)}" loading="lazy" width="600" height="600"
               onerror="this.onerror=null;this.src='${HOME_PLACEHOLDER}';">
        </a>
        ${outOfStock ? '<div class="stock-flag"><span>Hết hàng</span></div>' : ''}
        <div class="card-actions">
          <button class="card-action-btn js-wishlist" data-id="${p.id}" type="button" aria-label="Yêu thích" title="Thêm vào yêu thích">
            <i class="far fa-heart"></i>
          </button>
          <a class="card-action-btn" href="${href}" aria-label="Xem chi tiết" title="Xem chi tiết">
            <i class="fas fa-eye"></i>
          </a>
        </div>
      </div>

      <div class="card-body">
        <span class="card-cat">${homeEsc(p.brandName || p.categoryName || 'TechNova')}</span>
        <h3 class="card-title"><a href="${href}">${homeEsc(p.name)}</a></h3>

        <div class="card-rating">
          <span class="stars">${typeof renderStars === 'function' ? renderStars(rating) : ''}</span>
          <span class="count">${rating.toFixed(1)}</span>
        </div>

        <div class="card-price">
          <span class="price">${homeVnd(price)}</span>
          ${discount > 0 ? `<span class="price-old">${homeVnd(p.minPrice)}</span>` : ''}
          ${discount > 0 ? `<span class="price-off">${discount}% giảm</span>` : ''}
        </div>

        ${stockNote}

        <button class="btn btn-brand btn-sm btn-cart js-add-cart" data-id="${p.id}" type="button" ${outOfStock ? 'disabled' : ''}>
          <i class="fas fa-cart-plus me-1"></i>${outOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
        </button>
      </div>
    </article>
  </div>`;
}

function homeRenderGrid(products, container, emptyTitle, emptyText) {
  if (!container) return;
  if (!products.length) {
    container.innerHTML = `
      <div class="col-12 empty-state text-center py-4">
        <h3>${emptyTitle}</h3>
        <p>${emptyText}</p>
      </div>`;
    return;
  }
  container.innerHTML = products.map((p, i) => homeProductCard(p, i % 4)).join('');
  if (typeof observeReveals === 'function') observeReveals();
}

async function homeLoadProducts(query) {
  const page = await homeFetch(`/api/products?${query}`, null);
  return page && Array.isArray(page.content) ? page.content : [];
}

/* ==========================================================================
   1. DANH MỤC
   ========================================================================== */
let homeCategories = [];

function renderCategoryTiles() {
  const grid = document.getElementById('categoryGrid');
  if (!grid) return;

  const roots = homeCategories.filter(c => !c.parentId);
  const list = (roots.length ? roots : homeCategories).slice(0, 6);

  if (!list.length) {
    grid.innerHTML = '<div class="col-12 text-center text-muted py-3">Chưa có danh mục nào.</div>';
    return;
  }

  grid.innerHTML = list.map((cat, i) => `
    <div class="col-6 col-md-4 col-lg reveal" data-delay="${i % 4}">
      <a class="category-tile" href="${ROUTES.products}?category=${encodeURIComponent(cat.slug)}">
        <img src="${homeEsc(cat.image || HOME_PLACEHOLDER)}" alt="${homeEsc(cat.name)}" loading="lazy" width="280" height="240"
             onerror="this.onerror=null;this.src='${HOME_PLACEHOLDER}';">
        <div class="tile-body">
          <h3><i class="fas fa-tag me-2"></i>${homeEsc(cat.name)}</h3>
        </div>
      </a>
    </div>`).join('');

  if (typeof observeReveals === 'function') observeReveals();
}

/* ==========================================================================
   2. SẢN PHẨM NỔI BẬT (tab theo danh mục)
   ========================================================================== */
function renderFeaturedTabs() {
  const tabs = document.getElementById('featuredTabs');
  if (!tabs) return;

  const roots = homeCategories.filter(c => !c.parentId);
  const list = (roots.length ? roots : homeCategories).slice(0, 6);

  tabs.insertAdjacentHTML('beforeend', list.map(cat =>
    `<button class="pill-tab" data-filter="${homeEsc(cat.slug)}" type="button">${homeEsc(cat.name)}</button>`
  ).join(''));

  tabs.addEventListener('click', e => {
    const btn = e.target.closest('.pill-tab');
    if (!btn) return;
    tabs.querySelectorAll('.pill-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    renderFeatured(btn.dataset.filter);
  });
}

async function renderFeatured(filter = 'all') {
  const grid = document.getElementById('featuredGrid');
  if (!grid) return;

  let query = 'size=8&sort=featured';
  if (filter !== 'all') query += `&category=${encodeURIComponent(filter)}`;

  const products = await homeLoadProducts(query);
  homeRenderGrid(products, grid, 'Chưa có sản phẩm', 'Danh mục này đang được cập nhật, vui lòng quay lại sau.');
}

/* ==========================================================================
   3. BÁN CHẠY
   ========================================================================== */
async function renderBestSellers() {
  const grid = document.getElementById('bestSellerGrid');
  if (!grid) return;

  const products = await homeLoadProducts('size=24&sort=popular');
  products.sort((a, b) => (b.totalSold || 0) - (a.totalSold || 0));
  homeRenderGrid(products.slice(0, 8), grid, 'Chưa có sản phẩm', 'Sản phẩm bán chạy sẽ sớm được cập nhật.');
}

/* ==========================================================================
   4. DEAL OF THE DAY — sản phẩm giảm giá sâu nhất
   ========================================================================== */
async function initDealOfTheDay() {
  const nameEl = document.getElementById('dealName');
  if (!nameEl) return;
  const section = nameEl.closest('section');

  const onSale = await homeLoadProducts('onSale=true&size=24');
  const deals = onSale.filter(p => homeDiscountPercent(p) > 0);

  if (!deals.length) {
    if (section) section.style.display = 'none';
    return;
  }

  const deal = deals.sort((a, b) => homeDiscountPercent(b) - homeDiscountPercent(a))[0];

  document.getElementById('dealImage').src = deal.thumbnailUrl || HOME_PLACEHOLDER;
  document.getElementById('dealImage').alt = deal.name;
  nameEl.textContent = deal.name;
  document.getElementById('dealTagline').textContent =
    [deal.brandName, deal.categoryName].filter(Boolean).join(' · ');
  document.getElementById('dealPrice').textContent = homeVnd(deal.effectivePrice != null ? deal.effectivePrice : deal.minPromotionalPrice);
  document.getElementById('dealOldPrice').textContent = homeVnd(deal.minPrice);
  document.getElementById('dealOff').textContent = `GIẢM ${homeDiscountPercent(deal)}%`;
  document.getElementById('dealLink').href = homeProductUrl(deal);

  startCountdown();
}

/** Đếm ngược đến 00:00 hôm nay. */
function startCountdown() {
  const boxes = {
    h: document.querySelector('[data-cd="h"]'),
    m: document.querySelector('[data-cd="m"]'),
    s: document.querySelector('[data-cd="s"]')
  };
  if (!boxes.h) return;

  const tick = () => {
    const now = new Date();
    const midnight = new Date(now);
    midnight.setHours(24, 0, 0, 0);

    const totalSeconds = Math.floor(Math.max(0, midnight - now) / 1000);
    const pad = n => String(n).padStart(2, '0');
    boxes.h.textContent = pad(Math.floor(totalSeconds / 3600));
    boxes.m.textContent = pad(Math.floor((totalSeconds % 3600) / 60));
    boxes.s.textContent = pad(totalSeconds % 60);
  };

  tick();
  setInterval(tick, 1000);
}

/* ==========================================================================
   5. GIỎ HÀNG / YÊU THÍCH — chưa nối với dữ liệu thật
   ========================================================================== */
function homeInterceptCartActions() {
  ['featuredGrid', 'bestSellerGrid'].forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    el.addEventListener('click', e => {
      const btn = e.target.closest('.js-add-cart, .js-wishlist');
      if (!btn) return;
      e.preventDefault();
      e.stopPropagation();
      const msg = btn.classList.contains('js-add-cart')
        ? 'Chức năng giỏ hàng đang được cập nhật!'
        : 'Chức năng danh sách yêu thích đang được cập nhật!';
      if (window.showToast) window.showToast('Thông báo', msg, 'info');
      else if (window.toast) window.toast(msg);
      else alert(msg);
    }, true); // capture: chạy trước listener của main.js
  });
}

/* ==========================================================================
   6. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', async () => {
  homeInterceptCartActions();

  homeCategories = await homeFetch('/api/categories', []);
  renderCategoryTiles();
  renderFeaturedTabs();

  renderFeatured('all');
  renderBestSellers();
  initDealOfTheDay();
});
