/* ==========================================================================
   TechNova Electronics — Customer Product List Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  const filterForm = document.getElementById('filterForm');
  const sortSelect = document.getElementById('sortSelect');
  const formSort = document.getElementById('formSort');
  const formPage = document.getElementById('formPage');
  const priceRange = document.getElementById('priceRange');
  const priceValue = document.getElementById('priceValue');
  const gridViewBtn = document.getElementById('gridViewBtn');
  const listViewBtn = document.getElementById('listViewBtn');
  const productGrid = document.getElementById('productGrid');

  /* ---------- Format VNĐ ---------- */
  function formatVnd(val) {
    return Number(val).toLocaleString('vi-VN') + ' ₫';
  }

  /* ---------- Debounce helper ---------- */
  function debounce(fn, delay = 400) {
    let timer = null;
    return function (...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  }

  /* ---------- Price slider dynamic label & submit ---------- */
  if (priceRange && priceValue) {
    priceRange.addEventListener('input', () => {
      priceValue.textContent = formatVnd(priceRange.value);
    });

    priceRange.addEventListener('change', () => {
      if (formPage) formPage.value = '1';
      if (filterForm) filterForm.submit();
    });
  }

  /* ---------- Price Presets ---------- */
  document.querySelectorAll('.price-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      const max = btn.getAttribute('data-max');
      if (priceRange && max) {
        priceRange.value = max;
        if (priceValue) priceValue.textContent = formatVnd(max);
        if (formPage) formPage.value = '1';
        if (filterForm) filterForm.submit();
      }
    });
  });

  /* ---------- Checkbox / Radio Filter Changes ---------- */
  document.querySelectorAll('.js-filter-input').forEach(input => {
    input.addEventListener('change', () => {
      if (formPage) formPage.value = '1';
      if (filterForm) filterForm.submit();
    });
  });

  /* ---------- Search Input in Filter Panel ---------- */
  const filterSearch = document.getElementById('filterSearch');
  if (filterSearch) {
    filterSearch.addEventListener('keydown', e => {
      if (e.key === 'Enter') {
        e.preventDefault();
        if (formPage) formPage.value = '1';
        if (filterForm) filterForm.submit();
      }
    });
  }

  /* ---------- Sorting Selection ---------- */
  if (sortSelect && formSort && filterForm) {
    sortSelect.addEventListener('change', () => {
      formSort.value = sortSelect.value;
      if (formPage) formPage.value = '1';
      filterForm.submit();
    });
  }

  /* ---------- Pagination Links ---------- */
  document.querySelectorAll('.js-page-link').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      const targetPage = link.getAttribute('data-page');
      if (targetPage && formPage && filterForm) {
        formPage.value = targetPage;
        filterForm.submit();
      }
    });
  });

  /* ---------- Remove Single Filter Chip ---------- */
  document.querySelectorAll('.js-remove-filter').forEach(btn => {
    btn.addEventListener('click', () => {
      const field = btn.getAttribute('data-field');
      const val = btn.getAttribute('data-val');

      if (!filterForm) return;

      if (field === 'q') {
        const qInput = filterForm.querySelector('input[name="q"]');
        if (qInput) qInput.value = '';
      } else if (field === 'category' || field === 'brand') {
        const targetInput = filterForm.querySelector(`input[name="${field}"][value="${val}"]`);
        if (targetInput) targetInput.checked = false;
      } else if (field === 'inStock' || field === 'onSale') {
        const targetInput = filterForm.querySelector(`input[name="${field}"]`);
        if (targetInput) targetInput.checked = false;
      }

      if (formPage) formPage.value = '1';
      filterForm.submit();
    });
  });

  /* ---------- Grid / List View Toggle ---------- */
  if (gridViewBtn && listViewBtn && productGrid) {
    gridViewBtn.addEventListener('click', () => {
      gridViewBtn.classList.add('active');
      listViewBtn.classList.remove('active');
      productGrid.classList.remove('view-list');
    });

    listViewBtn.addEventListener('click', () => {
      listViewBtn.classList.add('active');
      gridViewBtn.classList.remove('active');
      productGrid.classList.add('view-list');
    });
  }

  /* ---------- Capture Phase Interception for Cart & Wishlist ---------- */
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
  }, true); // Use capture phase
});
