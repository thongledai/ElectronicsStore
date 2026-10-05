/* ==========================================================================
   TechNova Electronics — Home Page Script
   --------------------------------------------------------------------------
   Renders the category tiles, the filterable "Featured Products" grid, the
   best-seller row and the deal-of-the-day countdown.
   Depends on: js/data.js and js/main.js
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. CATEGORY TILES
   ========================================================================== */
function renderCategoryTiles() {
  const grid = document.getElementById('categoryGrid');
  if (!grid) return;

  grid.innerHTML = CATEGORIES.map((cat, i) => {
    const count = getProductsByCategory(cat.id).length;
    return `
      <div class="col-6 col-md-4 col-lg reveal" data-delay="${i % 4}">
        <a class="category-tile" href="${ROUTES.products}?category=${cat.id}">
          <img src="${cat.image}" alt="${cat.name}" loading="lazy" width="280" height="240">
          <span class="tile-count">${count} items</span>
          <div class="tile-body">
            <h3><i class="fas ${cat.icon} me-2"></i>${cat.name}</h3>
            <p>${cat.blurb}</p>
          </div>
        </a>
      </div>`;
  }).join('');

  observeReveals();
}

/* ==========================================================================
   2. FEATURED PRODUCTS + CATEGORY PILLS
   ========================================================================== */

/** Builds the filter pills from CATEGORIES (an "All" pill already exists). */
function renderFeaturedTabs() {
  const tabs = document.getElementById('featuredTabs');
  if (!tabs) return;

  tabs.insertAdjacentHTML('beforeend', CATEGORIES.map(cat =>
    `<button class="pill-tab" data-filter="${cat.id}" type="button">${cat.name}</button>`
  ).join(''));

  tabs.addEventListener('click', e => {
    const btn = e.target.closest('.pill-tab');
    if (!btn) return;

    tabs.querySelectorAll('.pill-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    renderFeatured(btn.dataset.filter);
  });
}

/**
 * Renders the featured grid, optionally narrowed to one category.
 * Falls back to the highest-rated products when a category has no
 * explicitly featured items, so a tab is never empty.
 */
function renderFeatured(filter = 'all') {
  const grid = document.getElementById('featuredGrid');
  if (!grid) return;

  let list = PRODUCTS.filter(p => p.featured);
  if (filter !== 'all') {
    const inCategory = list.filter(p => p.category === filter);
    list = inCategory.length
      ? inCategory
      : getProductsByCategory(filter).sort((a, b) => b.rating - a.rating);
  }

  list = list.slice(0, 8);

  if (!list.length) {
    grid.innerHTML = `
      <div class="col-12 empty-state">
        <h3>Nothing here yet</h3>
        <p>We're restocking this category. Check back soon.</p>
      </div>`;
    return;
  }

  renderProductGrid(list, grid);
}

/* ==========================================================================
   3. BEST SELLERS
   ========================================================================== */
function renderBestSellers() {
  const grid = document.getElementById('bestSellerGrid');
  if (!grid) return;

  const list = PRODUCTS
    .filter(p => p.bestSeller)
    .sort((a, b) => b.reviewCount - a.reviewCount)
    .slice(0, 8);

  renderProductGrid(list, grid);
}

/* ==========================================================================
   4. DEAL OF THE DAY
   --------------------------------------------------------------------------
   Picks the product with the largest percentage discount and counts down to
   midnight, so the demo always shows a live, ticking timer.
   ========================================================================== */
function initDealOfTheDay() {
  const nameEl = document.getElementById('dealName');
  if (!nameEl) return;

  // Highest discount wins
  const deal = [...PRODUCTS].sort((a, b) =>
    calcDiscount(b.price, b.oldPrice) - calcDiscount(a.price, a.oldPrice)
  )[0];

  const off = calcDiscount(deal.price, deal.oldPrice);

  document.getElementById('dealImage').src = deal.images[0];
  document.getElementById('dealImage').alt = deal.name;
  nameEl.textContent = deal.name;
  document.getElementById('dealTagline').textContent = deal.tagline;
  document.getElementById('dealPrice').textContent = formatPrice(deal.price);
  document.getElementById('dealOldPrice').textContent = formatPrice(deal.oldPrice);
  document.getElementById('dealOff').textContent = `${off}% OFF`;
  document.getElementById('dealLink').href = `${ROUTES.productDetails}?id=${deal.id}`;

  startCountdown();
}

/** Ticks the hours/minutes/seconds boxes down to midnight local time. */
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

    const diff = Math.max(0, midnight - now);
    const totalSeconds = Math.floor(diff / 1000);

    const pad = n => String(n).padStart(2, '0');
    boxes.h.textContent = pad(Math.floor(totalSeconds / 3600));
    boxes.m.textContent = pad(Math.floor((totalSeconds % 3600) / 60));
    boxes.s.textContent = pad(totalSeconds % 60);
  };

  tick();
  setInterval(tick, 1000);
}

/* ==========================================================================
   5. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  renderCategoryTiles();
  renderFeaturedTabs();
  renderFeatured('all');
  renderBestSellers();
  initDealOfTheDay();
});
