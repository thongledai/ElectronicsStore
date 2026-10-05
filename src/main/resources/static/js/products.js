/* ==========================================================================
   TechNova Electronics — Products Listing Page
   --------------------------------------------------------------------------
   Implements search, category / brand / price / rating filtering, sorting,
   grid-vs-list views and pagination. All filtering runs client-side over the
   PRODUCTS array from js/data.js.

   URL parameters supported (so links from the home page and nav work):
     ?category=laptops      pre-select a category filter
     ?search=nova           pre-fill the search box
     ?sort=popular          pre-select a sort order
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. STATE
   ========================================================================== */
const state = {
  search: '',
  categories: [],     // array of category ids
  brands: [],         // array of brand names
  maxPrice: 250000,
  minRating: 0,
  inStockOnly: false,
  onSaleOnly: false,
  sort: 'featured',
  view: 'grid',
  page: 1,
  perPage: 12
};

/* Cached DOM references */
const els = {};

/* Upper bound of the price slider. Calculated from the catalogue at init so
   the "max price" reset always matches the slider's real maximum. */
let PRICE_CEILING = 250000;

/* ==========================================================================
   2. FILTER PANEL BUILDERS
   ========================================================================== */

/** Renders one checkbox row used by the category and brand filters. */
function checkboxRow(value, label, count, group) {
  return `
    <label class="filter-check">
      <input type="checkbox" value="${value}" data-group="${group}">
      <span>${label}</span>
      <span class="count">${count}</span>
    </label>`;
}

function buildCategoryFilters() {
  els.categoryFilters.innerHTML = CATEGORIES
    .map(c => checkboxRow(c.id, c.name, getProductsByCategory(c.id).length, 'category'))
    .join('');
}

function buildBrandFilters() {
  // Unique brand list with product counts, alphabetically sorted
  const counts = {};
  PRODUCTS.forEach(p => { counts[p.brand] = (counts[p.brand] || 0) + 1; });

  els.brandFilters.innerHTML = Object.keys(counts)
    .sort()
    .map(brand => checkboxRow(brand, brand, counts[brand], 'brand'))
    .join('');
}

function buildRatingFilters() {
  // 4★ & up, 3★ & up, etc.
  els.ratingFilters.innerHTML = [4, 3, 2].map(min => `
    <label class="filter-check">
      <input type="radio" name="ratingFilter" value="${min}">
      <span class="rating-row">
        <span class="stars">${renderStars(min)}</span>
        <span>&amp; up</span>
      </span>
      <span class="count">${PRODUCTS.filter(p => p.rating >= min).length}</span>
    </label>`).join('') + `
    <label class="filter-check">
      <input type="radio" name="ratingFilter" value="0" checked>
      <span>All ratings</span>
      <span class="count">${PRODUCTS.length}</span>
    </label>`;
}

/* ==========================================================================
   3. FILTERING & SORTING
   ========================================================================== */

/** Applies every active filter to PRODUCTS and returns the matching subset. */
function getFilteredProducts() {
  const query = state.search.trim().toLowerCase();

  let list = PRODUCTS.filter(p => {
    // Text search across name, brand, category and tagline
    if (query) {
      const haystack = [
        p.name, p.brand, getCategoryName(p.category), p.tagline
      ].join(' ').toLowerCase();
      if (!haystack.includes(query)) return false;
    }

    if (state.categories.length && !state.categories.includes(p.category)) return false;
    if (state.brands.length && !state.brands.includes(p.brand)) return false;
    if (p.price > state.maxPrice) return false;
    if (p.rating < state.minRating) return false;
    if (state.inStockOnly && p.stock === 0) return false;
    if (state.onSaleOnly && calcDiscount(p.price, p.oldPrice) === 0) return false;

    return true;
  });

  return sortProducts(list);
}

/** Returns a new sorted array according to state.sort. */
function sortProducts(list) {
  const sorted = [...list];

  switch (state.sort) {
    case 'price-asc':
      return sorted.sort((a, b) => a.price - b.price);
    case 'price-desc':
      return sorted.sort((a, b) => b.price - a.price);
    case 'rating':
      return sorted.sort((a, b) => b.rating - a.rating || b.reviewCount - a.reviewCount);
    case 'popular':
      return sorted.sort((a, b) => b.reviewCount - a.reviewCount);
    case 'discount':
      return sorted.sort((a, b) =>
        calcDiscount(b.price, b.oldPrice) - calcDiscount(a.price, a.oldPrice));
    case 'name-asc':
      return sorted.sort((a, b) => a.name.localeCompare(b.name));
    case 'name-desc':
      return sorted.sort((a, b) => b.name.localeCompare(a.name));
    case 'featured':
    default:
      // Featured first, then best sellers, then by rating
      return sorted.sort((a, b) =>
        (b.featured - a.featured) || (b.bestSeller - a.bestSeller) || (b.rating - a.rating));
  }
}

