/* ==========================================================================
   TechNova Electronics — Wishlist Page (Kết nối backend /api/wishlist)
   ========================================================================== */

'use strict';

let wishlistItems = [];
let isLoading = false;

/* ---------- Tiện ích định dạng ---------- */
function formatMoney(amount) {
  const val = Number(amount) || 0;
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val);
}

function escapeHtml(str) {
  return String(str ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

function renderStars(rating) {
  const r = Math.max(0, Math.min(5, Number(rating) || 0));
  const full = Math.floor(r);
  return '★'.repeat(full) + '☆'.repeat(5 - full);
}

function calcDiscount(currentPrice, oldPrice) {
  const cur = Number(currentPrice) || 0;
  const old = Number(oldPrice) || 0;
  if (old > cur && old > 0) {
    return Math.round(((old - cur) / old) * 100);
  }
  return 0;
}

/* ==========================================================================
   1. RENDER & DATA FETCHING
   ========================================================================== */
async function loadWishlist() {
  try {
    isLoading = true;
    const res = await fetch('/api/wishlist', {
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    });

    if (res.status === 401) {
      window.location.href = '/login?error=unauthorized';
      return;
    }

    const body = await res.json();
    if (!res.ok || !body.success) {
      throw new Error(body?.message || 'Không thể tải danh sách yêu thích.');
    }

    wishlistItems = Array.isArray(body.data) ? body.data : [];
    renderWishlist();
  } catch (err) {
    console.error('Lỗi tải wishlist:', err);
    wishlistItems = [];
    renderWishlist();
    if (typeof showToast === 'function') {
      showToast('Thông báo', err.message || 'Không thể tải danh sách yêu thích.', 'warning');
    }
  } finally {
    isLoading = false;
  }
}

function renderWishlist() {
  const layout = document.getElementById('wishlistLayout');
  const empty = document.getElementById('wishlistEmpty');
  if (!layout || !empty) return;

  const count = wishlistItems.length;
  const isEmpty = count === 0;

  if (isEmpty) {
    layout.hidden = true;
    layout.style.display = 'none';
    empty.hidden = false;
    empty.style.display = 'block';
  } else {
    layout.hidden = false;
    layout.style.display = 'block';
    empty.hidden = true;
    empty.style.display = 'none';
  }

  // Cập nhật badge trên header
  if (typeof setBadge === 'function') {
    setBadge('.js-wishlist-count', count);
  }

  if (isEmpty) {
    renderRecommendations();
    return;
  }

  const countEl = document.getElementById('wishlistCount');
  if (countEl) {
    countEl.innerHTML = `<strong>${count}</strong> sản phẩm đã lưu`;
  }

  renderValueStrip(wishlistItems);

  const grid = document.getElementById('wishlistGrid');
  if (grid) {
    grid.innerHTML = wishlistItems
      .map((item, i) => wishlistCard(item, i % 4))
      .join('');
  }

  renderRecommendations();
}

/** Thẻ sản phẩm trong trang danh sách yêu thích (không dùng .reveal để tránh lỗi opacity: 0) */
function wishlistCard(product, delay) {
  const discount = calcDiscount(product.price, product.originalPrice);
  const outOfStock = Number(product.stock || 0) <= 0;
  const href = product.slug ? `/customer/products/${encodeURIComponent(product.slug)}` : '/customer/products';
  const imgUrl = product.thumbnailUrl || '/images/placeholder.webp';

  return `
  <div class="col-6 col-md-4 col-lg-3">
    <article class="product-card ${outOfStock ? 'is-out' : ''}" data-id="${escapeHtml(product.productId)}">

      <div class="card-media">
        ${discount > 0 ? `<span class="badge-discount">-${discount}%</span>` : ''}
        <a href="${href}" aria-label="Xem ${escapeHtml(product.name)}">
          <img src="${escapeHtml(imgUrl)}" alt="${escapeHtml(product.name)}" loading="lazy" width="600" height="600"
               onerror="this.onerror=null;this.src='/images/placeholder.webp';">
        </a>
        ${outOfStock ? '<div class="stock-flag"><span>Hết hàng</span></div>' : ''}

        <div class="card-actions">
          <button class="card-action-btn js-remove-wish" data-id="${escapeHtml(product.productId)}" type="button"
                  aria-label="Xóa ${escapeHtml(product.name)} khỏi yêu thích" title="Xóa khỏi yêu thích">
            <i class="fas fa-trash-can"></i>
          </button>
          <a class="card-action-btn" href="${href}" aria-label="Xem chi tiết" title="Xem chi tiết">
            <i class="fas fa-eye"></i>
          </a>
        </div>
      </div>

      <div class="card-body">
        <span class="card-cat">${escapeHtml(product.brandName || '')} · ${escapeHtml(product.categoryName || '')}</span>
        <h3 class="card-title"><a href="${href}">${escapeHtml(product.name)}</a></h3>

        <div class="card-rating">
          <span class="stars">${renderStars(product.rating)}</span>
          <span class="count">${(Number(product.rating) || 0).toFixed(1)}</span>
        </div>

        <div class="card-price">
          <span class="price">${formatMoney(product.price)}</span>
          ${product.originalPrice && product.originalPrice > product.price ? `<span class="price-old">${formatMoney(product.originalPrice)}</span>` : ''}
        </div>

        <button class="btn btn-brand btn-sm btn-cart js-move-cart"
                data-id="${escapeHtml(product.productId)}"
                data-variant-id="${escapeHtml(product.defaultVariantId || '')}"
                data-slug="${escapeHtml(product.slug)}"
                type="button" ${outOfStock ? 'disabled' : ''}>
          <i class="fas fa-cart-plus me-1"></i>${outOfStock ? 'Hết hàng' : 'Chuyển vào giỏ'}
        </button>
      </div>

    </article>
  </div>`;
}

/** Thanh hiển thị tổng giá trị */
function renderValueStrip(items) {
  const total = items.reduce((sum, p) => sum + (Number(p.price) || 0), 0);
  const mrp = items.reduce((sum, p) => sum + (Number(p.originalPrice) || Number(p.price) || 0), 0);
  const savings = mrp - total;

  const valEl = document.getElementById('wishlistValue');
  const mrpEl = document.getElementById('wishlistMrp');
  const savingsEl = document.getElementById('wishlistSavings');

  if (valEl) valEl.textContent = formatMoney(total);

  if (savingsEl && mrpEl) {
    if (savings > 0) {
      mrpEl.textContent = formatMoney(mrp);
      savingsEl.textContent = `Tiết kiệm ${formatMoney(savings)}`;
      savingsEl.hidden = false;
    } else {
      mrpEl.textContent = '';
      savingsEl.hidden = true;
    }
  }
}

/* ==========================================================================
   2. RECOMMENDATIONS
   ========================================================================== */
async function renderRecommendations() {
  const grid = document.getElementById('recommendGrid');
  const section = document.getElementById('recommendSection');
  if (!grid || !section) return;

  try {
    const res = await fetch('/api/products?sort=popular&size=4', {
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    });
    if (!res.ok) {
      section.hidden = true;
      return;
    }
    const body = await res.json();
    const products = body?.data?.content || body?.data || [];

    if (!Array.isArray(products) || !products.length) {
      section.hidden = true;
      return;
    }

    section.hidden = false;
    grid.innerHTML = products.map((p, i) => {
      const curPrice = p.effectivePrice ?? p.minPrice ?? 0;
      const oldPrice = p.minPrice;
      const discount = calcDiscount(curPrice, oldPrice);
      const stock = Number(p.totalStock || 0);
      const href = p.slug ? `/customer/products/${encodeURIComponent(p.slug)}` : '/customer/products';
      const img = p.thumbnailUrl || '/images/placeholder.webp';

      return `
      <div class="col-6 col-md-4 col-lg-3">
        <article class="product-card ${stock <= 0 ? 'is-out' : ''}" data-id="${escapeHtml(p.id)}">
          <div class="card-media">
            ${discount > 0 ? `<span class="badge-discount">-${discount}%</span>` : ''}
            <a href="${href}"><img src="${escapeHtml(img)}" alt="${escapeHtml(p.name)}" loading="lazy" width="600" height="600" onerror="this.onerror=null;this.src='/images/placeholder.webp';"></a>
            <div class="card-actions">
              <button class="card-action-btn js-wishlist" data-id="${escapeHtml(p.id)}" type="button" title="Yêu thích">
                <i class="far fa-heart"></i>
              </button>
              <a class="card-action-btn" href="${href}" title="Xem chi tiết"><i class="fas fa-eye"></i></a>
            </div>
          </div>
          <div class="card-body">
            <span class="card-cat">${escapeHtml(p.brandName || '')} · ${escapeHtml(p.categoryName || '')}</span>
            <h3 class="card-title"><a href="${href}">${escapeHtml(p.name)}</a></h3>
            <div class="card-rating"><span class="stars">${renderStars(p.rating)}</span></div>
            <div class="card-price">
              <span class="price">${formatMoney(curPrice)}</span>
              ${discount > 0 ? `<span class="price-old">${formatMoney(oldPrice)}</span>` : ''}
            </div>
          </div>
        </article>
      </div>`;
    }).join('');

    if (typeof Wishlist !== 'undefined' && typeof Wishlist.syncButtons === 'function') {
      Wishlist.syncButtons();
    }
  } catch (e) {
    section.hidden = true;
  }
}

/* ==========================================================================
   3. SỰ KIỆN TƯƠNG TÁC
   ========================================================================== */
function bindWishlistEvents() {

  // Xóa 1 sản phẩm khỏi yêu thích
  const grid = document.getElementById('wishlistGrid');
  if (grid) {
    grid.addEventListener('click', async e => {
      const btn = e.target.closest('.js-remove-wish');
      if (!btn) return;

      const productId = btn.dataset.id;
      if (!productId) return;

      const cardCol = btn.closest('.col-6, .col-md-4, .col-lg-3');
      if (cardCol) {
        cardCol.style.transition = 'opacity .25s ease, transform .25s ease';
        cardCol.style.opacity = '0';
        cardCol.style.transform = 'scale(.94)';
      }

      try {
        const res = await fetch(`/api/wishlist/${encodeURIComponent(productId)}`, {
          method: 'DELETE',
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json' }
        });

        if (res.status === 401) {
          window.location.href = '/login?error=unauthorized';
          return;
        }

        const body = await res.json();
        if (!res.ok || !body.success) {
          throw new Error(body?.message || 'Không thể xóa sản phẩm.');
        }

        wishlistItems = wishlistItems.filter(p => String(p.productId) !== String(productId));
        if (typeof Wishlist !== 'undefined') {
          Wishlist._ids.delete(String(productId));
        }

        renderWishlist();

        if (typeof showToast === 'function') {
          showToast('Đã xóa', 'Đã xóa sản phẩm khỏi danh sách yêu thích.', 'info');
        }
      } catch (err) {
        if (cardCol) {
          cardCol.style.opacity = '1';
          cardCol.style.transform = 'none';
        }
        if (typeof showToast === 'function') {
          showToast('Lỗi', err.message || 'Không thể xóa sản phẩm.', 'danger');
        }
      }
    });

    // Chuyển 1 sản phẩm vào giỏ hàng
    grid.addEventListener('click', async e => {
      const moveBtn = e.target.closest('.js-move-cart');
      if (!moveBtn) return;

      const productId = moveBtn.dataset.id;
      let variantId = moveBtn.dataset.variantId;
      const slug = moveBtn.dataset.slug;

      moveBtn.disabled = true;
      const originalText = moveBtn.innerHTML;
      moveBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xử lý...';

      try {
        // Nếu chưa có variantId trên button, fetch lấy biến thể phù hợp
        if (!variantId && slug) {
          const prodRes = await fetch(`/api/products/${encodeURIComponent(slug)}`, {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
          });
          const prodData = await prodRes.json();
          const variants = prodData?.data?.variants || [];
          const activeVariant = variants.find(v => (v.quantity == null || v.quantity > 0) && v.selling !== false);
          if (activeVariant) {
            variantId = activeVariant.id;
          }
        }

        if (!variantId) {
          window.location.href = `/customer/products/${encodeURIComponent(slug)}`;
          return;
        }

        // Thêm vào giỏ hàng với `count: 1`
        const cartRes = await fetch('/cart/items', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
          body: JSON.stringify({ productVariantId: variantId, count: 1 })
        });

        if (cartRes.status === 401) {
          window.location.href = '/login?error=unauthorized';
          return;
        }

        const cartBody = await cartRes.json();
        if (!cartRes.ok || !cartBody.success) {
          throw new Error(cartBody?.message || 'Không thể thêm vào giỏ hàng.');
        }

        // Xóa khỏi wishlist sau khi đã thêm giỏ hàng thành công
        await fetch(`/api/wishlist/${encodeURIComponent(productId)}`, {
          method: 'DELETE',
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json' }
        });

        wishlistItems = wishlistItems.filter(p => String(p.productId) !== String(productId));
        if (typeof Wishlist !== 'undefined') {
          Wishlist._ids.delete(String(productId));
        }

        renderWishlist();

        if (typeof refreshCartBadge === 'function') {
          refreshCartBadge();
        }

        if (typeof showToast === 'function') {
          showToast('Thành công', 'Đã chuyển sản phẩm vào giỏ hàng!', 'success');
        }
      } catch (err) {
        moveBtn.disabled = false;
        moveBtn.innerHTML = originalText;
        if (typeof showToast === 'function') {
          showToast('Thông báo', err.message || 'Chuyển vào giỏ hàng thất bại.', 'warning');
        }
      }
    });
  }

  // Chuyển tất cả sản phẩm còn hàng vào giỏ hàng
  const moveAllBtn = document.getElementById('moveAllBtn');
  if (moveAllBtn) {
    moveAllBtn.addEventListener('click', async () => {
      const inStockItems = wishlistItems.filter(p => Number(p.stock || 0) > 0);
      if (!inStockItems.length) {
        if (typeof showToast === 'function') {
          showToast('Thông báo', 'Không có sản phẩm nào còn hàng để chuyển vào giỏ.', 'warning');
        }
        return;
      }

      moveAllBtn.disabled = true;
      const originalText = moveAllBtn.innerHTML;
      moveAllBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang chuyển...';

      let successCount = 0;

      for (const item of inStockItems) {
        try {
          let variantId = item.defaultVariantId;
          if (!variantId && item.slug) {
            const prodRes = await fetch(`/api/products/${encodeURIComponent(item.slug)}`, {
              credentials: 'same-origin',
              headers: { 'Accept': 'application/json' }
            });
            const prodData = await prodRes.json();
            const variants = prodData?.data?.variants || [];
            const activeVariant = variants.find(v => (v.quantity == null || v.quantity > 0) && v.selling !== false);
            if (activeVariant) variantId = activeVariant.id;
          }

          if (variantId) {
            const addRes = await fetch('/cart/items', {
              method: 'POST',
              credentials: 'same-origin',
              headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
              body: JSON.stringify({ productVariantId: variantId, count: 1 })
            });

            if (addRes.ok) {
              await fetch(`/api/wishlist/${encodeURIComponent(item.productId)}`, {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
              });
              successCount++;
            }
          }
        } catch (_) {}
      }

      moveAllBtn.disabled = false;
      moveAllBtn.innerHTML = originalText;

      if (successCount > 0) {
        if (typeof refreshCartBadge === 'function') refreshCartBadge();
        await loadWishlist();
        if (typeof showToast === 'function') {
          showToast('Thành công', `Đã chuyển ${successCount} sản phẩm vào giỏ hàng!`, 'success');
        }
      } else {
        if (typeof showToast === 'function') {
          showToast('Thông báo', 'Không thể chuyển sản phẩm vào giỏ hàng.', 'warning');
        }
      }
    });
  }

  // Xóa toàn bộ danh sách yêu thích
  const clearBtn = document.getElementById('clearWishlistBtn');
  if (clearBtn) {
    clearBtn.addEventListener('click', async () => {
      if (!wishlistItems.length) return;
      if (!confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm khỏi danh sách yêu thích?')) return;

      clearBtn.disabled = true;

      try {
        const res = await fetch('/api/wishlist', {
          method: 'DELETE',
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json' }
        });

        if (res.status === 401) {
          window.location.href = '/login?error=unauthorized';
          return;
        }

        const body = await res.json();
        if (!res.ok || !body.success) {
          throw new Error(body?.message || 'Không thể xóa danh sách yêu thích.');
        }

        wishlistItems = [];
        if (typeof Wishlist !== 'undefined') {
          Wishlist._ids.clear();
        }

        renderWishlist();

        if (typeof showToast === 'function') {
          showToast('Đã dọn sạch', 'Danh sách yêu thích đã được xóa toàn bộ.', 'info');
        }
      } catch (err) {
        if (typeof showToast === 'function') {
          showToast('Lỗi', err.message || 'Không thể xóa danh sách yêu thích.', 'danger');
        }
      } finally {
        clearBtn.disabled = false;
      }
    });
  }
}

/* ==========================================================================
   4. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  loadWishlist();
  bindWishlistEvents();

  // Lắng nghe khi wishlist thay đổi ở tab hoặc modal khác
  document.addEventListener('wishlist:changed', () => {
    loadWishlist();
  });
});
