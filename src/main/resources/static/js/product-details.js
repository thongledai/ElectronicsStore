/* ==========================================================================
   TechNova Electronics — Product Details Page
   --------------------------------------------------------------------------
   Reads ?id= from the URL, renders the image gallery, buy box, tab content
   (description / specifications / reviews), related products and the
   recently-viewed strip.
   ========================================================================== */

'use strict';

let currentProduct = null;

/* ==========================================================================
   1. BUY BOX / GALLERY
   ========================================================================== */
function renderProduct(product) {
  const container = document.getElementById('productDetail');
  const discount = calcDiscount(product.price, product.oldPrice);
  const outOfStock = product.stock === 0;
  const inWishlist = Wishlist.has(product.id);
  const maxQty = Math.min(product.stock, CONFIG.maxQtyPerItem);

  // Estimated delivery: two working days from today
  const delivery = new Date();
  delivery.setDate(delivery.getDate() + 2);
  const deliveryText = delivery.toLocaleDateString('en-IN', {
    weekday: 'long', day: 'numeric', month: 'short'
  });

  container.innerHTML = `
    <div class="row g-4 g-lg-5">

      <!-- ---------- Image gallery ---------- -->
      <div class="col-lg-6">
        <div class="gallery-main" id="galleryMain" title="Hover to zoom">
          ${product.badge ? `<span class="badge-ribbon">${escapeHtml(product.badge)}</span>` : ''}
          ${discount > 0 ? `<span class="badge-discount">-${discount}%</span>` : ''}
          <img src="${product.images[0]}" alt="${escapeHtml(product.name)}" id="mainImage" width="600" height="600">
        </div>

        <div class="gallery-thumbs" role="group" aria-label="Product image thumbnails">
          ${product.images.map((src, i) => `
            <button class="gallery-thumb ${i === 0 ? 'active' : ''}" type="button"
                    data-src="${src}" aria-label="View image ${i + 1}">
              <img src="${src}" alt="${escapeHtml(product.name)} view ${i + 1}" loading="lazy">
            </button>`).join('')}
        </div>
      </div>

      <!-- ---------- Buy box ---------- -->
      <div class="col-lg-6">
        <span class="badge-soft mb-2 d-inline-block">${getCategoryName(product.category)}</span>
        <h1 class="pd-title">${escapeHtml(product.name)}</h1>

        <div class="pd-meta">
          <span><i class="fas fa-tag me-1 text-muted"></i>Brand: <strong>${escapeHtml(product.brand)}</strong></span>
          <span class="d-flex align-items-center gap-2">
            <span class="stars">${renderStars(product.rating)}</span>
            <a href="#tab-reviews" class="small">${product.rating} · ${product.reviewCount.toLocaleString('en-IN')} ratings</a>
          </span>
        </div>

        <p class="text-muted">${escapeHtml(product.tagline)}</p>

        <div class="pd-price-row">
          <span class="pd-price">${formatPrice(product.price)}</span>
          ${product.oldPrice ? `<span class="price-old fs-5">${formatPrice(product.oldPrice)}</span>` : ''}
          ${discount > 0 ? `<span class="badge bg-success">${discount}% OFF</span>` : ''}
        </div>
        <p class="pd-tax-note">
          Inclusive of all taxes ·
          ${product.oldPrice ? `You save <strong class="text-success">${formatPrice(product.oldPrice - product.price)}</strong>` : 'Best price guaranteed'}
        </p>

        <!-- Stock status -->
        <p class="mb-3">
          ${outOfStock
            ? '<span class="stock-note mb-0"><i class="fas fa-circle-xmark me-1"></i>Out of stock</span>'
            : product.stock <= 10
              ? `<span class="stock-note mb-0"><i class="fas fa-triangle-exclamation me-1"></i>Hurry — only ${product.stock} left!</span>`
              : '<span class="stock-note in mb-0"><i class="fas fa-circle-check me-1"></i>In stock &amp; ready to ship</span>'}
        </p>

        <!-- Key highlights -->
        <ul class="pd-highlights">
          ${product.highlights.map(h => `<li>${escapeHtml(h)}</li>`).join('')}
        </ul>

        <!-- Quantity + actions -->
        <div class="d-flex flex-wrap align-items-center gap-3 mt-4">
          <div class="qty-stepper" role="group" aria-label="Quantity">
            <button type="button" id="qtyMinus" aria-label="Decrease quantity" ${outOfStock ? 'disabled' : ''}>
              <i class="fas fa-minus"></i>
            </button>
            <input type="number" id="qtyInput" value="1" min="1" max="${maxQty || 1}"
                   aria-label="Quantity" ${outOfStock ? 'disabled' : ''}>
            <button type="button" id="qtyPlus" aria-label="Increase quantity" ${outOfStock ? 'disabled' : ''}>
              <i class="fas fa-plus"></i>
            </button>
          </div>
          ${!outOfStock ? `<span class="small text-muted">Max ${maxQty} per order</span>` : ''}
        </div>

        <div class="d-flex flex-wrap gap-2 mt-3">
          <button class="btn btn-brand btn-lg js-add-cart flex-grow-1" data-id="${product.id}"
                  data-use-qty="true" type="button" ${outOfStock ? 'disabled' : ''}>
            <i class="fas fa-cart-plus me-2"></i>${outOfStock ? 'Notify Me' : 'Add to Cart'}
          </button>

          <button class="btn btn-accent btn-lg" id="buyNowBtn" type="button" ${outOfStock ? 'disabled' : ''}>
            <i class="fas fa-bolt me-2"></i>Buy Now
          </button>

          <button class="btn btn-outline-brand btn-lg js-wishlist ${inWishlist ? 'is-active' : ''}"
                  data-id="${product.id}" type="button"
                  aria-label="${inWishlist ? 'Remove from' : 'Add to'} wishlist">
            <i class="${inWishlist ? 'fas' : 'far'} fa-heart"></i>
            <span class="d-none d-sm-inline ms-2">Wishlist</span>
          </button>
        </div>

        <!-- Delivery estimate -->
        <div class="mt-3 p-3 rounded" style="background:var(--surface-2)">
          <div class="d-flex align-items-center gap-2 small">
            <i class="fas fa-location-dot text-primary"></i>
            <span>Deliver to <strong>Patiala 147004</strong> —
              ${outOfStock ? 'currently unavailable' : `arrives by <strong class="text-success">${deliveryText}</strong>`}
            </span>
          </div>
        </div>

        <!-- Assurances -->
        <div class="assurance-row">
          <span class="assurance"><i class="fas fa-shield-halved"></i>Genuine warranty</span>
          <span class="assurance"><i class="fas fa-rotate-left"></i>7-day returns</span>
          <span class="assurance"><i class="fas fa-truck-fast"></i>Free delivery above ₹5,000</span>
          <span class="assurance"><i class="fas fa-lock"></i>Secure checkout</span>
        </div>

      </div>
    </div>`;

  bindGallery();
  bindQuantity(maxQty);
  bindBuyNow(product);
}