/* ==========================================================================
   4. RENDERING
   ========================================================================== */

/** Main render pass: grid, result count, chips and pagination. */
function render() {
  const filtered = getFilteredProducts();
  const totalPages = Math.max(1, Math.ceil(filtered.length / state.perPage));

  // Guard against landing on a page that no longer exists after filtering
  if (state.page > totalPages) state.page = totalPages;

  const start = (state.page - 1) * state.perPage;
  const pageItems = filtered.slice(start, start + state.perPage);

  renderResultCount(filtered.length, start, pageItems.length);
  renderGrid(pageItems, filtered.length);
  renderChips();
  renderPagination(totalPages);
}

function renderResultCount(total, start, shown) {
  if (!total) {
    els.resultCount.innerHTML = 'No products match your filters';
    return;
  }
  els.resultCount.innerHTML =
    `Showing <strong>${start + 1}–${start + shown}</strong> of <strong>${total}</strong> product${total === 1 ? '' : 's'}`;
}

function renderGrid(items, totalMatches) {
  if (!items.length) {
    els.productGrid.innerHTML = `
      <div class="col-12">
        <div class="empty-state">
          <img src="${IMG_BASE}empty-cart.svg" alt="">
          <h3>No products found</h3>
          <p>We couldn't find anything matching those filters. Try widening your price range or clearing a filter.</p>
          <button class="btn btn-brand" id="emptyClear" type="button">
            <i class="fas fa-rotate-left me-2"></i>Clear all filters
          </button>
        </div>
      </div>`;
    const clearBtn = document.getElementById('emptyClear');
    if (clearBtn) clearBtn.addEventListener('click', clearAllFilters);
    return;
  }

  renderProductGrid(items, els.productGrid);
  els.productGrid.classList.toggle('view-list', state.view === 'list');
}

/** Shows removable chips for each active filter. */
function renderChips() {
  const chips = [];

  if (state.search) {
    chips.push({ label: `Search: "${state.search}"`, type: 'search' });
  }
  state.categories.forEach(c => chips.push({ label: getCategoryName(c), type: 'category', value: c }));
  state.brands.forEach(b => chips.push({ label: b, type: 'brand', value: b }));
  if (state.maxPrice < PRICE_CEILING) chips.push({ label: `Under ${formatPrice(state.maxPrice)}`, type: 'price' });
  if (state.minRating > 0) chips.push({ label: `${state.minRating}★ & up`, type: 'rating' });
  if (state.inStockOnly) chips.push({ label: 'In stock only', type: 'stock' });
  if (state.onSaleOnly) chips.push({ label: 'On sale', type: 'sale' });

  if (!chips.length) {
    els.activeChips.innerHTML = '';
    return;
  }

  els.activeChips.innerHTML = chips.map(chip => `
    <span class="filter-chip">
      ${escapeHtml(chip.label)}
      <button type="button" data-type="${chip.type}" data-value="${chip.value || ''}"
              aria-label="Remove filter ${escapeHtml(chip.label)}">
        <i class="fas fa-xmark"></i>
      </button>
    </span>`).join('') + `
    <button class="filter-chip" id="chipClearAll" type="button"
            style="background:var(--surface-3);color:var(--text-muted)">
      Clear all <i class="fas fa-xmark"></i>
    </button>`;
}

function renderPagination(totalPages) {
  if (totalPages <= 1) {
    els.pagination.innerHTML = '';
    return;
  }

  let html = `
    <li class="page-item ${state.page === 1 ? 'disabled' : ''}">
      <button class="page-link" data-page="${state.page - 1}" aria-label="Previous page">
        <i class="fas fa-chevron-left"></i>
      </button>
    </li>`;

  for (let i = 1; i <= totalPages; i++) {
    html += `
      <li class="page-item ${i === state.page ? 'active' : ''}">
        <button class="page-link" data-page="${i}">${i}</button>
      </li>`;
  }

  html += `
    <li class="page-item ${state.page === totalPages ? 'disabled' : ''}">
      <button class="page-link" data-page="${state.page + 1}" aria-label="Next page">
        <i class="fas fa-chevron-right"></i>
      </button>
    </li>`;

  els.pagination.innerHTML = html;
}

