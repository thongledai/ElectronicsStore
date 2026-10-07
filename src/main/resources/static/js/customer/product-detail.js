/* ==========================================================================
   TechNova Electronics — Customer Product Detail Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  const mainImage = document.getElementById('mainImage');
  const thumbnailGallery = document.getElementById('thumbnailGallery');
  const currentPrice = document.getElementById('currentPrice');
  const currentOldPrice = document.getElementById('currentOldPrice');
  const currentDiscountBadge = document.getElementById('currentDiscountBadge');
  const currentSku = document.getElementById('currentSku');
  const currentStockStatus = document.getElementById('currentStockStatus');
  const specSku = document.getElementById('specSku');
  const qtyInput = document.getElementById('qtyInput');
  const btnQtyMinus = document.getElementById('btnQtyMinus');
  const btnQtyPlus = document.getElementById('btnQtyPlus');
  const btnAddToCart = document.getElementById('btnAddToCart');

  function formatVnd(val) {
    if (!val) return '0 ₫';
    return Number(val).toLocaleString('vi-VN') + ' ₫';
  }

  /* ---------- Quantity Stepper ---------- */
  if (btnQtyMinus && btnQtyPlus && qtyInput) {
    btnQtyMinus.addEventListener('click', () => {
      let val = parseInt(qtyInput.value, 10) || 1;
      if (val > 1) qtyInput.value = val - 1;
    });

    btnQtyPlus.addEventListener('click', () => {
      let val = parseInt(qtyInput.value, 10) || 1;
      let max = parseInt(qtyInput.max, 10) || 10;
      if (val < max) qtyInput.value = val + 1;
    });

    qtyInput.addEventListener('change', () => {
      let val = parseInt(qtyInput.value, 10) || 1;
      let max = parseInt(qtyInput.max, 10) || 10;
      if (val < 1) val = 1;
      if (val > max) val = max;
      qtyInput.value = val;
    });
  }

  /* ---------- Thumbnail Gallery click ---------- */
  function initThumbnails() {
    if (!thumbnailGallery) return;
    thumbnailGallery.querySelectorAll('.thumb-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        thumbnailGallery.querySelectorAll('.thumb-btn').forEach(b => b.classList.remove('border-primary'));
        btn.classList.add('border-primary');
        const img = btn.querySelector('img');
        if (img && mainImage) {
          mainImage.src = img.src;
        }
      });
    });
  }
  initThumbnails();

  /* ---------- Fetch Product Details JSON from API for dynamic switching ---------- */
  const pathParts = window.location.pathname.split('/');
  const slug = pathParts[pathParts.length - 1];

  if (slug) {
    fetch(`/api/products/${encodeURIComponent(slug)}`, {
      headers: { 'Accept': 'application/json' }
    })
      .then(res => res.json())
      .then(res => {
        if (res.success && res.data && res.data.variants) {
          setupVariantSwitcher(res.data.variants);
        }
      })
      .catch(err => console.warn('Không thể tải dữ liệu biến thể:', err));
  }

  function setupVariantSwitcher(variants) {
    const variantButtons = document.querySelectorAll('.js-variant-choice');
    variantButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const variantId = btn.getAttribute('data-variant-id');
        const variant = variants.find(v => v.id === variantId);
        if (!variant) return;

        variantButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // 1. Update Price
        const effPrice = (variant.promotionalPrice && variant.promotionalPrice > 0)
          ? variant.promotionalPrice
          : variant.price;

        if (currentPrice) currentPrice.textContent = formatVnd(effPrice);

        if (variant.promotionalPrice && variant.promotionalPrice < variant.price) {
          if (currentOldPrice) {
            currentOldPrice.textContent = formatVnd(variant.price);
            currentOldPrice.hidden = false;
          }
          if (currentDiscountBadge) {
            const pct = Math.round(((variant.price - variant.promotionalPrice) * 100) / variant.price);
            currentDiscountBadge.textContent = `-${pct}%`;
            currentDiscountBadge.hidden = false;
          }
        } else {
          if (currentOldPrice) currentOldPrice.hidden = true;
          if (currentDiscountBadge) currentDiscountBadge.hidden = true;
        }

        // 2. Update SKU & Specs
        if (currentSku) currentSku.textContent = variant.sku;
        if (specSku) specSku.textContent = variant.sku;

        // 3. Update Stock Status
        if (currentStockStatus) {
          if (variant.quantity > 0) {
            currentStockStatus.innerHTML = `<strong class="text-success">Còn hàng (${variant.quantity} sản phẩm)</strong>`;
            if (btnAddToCart) btnAddToCart.disabled = false;
            if (qtyInput) qtyInput.max = Math.min(variant.quantity, 10);
          } else {
            currentStockStatus.innerHTML = `<strong class="text-danger">Tạm hết hàng</strong>`;
            if (btnAddToCart) btnAddToCart.disabled = true;
          }
        }

        // 4. Update Images
        if (variant.imageUrls && variant.imageUrls.length > 0) {
          if (mainImage) mainImage.src = variant.imageUrls[0];
          if (thumbnailGallery) {
            thumbnailGallery.innerHTML = variant.imageUrls.map((url, i) => `
              <button type="button" class="thumb-btn p-1 border rounded ${i === 0 ? 'border-primary' : ''}"
                      style="width:70px; height:70px; background:white; overflow:hidden;">
                <img src="${url}" alt="Variant Image" style="width:100%; height:100%; object-fit:contain;">
              </button>
            `).join('');
            initThumbnails();
          }
        }
      });
    });
  }

  /* ---------- Intercept Cart & Wishlist actions in capture phase ---------- */
  document.body.addEventListener('click', e => {
    const addCartBtn = e.target.closest('.js-add-cart');
    if (addCartBtn) {
      e.stopPropagation();
      e.preventDefault();
      if (window.showToast) {
        window.showToast('Thông báo', 'Chức năng giỏ hàng đang được cập nhật!', 'info');
      } else if (window.toast) {
        window.toast('Chức năng giỏ hàng đang được cập nhật!');
      } else {
        alert('Chức năng giỏ hàng đang được cập nhật!');
      }
      return;
    }

    const wishBtn = e.target.closest('.js-wishlist');
    if (wishBtn) {
      e.stopPropagation();
      e.preventDefault();
      if (window.showToast) {
        window.showToast('Thông báo', 'Chức năng danh sách yêu thích đang được cập nhật!', 'info');
      } else if (window.toast) {
        window.toast('Chức năng danh sách yêu thích đang được cập nhật!');
      } else {
        alert('Chức năng danh sách yêu thích đang được cập nhật!');
      }
    }
  }, true);
});
