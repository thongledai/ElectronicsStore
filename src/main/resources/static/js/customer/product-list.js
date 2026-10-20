/* Danh sách sản phẩm Customer: mọi tìm kiếm/lọc/sắp xếp/phân trang chạy ở backend. */
'use strict';

(() => {
  const PAGE_SIZE = 12;
  const SORT_VALUES = new Set(['', 'oldest', 'price-asc', 'price-desc', 'rating', 'popular', 'discount']);
  const form = document.getElementById('productFilterForm');
  const grid = document.getElementById('productGrid');
  const resultCount = document.getElementById('resultCount');
  const pagination = document.getElementById('productPagination');
  const message = document.getElementById('filterMessage');
  const sortOrder = document.getElementById('sortOrder');

  if (!form || !grid || !resultCount || !pagination) return;

  // Keep the legacy category API load and use it to fill options if the page
  // was rendered without the server-provided category list.
  async function fetchCategories() {
    try {
      const response = await fetch('/api/categories', { headers: { Accept: 'application/json' } });
      const body = await response.json();
      if (!response.ok || body?.success !== true || !Array.isArray(body.data)) return;
      const existing = new Set(Array.from(fields.category.options, option => option.value));
      body.data.forEach(category => {
        if (category?.slug && !existing.has(category.slug)) {
          fields.category.add(new Option(category.name || category.slug, category.slug));
        }
      });
    } catch (error) {
      console.warn('Không tải được danh mục:', error);
    }
  }

  let currentPage = 1;
  let activeController = null;

  const fields = {
    q: document.getElementById('filterSearch'),
    category: document.getElementById('filterCategory'),
    brand: document.getElementById('filterBrand'),
    minPrice: document.getElementById('minPrice'),
    maxPrice: document.getElementById('maxPrice'),
    stockStatus: document.getElementById('filterStockStatus'),
    minRating: document.getElementById('minRating'),
    onSale: document.getElementById('onSale')
  };

  function escapeHtml(value) {
    const element = document.createElement('span');
    element.textContent = value == null ? '' : String(value);
    return element.innerHTML;
  }

  function formatMoney(value) {
    const amount = Number(value);
    if (!Number.isFinite(amount)) return 'Liên hệ';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency', currency: 'VND', maximumFractionDigits: 0
    }).format(amount);
  }

  function formatStars(value) {
    const rating = Math.max(0, Math.min(5, Number(value) || 0));
    const fullStars = Math.floor(rating);
    return `${'★'.repeat(fullStars)}${'☆'.repeat(5 - fullStars)} <span class="small text-muted">${rating.toFixed(1)}/5</span>`;
  }

  function readForm() {
    return {
      q: fields.q.value.trim(),
      category: fields.category.value,
      brand: fields.brand.value,
      minPrice: fields.minPrice.value.trim(),
      maxPrice: fields.maxPrice.value.trim(),
      stockStatus: fields.stockStatus.value,
      minRating: fields.minRating.value,
      onSale: fields.onSale.checked,
      sort: SORT_VALUES.has(sortOrder.value) ? sortOrder.value : ''
    };
  }

  function setFormFromUrl() {
    const params = new URLSearchParams(window.location.search);
    fields.q.value = (params.get('q') || params.get('search') || '').trim();
    restoreSelectValue(fields.category, params.get('category'), 'Danh mục không hợp lệ');
    restoreSelectValue(fields.brand, params.get('brand'), 'Thương hiệu không hợp lệ');
    fields.minPrice.value = params.get('minPrice') || '';
    fields.maxPrice.value = params.get('maxPrice') || '';
    fields.minRating.value = params.get('minRating') || '';
    fields.onSale.checked = params.get('onSale') === 'true';
    fields.stockStatus.value = params.get('outOfStock') === 'true'
      ? 'out-of-stock'
      : params.get('inStock') === 'true' ? 'in-stock' : '';
    const sort = params.get('sort') || '';
    sortOrder.value = SORT_VALUES.has(sort) ? sort : '';
    const page = Number.parseInt(params.get('page') || '1', 10);
    currentPage = Number.isInteger(page) && page > 0 ? page : 1;
  }

  function restoreSelectValue(select, value, label) {
    if (!value) {
      select.value = '';
      return;
    }
    const existing = Array.from(select.options).find(option => option.value === value);
    if (existing) {
      select.value = value;
      return;
    }
    select.add(new Option(label, value, true, true));
    select.value = value;
  }

  function writeUrl(page) {
    const filter = readForm();
    const url = new URL(window.location.href);
    const managed = ['q', 'search', 'category', 'brand', 'minPrice', 'maxPrice',
      'inStock', 'outOfStock', 'onSale', 'minRating', 'sort', 'page', 'size'];
    managed.forEach(key => url.searchParams.delete(key));

    if (filter.q) url.searchParams.set('q', filter.q);
    if (filter.category) url.searchParams.set('category', filter.category);
    if (filter.brand) url.searchParams.set('brand', filter.brand);
    if (filter.minPrice !== '') url.searchParams.set('minPrice', filter.minPrice);
    if (filter.maxPrice !== '') url.searchParams.set('maxPrice', filter.maxPrice);
    if (filter.stockStatus === 'in-stock') url.searchParams.set('inStock', 'true');
    if (filter.stockStatus === 'out-of-stock') url.searchParams.set('outOfStock', 'true');
    if (filter.onSale) url.searchParams.set('onSale', 'true');
    if (filter.minRating) url.searchParams.set('minRating', filter.minRating);
    if (filter.sort) url.searchParams.set('sort', filter.sort);
    if (page > 1) url.searchParams.set('page', String(page));
    window.history.pushState({}, '', url);
  }

  function apiUrl(page) {
    const filter = readForm();
    const params = new URLSearchParams({ page: String(page - 1), size: String(PAGE_SIZE) });
    if (filter.q) params.set('q', filter.q);
    if (filter.category) params.set('category', filter.category);
    if (filter.brand) params.set('brand', filter.brand);
    if (filter.minPrice !== '') params.set('minPrice', filter.minPrice);
    if (filter.maxPrice !== '') params.set('maxPrice', filter.maxPrice);
    if (filter.stockStatus === 'in-stock') params.set('inStock', 'true');
    if (filter.stockStatus === 'out-of-stock') params.set('outOfStock', 'true');
    if (filter.onSale) params.set('onSale', 'true');
    if (filter.minRating) params.set('minRating', filter.minRating);
    if (filter.sort) params.set('sort', filter.sort);
    return `/api/products?${params.toString()}`;
  }

  function renderProduct(product) {
    const currentPrice = product.effectivePrice ?? product.minPrice ?? product.maxPrice;
    const originalPrice = product.minPrice;
    const promotionalPrice = product.minPromotionalPrice;
    const hasDiscount = currentPrice != null && originalPrice != null && promotionalPrice != null
      && Number(promotionalPrice) < Number(originalPrice)
      && Number(currentPrice) === Number(promotionalPrice);
    const stock = Number(product.totalStock || 0);
    const name = escapeHtml(product.name || 'Sản phẩm');
    const brand = escapeHtml(product.brandName || 'Chưa phân loại');
    const category = escapeHtml(product.categoryName || 'Danh mục khác');
    const slug = String(product.slug || '');
    const href = slug ? `/customer/products/${encodeURIComponent(slug)}` : '/customer/products';
    const imageUrl = String(product.thumbnailUrl || '').trim();
    const validImageUrl = imageUrl.startsWith('/') || /^https?:\/\//i.test(imageUrl);
    const image = validImageUrl
      ? `<img src="${escapeHtml(imageUrl)}" alt="${name}" loading="lazy" width="600" height="600">`
      : '<div class="d-flex align-items-center justify-content-center h-100 text-muted" aria-label="Không có ảnh"><i class="fas fa-image fa-3x"></i></div>';
    const stockText = stock > 0
      ? `<p class="stock-note in">Còn hàng (${stock})</p>`
      : '<p class="stock-note">Hết hàng</p>';

    return `<div class="col-6 col-md-4 col-lg-3 product-col">
      <article class="product-card ${stock <= 0 ? 'is-out' : ''}" data-id="${escapeHtml(product.id)}">
        <div class="card-media">
          <a href="${href}" aria-label="Xem ${name}">${image}</a>
          ${stock <= 0 ? '<div class="stock-flag"><span>Hết hàng</span></div>' : ''}
        </div>
        <div class="card-body">
          <span class="card-cat">${brand} · ${category}</span>
          <h3 class="card-title"><a href="${href}">${name}</a></h3>
          <div class="card-rating"><span class="stars" aria-label="Đánh giá">${formatStars(product.rating)}</span></div>
          <div class="card-price">
            <span class="price">${currentPrice == null ? 'Liên hệ' : formatMoney(currentPrice)}</span>
            ${hasDiscount ? `<span class="price-old">${formatMoney(originalPrice)}</span>` : ''}
          </div>
          ${stockText}
          <button class="btn btn-brand btn-sm w-100 mt-auto js-add-cart" type="button"
                  data-slug="${escapeHtml(slug)}"${stock <= 0 || !slug ? ' disabled' : ''}>
            <i class="fas fa-cart-plus me-1"></i>Thêm vào giỏ
          </button>
        </div>
      </article>
    </div>`;
  }

  function renderGrid(products) {
    if (!products.length) {
      grid.innerHTML = `<div class="col-12"><div class="empty-state text-center py-5">
        <i class="fas fa-box-open fa-3x text-muted mb-3"></i>
        <h3 class="h5">Không tìm thấy sản phẩm phù hợp</h3>
        <p>Thử thay đổi từ khóa hoặc bỏ bớt điều kiện lọc.</p>
        <button class="btn btn-brand js-clear-filters" type="button">Xóa toàn bộ bộ lọc</button>
      </div></div>`;
      return;
    }
    grid.innerHTML = products.map(renderProduct).join('');
  }

  function renderPagination(totalPages, page) {
    pagination.innerHTML = '';
    if (totalPages <= 1) return;

    const addButton = (label, target, active = false, disabled = false, ariaLabel = '') => {
      const item = document.createElement('li');
      item.className = `page-item${active ? ' active' : ''}${disabled ? ' disabled' : ''}`;
      const button = document.createElement('button');
      button.className = 'page-link';
      button.type = 'button';
      button.textContent = label;
      button.dataset.page = String(target);
      if (ariaLabel) button.setAttribute('aria-label', ariaLabel);
      if (active) button.setAttribute('aria-current', 'page');
      button.disabled = disabled;
      item.appendChild(button);
      pagination.appendChild(item);
    };

    addButton('‹', page - 1, false, page <= 1, 'Trang trước');
    const start = Math.max(1, page - 2);
    const end = Math.min(totalPages, page + 2);
    if (start > 1) {
      addButton('1', 1, page === 1);
      if (start > 2) {
        const gap = document.createElement('li');
        gap.className = 'page-item disabled';
        gap.innerHTML = '<span class="page-link">…</span>';
        pagination.appendChild(gap);
      }
    }
    for (let p = start; p <= end; p++) addButton(String(p), p, p === page);
    if (end < totalPages) {
      if (end < totalPages - 1) {
        const gap = document.createElement('li');
        gap.className = 'page-item disabled';
        gap.innerHTML = '<span class="page-link">…</span>';
        pagination.appendChild(gap);
      }
      addButton(String(totalPages), totalPages, page === totalPages);
    }
    addButton('›', page + 1, false, page >= totalPages, 'Trang sau');
  }

  function showMessage(text, isError = true) {
    message.textContent = text || '';
    message.classList.toggle('text-danger', isError);
    message.classList.toggle('text-success', !isError);
  }

  async function loadPage(page, updateHistory = true) {
    const filter = readForm();
    if (filter.minPrice !== '' && (!Number.isFinite(Number(filter.minPrice)) || Number(filter.minPrice) < 0)) {
      showMessage('Giá tối thiểu phải là số không âm.');
      return;
    }
    if (filter.maxPrice !== '' && (!Number.isFinite(Number(filter.maxPrice)) || Number(filter.maxPrice) < 0)) {
      showMessage('Giá tối đa phải là số không âm.');
      return;
    }
    if (filter.minPrice !== '' && filter.maxPrice !== '' && Number(filter.minPrice) > Number(filter.maxPrice)) {
      showMessage('Giá tối thiểu không được lớn hơn giá tối đa.');
      return;
    }

    currentPage = Math.max(1, Number.parseInt(String(page), 10) || 1);
    if (updateHistory) writeUrl(currentPage);
    showMessage('');
    if (activeController) activeController.abort();
    activeController = new AbortController();
    grid.innerHTML = '<div class="col-12 text-center py-5"><i class="fas fa-spinner fa-spin fa-2x text-muted"></i><p class="mt-2">Đang tải sản phẩm...</p></div>';
    resultCount.textContent = 'Đang tìm sản phẩm...';
    pagination.innerHTML = '';

    try {
      const response = await fetch(apiUrl(currentPage), {
        headers: { Accept: 'application/json' },
        signal: activeController.signal
      });
      const body = await response.json();
      if (!response.ok || !body || body.success !== true || !body.data) {
        throw new Error(body?.message || 'Không thể tải danh sách sản phẩm.');
      }

      const data = body.data;
      const total = Number(data.totalElements || 0);
      const totalPages = Number(data.totalPages || 0);
      if (currentPage > Math.max(1, totalPages)) {
        currentPage = Math.max(1, totalPages);
        writeUrl(currentPage);
        return loadPage(currentPage, false);
      }

      const products = Array.isArray(data.content) ? data.content : [];
      const firstItem = total === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
      const lastItem = Math.min(currentPage * PAGE_SIZE, total);
      resultCount.textContent = `Hiển thị ${firstItem}–${lastItem} trong ${total} sản phẩm`;
      renderGrid(products);
      renderPagination(totalPages, currentPage);
    } catch (error) {
      if (error.name === 'AbortError') return;
      console.error('Lỗi tải danh sách sản phẩm:', error);
      grid.innerHTML = '<div class="col-12"><div class="alert alert-danger" role="alert">Không tải được sản phẩm. Vui lòng thử lại.</div></div>';
      resultCount.textContent = 'Không thể tải danh sách sản phẩm';
      showMessage(error.message || 'Có lỗi xảy ra khi tải sản phẩm.');
    }
  }

  form.addEventListener('submit', event => {
    event.preventDefault();
    loadPage(1);
  });

  sortOrder.addEventListener('change', () => loadPage(1));
  pagination.addEventListener('click', event => {
    const button = event.target.closest('button[data-page]');
    if (!button || button.disabled) return;
    loadPage(Number(button.dataset.page));
    window.scrollTo({ top: grid.getBoundingClientRect().top + window.scrollY - 120, behavior: 'smooth' });
  });

  document.addEventListener('click', event => {
    const clearButton = event.target.closest('.js-clear-filters');
    if (!clearButton) return;
    event.preventDefault();
    form.reset();
    sortOrder.value = '';
    currentPage = 1;
    loadPage(1);
  });

    window.addEventListener('popstate', () => {
        setFormFromUrl();
        loadPage(currentPage, false);
    });

    /* ===== ADD TO CART: mặc định 1 sản phẩm, variant giá cao nhất còn hàng ===== */
    const addingSlugs = new Set();

    function notify(title, text, type) {
        if (window.showToast) window.showToast(title, text, type);
        else alert(text);
    }

    function effectivePrice(variant) {
        const promo = Number(variant.promotionalPrice);
        return promo > 0 ? promo : (Number(variant.price) || 0);
    }

    function pickDefaultVariant(variants) {
        const inStock = variants.filter(v => Number(v.quantity) > 0);
        if (!inStock.length) return null;
        return inStock.reduce((best, v) => effectivePrice(v) > effectivePrice(best) ? v : best);
    }

    async function addToCart(button) {
        const slug = button.dataset.slug;
        if (!slug || addingSlugs.has(slug)) return;
        addingSlugs.add(slug);

        const oldHtml = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';

        try {
            const productRes = await fetch('/api/products/' + encodeURIComponent(slug), {
                headers: { Accept: 'application/json' }
            });
            if (!productRes.ok) throw new Error('HTTP ' + productRes.status);
            const productBody = await productRes.json();
            const variants = productBody?.success && productBody.data?.variants;

            if (!Array.isArray(variants) || !variants.length) {
                notify('Thông báo', 'Sản phẩm chưa có phiên bản để mua', 'warning');
                return;
            }
            const variant = pickDefaultVariant(variants);
            if (!variant) {
                notify('Thông báo', 'Sản phẩm tạm hết hàng', 'warning');
                return;
            }

            const res = await fetch('/cart/items', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
                body: JSON.stringify({ productVariantId: variant.id, count: 1 })
            });

            if (res.status === 401) {
                window.location.href = '/login?error=unauthorized';
                return;
            }

            let body = null;
            try { body = await res.json(); } catch (e) { /* không có body JSON */ }

            if (!res.ok || !body || body.success === false) {
                notify('Lỗi', body?.message || 'Không thể thêm vào giỏ hàng', 'danger');
                return;
            }

            notify('Đã thêm', body.message || 'Đã thêm vào giỏ hàng', 'success');
            if (typeof refreshCartBadge === 'function') refreshCartBadge();
        } catch (error) {
            console.error('Lỗi thêm vào giỏ:', error);
            notify('Lỗi', 'Không thể kết nối máy chủ', 'danger');
        } finally {
            addingSlugs.delete(slug);
            button.disabled = false;
            button.innerHTML = oldHtml;
        }
    }

    grid.addEventListener('click', event => {
        const button = event.target.closest('.js-add-cart');
        if (!button || button.disabled) return;
        event.preventDefault();
        event.stopPropagation();
        addToCart(button);
    });

    setFormFromUrl();
    fetchCategories().then(() => {
        setFormFromUrl();
        loadPage(currentPage, false);
    });
})();