/* ==========================================================================
   5. FILTER ACTIONS
   ========================================================================== */

/** Resets every filter back to its default and re-renders. */
function clearAllFilters() {
  state.search = '';
  state.categories = [];
  state.brands = [];
  state.maxPrice = PRICE_CEILING;
  state.minRating = 0;
  state.inStockOnly = false;
  state.onSaleOnly = false;
  state.page = 1;

  // Reset the form controls to match
  els.filterSearch.value = '';
  els.priceRange.value = PRICE_CEILING;
  els.priceValue.textContent = formatPrice(PRICE_CEILING);
  els.inStockOnly.checked = false;
  els.onSaleOnly.checked = false;
  document.querySelectorAll('#categoryFilters input, #brandFilters input').forEach(cb => cb.checked = false);
  const allRatings = document.querySelector('input[name="ratingFilter"][value="0"]');
  if (allRatings) allRatings.checked = true;

  updatePageTitle();
  render();
  showToast('Filters cleared', 'Showing all products again.', 'info', 2000);
}

/** Removes a single filter identified by a chip's type/value. */
function removeFilter(type, value) {
  switch (type) {
    case 'search':
      state.search = '';
      els.filterSearch.value = '';
      break;
    case 'category':
      state.categories = state.categories.filter(c => c !== value);
      document.querySelectorAll('#categoryFilters input').forEach(cb => {
        if (cb.value === value) cb.checked = false;
      });
      break;
    case 'brand':
      state.brands = state.brands.filter(b => b !== value);
      document.querySelectorAll('#brandFilters input').forEach(cb => {
        if (cb.value === value) cb.checked = false;
      });
      break;
    case 'price':
      state.maxPrice = PRICE_CEILING;
      els.priceRange.value = PRICE_CEILING;
      els.priceValue.textContent = formatPrice(PRICE_CEILING);
      break;
    case 'rating': {
      state.minRating = 0;
      const allRatings = document.querySelector('input[name="ratingFilter"][value="0"]');
      if (allRatings) allRatings.checked = true;
      break;
    }
    case 'stock':
      state.inStockOnly = false;
      els.inStockOnly.checked = false;
      break;
    case 'sale':
      state.onSaleOnly = false;
      els.onSaleOnly.checked = false;
      break;
  }

  state.page = 1;
  updatePageTitle();
  render();
}

/** Updates the h1 and breadcrumb to reflect a single-category view. */
function updatePageTitle() {
  const title = document.getElementById('pageTitle');
  const crumb = document.getElementById('crumbCurrent');
  if (!title) return;

  if (state.categories.length === 1) {
    const name = getCategoryName(state.categories[0]);
    title.textContent = name;
    crumb.textContent = name;
    document.title = `${name} — TechNova Electronics`;
  } else if (state.search) {
    title.textContent = `Results for "${state.search}"`;
    crumb.textContent = 'Search';
    document.title = `Search: ${state.search} — TechNova Electronics`;
  } else {
    title.textContent = 'All Products';
    crumb.textContent = 'Products';
    document.title = 'All Products — TechNova Electronics';
  }
}

/* ==========================================================================
   6. EVENT WIRING
   ========================================================================== */

/** Small debounce so typing in the search box doesn't re-render on every key. */
function debounce(fn, delay = 250) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

