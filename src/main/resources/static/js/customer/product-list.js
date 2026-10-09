/* ==========================================================================
   TechNova Electronics — Customer Product List Script (AJAX version)
   ========================================================================== */
'use strict';

let allCategories = [];
let currentCategory = null;

async function fetchCategories() {
  try {
    const res = await fetch('/api/categories', { headers: { 'Accept': 'application/json' } }).then(r => r.json());
    if (res && res.success && res.data) {
      allCategories = res.data;
    }
  } catch (e) {
    console.warn('Error fetching categories:', e);
  }
}

async function fetchProducts(categorySlug) {
  try {
    let url = '/api/products?size=100'; // Default to a larger size to show all without pagination for now
    if (categorySlug) {
      url += `&category=${encodeURIComponent(categorySlug)}`;
    }
    const res = await fetch(url, { headers: { 'Accept': 'application/json' } }).then(r => r.json());
    if (res && res.success && res.data && Array.isArray(res.data.content)) {
      return res.data;
    }
    return { content: [], totalElements: 0 };
  } catch (e) {
    console.warn('Error fetching products:', e);
    return { content: [], totalElements: 0 };
  }
}

function renderCategoryList() {
  const container = document.getElementById('categoryList');
  if (!container) return;

  if (allCategories.length === 0) {
    container.innerHTML = '<div class="text-muted small">Không có danh mục nào.</div>';
    return;
  }

  let html = '';
  allCategories.forEach(cat => {
    const isActive = currentCategory === cat.slug ? 'fw-bold text-primary' : 'text-body';
    html += `
      <div class="form-check cursor-pointer">
        <input class="form-check-input js-cat-radio" type="radio" name="categoryFilter" 
               id="cat_${cat.slug}" value="${cat.slug}" ${currentCategory === cat.slug ? 'checked' : ''}>
        <label class="form-check-label ${isActive} w-100" for="cat_${cat.slug}" style="cursor:pointer">
          ${typeof escapeHtml === 'function' ? escapeHtml(cat.name) : cat.name}
        </label>
      </div>`;
  });
  
  container.innerHTML = html;

  // Event listeners for radio buttons
  container.querySelectorAll('.js-cat-radio').forEach(radio => {
    radio.addEventListener('change', (e) => {
      if (e.target.checked) {
        currentCategory = e.target.value;
        renderCategoryList(); // Re-render to update bold text
        loadAndRenderProducts();
      }
    });
  });
}

async function loadAndRenderProducts() {
  const grid = document.getElementById('productGrid');
  const countLabel = document.getElementById('resultCount');
  
  if (grid) {
    grid.innerHTML = '<div class="col-12 text-center py-5"><i class="fas fa-spinner fa-spin fa-2x text-muted"></i></div>';
  }

  const data = await fetchProducts(currentCategory);
  const products = data.content;

  if (countLabel) {
    countLabel.innerHTML = `Tìm thấy <strong>${data.totalElements || products.length}</strong> sản phẩm`;
  }

  if (grid) {
    if (products.length === 0) {
      grid.innerHTML = `
        <div class="col-12 text-center py-5">
          <div class="p-5">
            <i class="fas fa-box-open fa-3x text-muted mb-3"></i>
            <h3 class="h5">Không tìm thấy sản phẩm phù hợp</h3>
          </div>
        </div>`;
    } else {
      // Use renderProductGrid from main.js if available
      if (typeof renderProductGrid === 'function') {
        renderProductGrid(products, grid);
      } else {
        grid.innerHTML = '<div class="col-12 text-danger">Lỗi: Hàm hiển thị sản phẩm chưa được tải.</div>';
      }
    }
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  // Read category from URL if present
  const params = new URLSearchParams(window.location.search);
  currentCategory = params.get('category');

  // Load categories and products concurrently
  await fetchCategories();
  renderCategoryList();
  await loadAndRenderProducts();

  // Clear button
  const clearBtn = document.querySelector('.js-clear-category');
  if (clearBtn) {
    clearBtn.addEventListener('click', (e) => {
      e.preventDefault();
      currentCategory = null;
      
      // Update URL without reloading
      const url = new URL(window.location);
      url.searchParams.delete('category');
      window.history.pushState({}, '', url);
      
      renderCategoryList();
      loadAndRenderProducts();
    });
  }

  // Intercept cart/wishlist clicks
  document.body.addEventListener('click', e => {
    const addCartBtn = e.target.closest('.js-add-cart');
    if (addCartBtn) {
      e.stopPropagation(); e.preventDefault();
      const msg = 'Chức năng giỏ hàng đang được cập nhật!';
      if (window.showToast) window.showToast('Thông báo', msg, 'info'); else alert(msg);
      return;
    }

    const wishBtn = e.target.closest('.js-wishlist');
    if (wishBtn) {
      e.stopPropagation(); e.preventDefault();
      const msg = 'Chức năng danh sách yêu thích đang được cập nhật!';
      if (window.showToast) window.showToast('Thông báo', msg, 'info'); else alert(msg);
    }
  }, true);
});