/** Thumbnail clicks swap the main gallery image. */
function bindGallery() {
  const main = document.getElementById('mainImage');
  document.querySelectorAll('.gallery-thumb').forEach(thumb => {
    thumb.addEventListener('click', () => {
      document.querySelectorAll('.gallery-thumb').forEach(t => t.classList.remove('active'));
      thumb.classList.add('active');

      // Quick cross-fade between images
      main.style.opacity = '0';
      setTimeout(() => {
        main.src = thumb.dataset.src;
        main.style.opacity = '1';
      }, 140);
    });
  });
  main.style.transition = 'opacity .18s ease, transform .45s ease';

  // Move the zoom origin with the cursor for a proper magnifier feel
  const frame = document.getElementById('galleryMain');
  frame.addEventListener('mousemove', e => {
    const rect = frame.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    main.style.transformOrigin = `${x}% ${y}%`;
  });
  frame.addEventListener('mouseleave', () => { main.style.transformOrigin = 'center center'; });
}

/** Wires the +/- quantity stepper and clamps typed values. */
function bindQuantity(maxQty) {
  const input = document.getElementById('qtyInput');
  const minus = document.getElementById('qtyMinus');
  const plus = document.getElementById('qtyPlus');
  if (!input) return;

  const sync = () => {
    let value = parseInt(input.value, 10);
    if (isNaN(value) || value < 1) value = 1;
    if (value > maxQty) {
      value = maxQty;
      showToast(UiMessage.VARIANT_MAXIMUM_TITLE, UiMessage.VARIANT_MAXIMUM(maxQty), 'warning', 2400);
    }
    input.value = value;
    minus.disabled = value <= 1;
    plus.disabled = value >= maxQty;
  };

  minus.addEventListener('click', () => { input.value = Number(input.value) - 1; sync(); });
  plus.addEventListener('click', () => { input.value = Number(input.value) + 1; sync(); });
  input.addEventListener('change', sync);
  sync();
}