function bindEvents() {

  // --- Search within results -------------------------------------------
  els.filterSearch.addEventListener('input', debounce(e => {
    state.search = e.target.value;
    state.page = 1;
    updatePageTitle();
    render();
  }, 250));

  // --- Category & brand checkboxes -------------------------------------
  document.querySelectorAll('#categoryFilters, #brandFilters').forEach(container => {
    container.addEventListener('change', e => {
      if (e.target.type !== 'checkbox') return;

      const group = e.target.dataset.group;
      const target = group === 'category' ? state.categories : state.brands;
      const value = e.target.value;

      if (e.target.checked) {
        if (!target.includes(value)) target.push(value);
      } else {
        const index = target.indexOf(value);
        if (index > -1) target.splice(index, 1);
      }

      state.page = 1;
      updatePageTitle();
      render();
    });
  });

  // --- Price slider ------------------------------------------------------
  els.priceRange.addEventListener('input', e => {
    state.maxPrice = Number(e.target.value);
    els.priceValue.textContent = formatPrice(state.maxPrice);
  });
  // Only re-render when the user lets go — keeps dragging smooth
  els.priceRange.addEventListener('change', () => {
    state.page = 1;
    render();
  });

  // --- Price presets -----------------------------------------------------
  document.querySelectorAll('.price-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      state.maxPrice = Number(btn.dataset.max);
      els.priceRange.value = state.maxPrice;
      els.priceValue.textContent = formatPrice(state.maxPrice);
      state.page = 1;
      render();
    });
  });

  // --- Rating radios -----------------------------------------------------
  els.ratingFilters.addEventListener('change', e => {
    if (e.target.name !== 'ratingFilter') return;
    state.minRating = Number(e.target.value);
    state.page = 1;
    render();
  });

  // --- Availability checkboxes -------------------------------------------
  els.inStockOnly.addEventListener('change', e => {
    state.inStockOnly = e.target.checked;
    state.page = 1;
    render();
  });
  els.onSaleOnly.addEventListener('change', e => {
    state.onSaleOnly = e.target.checked;
    state.page = 1;
    render();
  });

  // --- Sort --------------------------------------------------------------
  els.sortSelect.addEventListener('change', e => {
    state.sort = e.target.value;
    state.page = 1;
    render();
  });

  // --- Grid / list view --------------------------------------------------
  els.gridViewBtn.addEventListener('click', () => setView('grid'));
  els.listViewBtn.addEventListener('click', () => setView('list'));

  // --- Clear all ---------------------------------------------------------
  els.clearFilters.addEventListener('click', clearAllFilters);

  // --- Chip removal (delegated) ------------------------------------------
  els.activeChips.addEventListener('click', e => {
    if (e.target.closest('#chipClearAll')) return clearAllFilters();

    const btn = e.target.closest('button[data-type]');
    if (btn) removeFilter(btn.dataset.type, btn.dataset.value);
  });

  // --- Pagination (delegated) --------------------------------------------
  els.pagination.addEventListener('click', e => {
    const btn = e.target.closest('button[data-page]');
    if (!btn || btn.closest('.disabled')) return;

    state.page = Number(btn.dataset.page);
    render();

    // Scroll back up to the top of the grid for the new page
    document.querySelector('.toolbar').scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
}

function setView(view) {
  state.view = view;
  els.gridViewBtn.classList.toggle('active', view === 'grid');
  els.listViewBtn.classList.toggle('active', view === 'list');
  els.productGrid.classList.toggle('view-list', view === 'list');
}

/* ==========================================================================
   7. URL PARAMETERS
   --------------------------------------------------------------------------
   Lets other pages deep-link into a filtered view.
   ========================================================================== */
function applyUrlParams() {
  const category = getParam('category');
  const search = getParam('search');
  const sort = getParam('sort');

  if (category && CATEGORIES.some(c => c.id === category)) {
    state.categories = [category];
    document.querySelectorAll('#categoryFilters input').forEach(cb => {
      if (cb.value === category) cb.checked = true;
    });
  }

  if (search) {
    state.search = search;
    els.filterSearch.value = search;
  }

  if (sort && [...els.sortSelect.options].some(o => o.value === sort)) {
    state.sort = sort;
    els.sortSelect.value = sort;
  }

  updatePageTitle();
}

/* ==========================================================================
   8. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  // Cache DOM nodes once
  Object.assign(els, {
    productGrid: document.getElementById('productGrid'),
    resultCount: document.getElementById('resultCount'),
    activeChips: document.getElementById('activeChips'),
    pagination: document.getElementById('pagination'),
    categoryFilters: document.getElementById('categoryFilters'),
    brandFilters: document.getElementById('brandFilters'),
    ratingFilters: document.getElementById('ratingFilters'),
    filterSearch: document.getElementById('filterSearch'),
    priceRange: document.getElementById('priceRange'),
    priceValue: document.getElementById('priceValue'),
    inStockOnly: document.getElementById('inStockOnly'),
    onSaleOnly: document.getElementById('onSaleOnly'),
    sortSelect: document.getElementById('sortSelect'),
    gridViewBtn: document.getElementById('gridViewBtn'),
    listViewBtn: document.getElementById('listViewBtn'),
    clearFilters: document.getElementById('clearFilters')
  });

  // Set the slider ceiling to the most expensive product, rounded up
  const maxProductPrice = Math.max(...PRODUCTS.map(p => p.price));
  PRICE_CEILING = Math.ceil(maxProductPrice / 10000) * 10000;
  els.priceRange.max = PRICE_CEILING;
  els.priceRange.value = PRICE_CEILING;
  state.maxPrice = PRICE_CEILING;
  els.priceValue.textContent = formatPrice(PRICE_CEILING);

  buildCategoryFilters();
  buildBrandFilters();
  buildRatingFilters();
  bindEvents();
  applyUrlParams();
  render();
});