/** "Buy Now" adds to the cart and jumps straight to the cart page. */
function bindBuyNow(product) {
  const btn = document.getElementById('buyNowBtn');
  if (!btn) return;

  btn.addEventListener('click', () => {
    const qty = Number(document.getElementById('qtyInput').value) || 1;
    if (Cart.add(product.id, qty, true)) {
      showToast(UiMessage.CHECKOUT_REDIRECT_TITLE, UiMessage.CHECKOUT_REDIRECT(product.name, qty), 'success', 1500);
      setTimeout(() => { location.href = ROUTES.cart; }, 700);
    }
  });
}

/* ==========================================================================
   2. TAB CONTENT
   ========================================================================== */
function renderTabs(product) {
  document.getElementById('detailTabsSection').hidden = false;
  document.getElementById('reviewTabCount').textContent = `(${product.reviews.length})`;

  /* --- Description --- */
  document.getElementById('tab-desc').innerHTML = `
    <div class="row g-4">
      <div class="col-lg-8">
        <h3 class="h5 mb-3">About the ${escapeHtml(product.name)}</h3>
        <p class="text-muted">${escapeHtml(product.description)}</p>

        <h4 class="h6 mt-4 mb-3">What makes it stand out</h4>
        <ul class="pd-highlights">
          ${product.highlights.map(h => `<li>${escapeHtml(h)}</li>`).join('')}
        </ul>
      </div>

      <div class="col-lg-4">
        <div class="surface-card p-4">
          <h4 class="h6 mb-3">At a glance</h4>
          <div class="summary-row"><span>Brand</span><span>${escapeHtml(product.brand)}</span></div>
          <div class="summary-row"><span>Category</span><span>${getCategoryName(product.category)}</span></div>
          <div class="summary-row"><span>Rating</span><span>${product.rating} / 5</span></div>
          <div class="summary-row"><span>Availability</span>
            <span class="${product.stock ? 'text-success' : 'text-danger'}">
              ${product.stock ? `${product.stock} in stock` : 'Out of stock'}
            </span>
          </div>
          <div class="summary-row"><span>Warranty</span>
            <span>${escapeHtml(product.specs.Warranty || '1 Year')}</span>
          </div>
        </div>
      </div>
    </div>`;

  /* --- Specifications --- */
  document.getElementById('tab-specs').innerHTML = `
    <div class="row">
      <div class="col-lg-9">
        <h3 class="h5 mb-3">Full Specifications</h3>
        <div class="surface-card p-3 p-md-4">
          <table class="spec-table">
            <tbody>
              ${Object.entries(product.specs).map(([key, value]) => `
                <tr>
                  <td>${escapeHtml(key)}</td>
                  <td>${escapeHtml(value)}</td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
        <p class="small text-muted mt-3">
          <i class="fas fa-circle-info me-1"></i>
          Specifications are provided by the manufacturer and may change without notice.
        </p>
      </div>
    </div>`;

  /* --- Reviews --- */
  renderReviews(product);
}

/** Builds the rating summary, distribution bars and the review list. */
function renderReviews(product) {
  // Distribution across 5..1 stars, derived from the sample reviews
  const distribution = [5, 4, 3, 2, 1].map(star => {
    const count = product.reviews.filter(r => r.rating === star).length;
    const pct = product.reviews.length ? (count / product.reviews.length) * 100 : 0;
    return { star, count, pct };
  });

  document.getElementById('tab-reviews').innerHTML = `
    <div class="row g-4">

      <div class="col-lg-4">
        <div class="rating-summary">
          <div class="big-score">${product.rating}</div>
          <div class="stars mb-2" style="font-size:1.05rem">${renderStars(product.rating)}</div>
          <p class="small text-muted mb-3">Based on ${product.reviewCount.toLocaleString('en-IN')} ratings</p>

          ${distribution.map(d => `
            <div class="rating-bar-row">
              <span>${d.star}★</span>
              <span class="rating-bar"><span style="width:${d.pct}%"></span></span>
              <span>${d.count}</span>
            </div>`).join('')}
        </div>

        <button class="btn btn-outline-brand w-100 mt-3" id="writeReviewBtn" type="button">
          <i class="fas fa-pen me-2"></i>Write a Review
        </button>
      </div>

      <div class="col-lg-8">
        <h3 class="h5 mb-3">Customer Reviews (${product.reviews.length})</h3>

        ${product.reviews.length ? product.reviews.map(review => `
          <article class="review-item">
            <div class="review-head">
              <div class="avatar-initials" style="width:38px;height:38px;font-size:.8rem">
                ${escapeHtml(review.name.split(' ').map(n => n[0]).join('').slice(0, 2))}
              </div>
              <div>
                <div class="fw-semibold small">${escapeHtml(review.name)}
                  <span class="verified-tag ms-1"><i class="fas fa-circle-check"></i> Verified Purchase</span>
                </div>
                <div class="small text-muted">${escapeHtml(review.date)}</div>
              </div>
              <span class="stars ms-auto">${renderStars(review.rating)}</span>
            </div>
            <h6>${escapeHtml(review.title)}</h6>
            <p>${escapeHtml(review.text)}</p>
            <div class="mt-2 d-flex gap-3 small text-muted">
              <button class="btn-link-danger"><i class="far fa-thumbs-up"></i> Helpful</button>
              <button class="btn-link-danger"><i class="far fa-flag"></i> Report</button>
            </div>
          </article>`).join('') : `
          <p class="text-muted">No written reviews yet — be the first to share your experience.</p>`}
      </div>
    </div>`;

  // The review form is out of scope for a frontend-only demo
  const writeBtn = document.getElementById('writeReviewBtn');
  if (writeBtn) {
    writeBtn.addEventListener('click', () => {
      showToast(UiMessage.SIGN_IN_REQUIRED_TITLE, UiMessage.REVIEW_SIGN_IN_REQUIRED, 'info');
      setTimeout(() => { location.href = ROUTES.login; }, 1200);
    });
  }
}

/* ==========================================================================
   3. RELATED & RECENTLY VIEWED
   ========================================================================== */
function renderRelated(product) {
  const related = getRelatedProducts(product, 4);
  if (!related.length) return;

  document.getElementById('relatedSection').hidden = false;
  renderProductGrid(related, document.getElementById('relatedGrid'));
}

function renderRecent(product) {
  const recent = Recent.detailed(product.id).slice(0, 4);
  if (!recent.length) return;

  document.getElementById('recentSection').hidden = false;
  renderProductGrid(recent, document.getElementById('recentGrid'));
}

/* ==========================================================================
   4. NOT FOUND STATE
   ========================================================================== */
function renderNotFound() {
  document.getElementById('productDetail').innerHTML = `
    <div class="empty-state">
      <img src="${IMG_BASE}empty-cart.svg" alt="">
      <h3>Product not found</h3>
      <p>The product you're looking for doesn't exist or may have been removed from our catalogue.</p>
      <a href="${ROUTES.products}" class="btn btn-brand">
        <i class="fas fa-arrow-left me-2"></i>Back to All Products
      </a>
    </div>`;
  document.title = 'Product Not Found — TechNova Electronics';
}

/* ==========================================================================
   5. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  const id = getParam('id');
  const product = getProductById(id);

  if (!product) {
    renderNotFound();
    return;
  }

  currentProduct = product;

  // Page metadata
  document.title = `${product.name} — TechNova Electronics`;
  const metaDesc = document.querySelector('meta[name="description"]');
  if (metaDesc) metaDesc.setAttribute('content', product.tagline);

  // Breadcrumb
  const crumbCat = document.getElementById('crumbCategory');
  crumbCat.textContent = getCategoryName(product.category);
  crumbCat.href = `products.html?category=${product.category}`;
  document.getElementById('crumbProduct').textContent = product.name;

  renderProduct(product);
  renderTabs(product);
  renderRelated(product);
  renderRecent(product);

  // Record the visit AFTER rendering so it isn't shown in its own history strip
  Recent.add(product.id);

  observeReveals();
});
