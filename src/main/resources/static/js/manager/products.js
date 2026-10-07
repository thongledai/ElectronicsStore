/* ==========================================================================
   TechNova Electronics — Manager Products Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;
  let currentProductId = null;
  let activeVariantId = null;

  // Cache options
  let categoryOptions = [];
  let brandOptions = [];
  let styleValueOptions = [];

  function getJwtToken() {
    let token = localStorage.getItem('technova_jwt');
    if (!token) {
      const match = document.cookie.match(new RegExp('(^| )JWT_TOKEN=([^;]+)'));
      if (match) token = match[2];
    }
    return token;
  }

  function authHeaders(isJson = true) {
    const headers = { 'Accept': 'application/json' };
    const token = getJwtToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (isJson) headers['Content-Type'] = 'application/json';
    return headers;
  }

  function notify(msg) {
    if (window.toast) window.toast(msg);
    else if (window.showToast) window.showToast('Thông báo', msg, 'info');
    else alert(msg);
  }

  function formatVnd(val) {
    if (val === null || val === undefined) return '0 ₫';
    return Number(val).toLocaleString('vi-VN') + ' ₫';
  }

  /* ---------- Load Dropdown Options ---------- */
  async function loadOptions() {
    try {
      const [catRes, brandRes, styleValRes] = await Promise.all([
        fetch('/api/manager/categories/options', { headers: authHeaders(false) }).then(r => r.json()),
        fetch('/api/manager/brands/options', { headers: authHeaders(false) }).then(r => r.json()),
        fetch('/api/manager/style-values/options', { headers: authHeaders(false) }).then(r => r.json())
      ]);

      if (catRes.success) {
        categoryOptions = catRes.data || [];
        const filterSelect = document.getElementById('categoryFilter');
        const formSelect = document.getElementById('productCategory');
        if (filterSelect) {
          filterSelect.innerHTML = '<option value="">-- Tất cả danh mục --</option>' +
            categoryOptions.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
        if (formSelect) {
          formSelect.innerHTML = '<option value="">-- Chọn danh mục --</option>' +
            categoryOptions.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
      }

      if (brandRes.success) {
        brandOptions = brandRes.data || [];
        const brandSelect = document.getElementById('productBrand');
        if (brandSelect) {
          brandSelect.innerHTML = '<option value="">-- Chọn thương hiệu --</option>' +
            brandOptions.map(b => `<option value="${b.id}">${b.name}</option>`).join('');
        }
      }

      if (styleValRes.success) {
        styleValueOptions = styleValRes.data || [];
      }
    } catch (e) {
      console.warn('Lỗi khi nạp danh sách lựa chọn:', e);
    }
  }

  /* ---------- Load Products Table ---------- */
  async function loadProducts(page = 0) {
    const search = document.getElementById('searchInput')?.value.trim() || '';
    const categoryId = document.getElementById('categoryFilter')?.value || '';
    const status = document.getElementById('statusFilter')?.value || '';

    let isActive = '';
    if (status === 'active') isActive = 'true';
    if (status === 'inactive') isActive = 'false';

    let url = `/api/manager/products?page=${page}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (categoryId) url += `&categoryId=${encodeURIComponent(categoryId)}`;
    if (isActive) url += `&isActive=${isActive}`;

    const tbody = document.getElementById('productTableBody');
    if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;

    try {
      const res = await fetch(url, { headers: authHeaders(false) }).then(r => r.json());
      if (!res.success) throw new Error(res.message);

      const data = res.data;
      currentPage = data.page;
      totalPages = data.totalPages || 1;

      document.getElementById('totalRecordsText').textContent = `${data.totalElements} bản ghi`;
      document.getElementById('pageInfoText').textContent = `Trang ${currentPage + 1}/${totalPages}`;
      document.getElementById('btnCurrentPage').textContent = `${currentPage + 1}`;
      document.getElementById('btnPrevPage').disabled = currentPage <= 0;
      document.getElementById('btnNextPage').disabled = currentPage >= totalPages - 1;

      if (!data.content || data.content.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Không tìm thấy sản phẩm nào</td></tr>`;
        return;
      }

      tbody.innerHTML = data.content.map(p => {
        let statusBadge = '<span class="status green">Đang bán</span>';
        if (!p.isActive) {
          statusBadge = '<span class="status red">Ngừng hoạt động</span>';
        } else if (!p.isSelling) {
          statusBadge = '<span class="status yellow">Tạm ngưng bán</span>';
        } else if (p.totalStock === 0) {
          statusBadge = '<span class="status red">Hết hàng</span>';
        } else if (p.totalStock <= 5) {
          statusBadge = '<span class="status yellow">Sắp hết</span>';
        }

        return `
          <tr>
            <td>
              <div class="d-flex align-items-center gap-2">
                <img src="${p.thumbnailUrl || '/images/placeholder.webp'}" alt="" width="36" height="36" class="rounded border" style="object-fit:cover;" onerror="this.src='/images/placeholder.webp'">
                <div>
                  <div class="fw-semibold">${p.name}</div>
                  <small class="text-muted">${p.slug}</small>
                </div>
              </div>
            </td>
            <td>${p.categoryName || '-'}</td>
            <td>${p.brandName || '-'}</td>
            <td>${formatVnd(p.effectivePrice || p.minPrice)}</td>
            <td>${p.totalStock !== null ? p.totalStock : 0}</td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <button class="action js-manage-variants" data-id="${p.id}" data-name="${p.name}" title="Quản lý biến thể & Ảnh"><i class="fa-solid fa-layer-group"></i></button>
              <button class="action js-edit-product" data-id="${p.id}" title="Chỉnh sửa"><i class="fa-solid fa-pen"></i></button>
              ${p.isActive
                ? `<button class="action text-danger js-delete-product" data-id="${p.id}" title="Vô hiệu hóa"><i class="fa-solid fa-trash"></i></button>`
                : `<button class="action text-success js-restore-product" data-id="${p.id}" title="Khôi phục"><i class="fa-solid fa-rotate-left"></i></button>`
              }
            </td>
          </tr>
        `;
      }).join('');

      attachProductEvents();
    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Lỗi: ${e.message}</td></tr>`;
    }
  }

  function attachProductEvents() {
    // Edit Product
    document.querySelectorAll('.js-edit-product').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/products/${id}`, { headers: authHeaders(false) }).then(r => r.json());
          if (res.success && res.data) {
            const p = res.data;
            document.getElementById('productId').value = p.id;
            document.getElementById('productName').value = p.name;
            document.getElementById('productSlug').value = p.slug;
            document.getElementById('productCategory').value = p.category ? p.category.id : '';
            document.getElementById('productBrand').value = p.brand ? p.brand.id : '';
            document.getElementById('productDescription').value = p.description;
            document.getElementById('productIsActive').checked = p.isActive;
            document.getElementById('productIsSelling').checked = p.isSelling;
            document.getElementById('productModalTitle').textContent = 'Chỉnh sửa sản phẩm';

            const modal = new bootstrap.Modal(document.getElementById('productFormModal'));
            modal.show();
          }
        } catch (e) {
          notify('Lỗi tải thông tin sản phẩm: ' + e.message);
        }
      });
    });

    // Delete Product
    document.querySelectorAll('.js-delete-product').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc chắn muốn vô hiệu hóa sản phẩm này?')) return;
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/products/${id}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã vô hiệu hóa sản phẩm');
            loadProducts(currentPage);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Restore Product
    document.querySelectorAll('.js-restore-product').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/products/${id}/restore`, {
            method: 'POST',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã khôi phục sản phẩm');
            loadProducts(currentPage);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Manage Variants
    document.querySelectorAll('.js-manage-variants').forEach(btn => {
      btn.addEventListener('click', async () => {
        currentProductId = btn.dataset.id;
        document.getElementById('variantProductName').textContent = btn.dataset.name;
        try {
          const styleValRes = await fetch('/api/manager/style-values/options', { headers: authHeaders(false) }).then(r => r.json());
          if (styleValRes.success && styleValRes.data) {
            styleValueOptions = styleValRes.data;
          }
        } catch (e) {
          console.warn('Lỗi tải thuộc tính:', e);
        }
        loadVariants(currentProductId);
        const modal = new bootstrap.Modal(document.getElementById('variantManageModal'));
        modal.show();
      });
    });
  }

  /* ---------- Product Form Submit (Create / Edit) ---------- */
  const productForm = document.getElementById('productForm');
  if (productForm) {
    productForm.addEventListener('submit', async e => {
      e.preventDefault();
      const id = document.getElementById('productId').value;
      const payload = {
        name: document.getElementById('productName').value.trim(),
        slug: document.getElementById('productSlug').value.trim() || null,
        categoryId: document.getElementById('productCategory').value,
        brandId: document.getElementById('productBrand').value,
        description: document.getElementById('productDescription').value.trim(),
        isActive: document.getElementById('productIsActive').checked,
        isSelling: document.getElementById('productIsSelling').checked
      };

      try {
        const url = id ? `/api/manager/products/${id}` : '/api/manager/products';
        const method = id ? 'PUT' : 'POST';

        const res = await fetch(url, {
          method: method,
          headers: authHeaders(true),
          body: JSON.stringify(payload)
        }).then(r => r.json());

        if (res.success) {
          notify(id ? 'Cập nhật sản phẩm thành công' : 'Tạo mới sản phẩm thành công');
          bootstrap.Modal.getInstance(document.getElementById('productFormModal'))?.hide();
          loadProducts(currentPage);
        } else {
          notify('Lỗi: ' + res.message);
        }
      } catch (err) {
        notify('Lỗi: ' + err.message);
      }
    });
  }

  document.getElementById('btnOpenCreateModal')?.addEventListener('click', () => {
    document.getElementById('productId').value = '';
    document.getElementById('productForm').reset();
    document.getElementById('productIsActive').checked = true;
    document.getElementById('productIsSelling').checked = true;
    document.getElementById('productModalTitle').textContent = 'Thêm mới sản phẩm';
    const modal = new bootstrap.Modal(document.getElementById('productFormModal'));
    modal.show();
  });

  /* ---------- Variant Management Logic ---------- */
  /* ---------- Variant Management Logic ---------- */
  async function loadVariants(productId) {
    document.getElementById('variantFormContainer').style.display = 'none';
    document.getElementById('variantImagesSection').style.display = 'none';
    const tbody = document.getElementById('variantTableBody');
    tbody.innerHTML = `<tr><td colspan="9" class="text-center py-3 text-muted">Đang tải biến thể...</td></tr>`;

    try {
      const res = await fetch(`/api/manager/products/${productId}/variants`, {
        headers: authHeaders(false)
      }).then(r => r.json());

      if (!res.success || !res.data || res.data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-3 text-muted">Chưa có biến thể nào</td></tr>`;
        return;
      }

      tbody.innerHTML = res.data.map(v => {
        const stylesText = (v.styleValues && v.styleValues.length > 0)
          ? v.styleValues.map(sv => `${sv.styleName}: ${sv.name}`).join(' | ')
          : '-';
        const imgCount = v.imageUrls ? v.imageUrls.length : 0;
        const statusBadge = v.isActive
          ? '<span class="status green">Hoạt động</span>'
          : '<span class="status red">Đã tắt</span>';

        return `
          <tr>
            <td><strong>${v.sku}</strong></td>
            <td>${stylesText}</td>
            <td>${formatVnd(v.price)}</td>
            <td>${v.promotionalPrice ? formatVnd(v.promotionalPrice) : '-'}</td>
            <td>${v.quantity}</td>
            <td>${v.sold || 0}</td>
            <td>
              <button class="btn btn-xs btn-outline-info js-manage-images" data-variant-id="${v.id}" data-sku="${v.sku}">
                <i class="fa-regular fa-image me-1"></i>${imgCount} ảnh
              </button>
            </td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <button class="action js-edit-variant" data-id="${v.id}" title="Sửa biến thể"><i class="fa-solid fa-pen"></i></button>
              ${v.isActive
                ? `<button class="action text-danger js-delete-variant" data-id="${v.id}" title="Vô hiệu hóa"><i class="fa-solid fa-trash"></i></button>`
                : `<button class="action text-success js-restore-variant" data-id="${v.id}" title="Khôi phục"><i class="fa-solid fa-rotate-left"></i></button>`
              }
            </td>
          </tr>
        `;
      }).join('');

      attachVariantRowEvents(productId, res.data);
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="9" class="text-center py-3 text-danger">Lỗi: ${e.message}</td></tr>`;
    }
  }

  function renderStyleValueCheckboxes(selectedIds = []) {
    const container = document.getElementById('variantStyleValuesContainer');
    if (!container) return;
    if (!styleValueOptions || styleValueOptions.length === 0) {
      container.innerHTML = '<span class="text-muted small">Chưa có giá trị thuộc tính nào</span>';
      return;
    }
    container.innerHTML = styleValueOptions.map(sv => `
      <div class="form-check form-check-inline border rounded p-1 px-2 bg-white">
        <input class="form-check-input js-style-val-check" type="checkbox" id="sv_${sv.id}" value="${sv.id}" ${selectedIds.includes(sv.id) ? 'checked' : ''}>
        <label class="form-check-label small" for="sv_${sv.id}"><strong>${sv.styleName}:</strong> ${sv.name}</label>
      </div>
    `).join('');
  }

  function attachVariantRowEvents(productId, variants) {
    // Edit Variant
    document.querySelectorAll('.js-edit-variant').forEach(btn => {
      btn.addEventListener('click', () => {
        const vId = btn.dataset.id;
        const v = variants.find(x => x.id === vId);
        if (!v) return;

        document.getElementById('variantId').value = v.id;
        document.getElementById('variantSku').value = v.sku;
        document.getElementById('variantPrice').value = v.price;
        document.getElementById('variantPromoPrice').value = v.promotionalPrice || '';
        document.getElementById('variantQuantity').value = v.quantity;

        const selIds = v.styleValues ? v.styleValues.map(sv => sv.id) : [];
        renderStyleValueCheckboxes(selIds);

        document.getElementById('variantFormTitle').textContent = 'Chỉnh sửa biến thể: ' + v.sku;
        document.getElementById('variantFormContainer').style.display = 'block';
      });
    });

    // Delete Variant
    document.querySelectorAll('.js-delete-variant').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc muốn vô hiệu hóa biến thể này?')) return;
        const vId = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/products/${productId}/variants/${vId}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã vô hiệu hóa biến thể');
            loadVariants(productId);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Restore Variant
    document.querySelectorAll('.js-restore-variant').forEach(btn => {
      btn.addEventListener('click', async () => {
        const vId = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/products/${productId}/variants/${vId}/restore`, {
            method: 'POST',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã khôi phục biến thể');
            loadVariants(productId);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Manage Images
    document.querySelectorAll('.js-manage-images').forEach(btn => {
      btn.addEventListener('click', () => {
        activeVariantId = btn.dataset.variantId;
        const sku = btn.dataset.sku;
        document.getElementById('imgVariantSku').textContent = sku;
        loadVariantImages(productId, activeVariantId);
        document.getElementById('variantImagesSection').style.display = 'block';
      });
    });
  }

  // Open Create Variant
  document.getElementById('btnOpenCreateVariant')?.addEventListener('click', () => {
    document.getElementById('variantId').value = '';
    document.getElementById('variantForm').reset();
    renderStyleValueCheckboxes([]);
    document.getElementById('variantFormTitle').textContent = 'Thêm mới biến thể';
    document.getElementById('variantFormContainer').style.display = 'block';
  });

  document.getElementById('btnCancelVariantForm')?.addEventListener('click', () => {
    document.getElementById('variantFormContainer').style.display = 'none';
  });

  // Variant Form Submit
  const variantForm = document.getElementById('variantForm');
  if (variantForm) {
    variantForm.addEventListener('submit', async e => {
      e.preventDefault();
      const vId = document.getElementById('variantId').value;
      const selectedStyleIds = Array.from(document.querySelectorAll('.js-style-val-check:checked'))
        .map(el => el.value);

      const sku = document.getElementById('variantSku').value.trim();
      const priceVal = parseFloat(document.getElementById('variantPrice').value);
      const promoRaw = document.getElementById('variantPromoPrice').value;
      const promoVal = promoRaw ? parseFloat(promoRaw) : null;
      const qtyVal = parseInt(document.getElementById('variantQuantity').value, 10);

      if (!sku) {
        notify('Vui lòng nhập mã SKU');
        return;
      }
      if (isNaN(priceVal) || priceVal < 0) {
        notify('Giá sản phẩm không hợp lệ');
        return;
      }
      if (promoVal !== null && !isNaN(promoVal) && promoVal >= priceVal) {
        notify('Giá khuyến mãi phải nhỏ hơn giá gốc');
        return;
      }

      const payload = {
        sku: sku,
        price: priceVal,
        promotionalPrice: (promoVal !== null && !isNaN(promoVal)) ? promoVal : null,
        quantity: isNaN(qtyVal) ? 0 : qtyVal,
        isActive: true,
        isSelling: true,
        styleValueIds: selectedStyleIds
      };

      try {
        const url = vId
          ? `/api/manager/products/${currentProductId}/variants/${vId}`
          : `/api/manager/products/${currentProductId}/variants`;
        const method = vId ? 'PUT' : 'POST';

        const res = await fetch(url, {
          method: method,
          headers: authHeaders(true),
          body: JSON.stringify(payload)
        }).then(r => r.json());

        if (res.success) {
          notify(vId ? 'Cập nhật biến thể thành công' : 'Tạo mới biến thể thành công');
          document.getElementById('variantFormContainer').style.display = 'none';
          document.getElementById('variantForm').reset();
          loadVariants(currentProductId);
          loadProducts(currentPage); // update stock & min price
        } else {
          notify('Lỗi: ' + res.message);
        }
      } catch (err) {
        notify('Lỗi: ' + err.message);
      }
    });
  }

  // Variant Images Load & Upload & Delete
  async function loadVariantImages(productId, variantId) {
    const gallery = document.getElementById('variantImagesGallery');
    gallery.innerHTML = '<div class="text-muted small">Đang tải ảnh...</div>';

    try {
      const res = await fetch(`/api/manager/products/${productId}/variants/${variantId}/images`, {
        headers: authHeaders(false)
      }).then(r => r.json());

      if (!res.success || !res.data || res.data.length === 0) {
        gallery.innerHTML = '<div class="text-muted small">Biến thể chưa có ảnh nào</div>';
        return;
      }

      gallery.innerHTML = res.data.map(url => `
        <div class="position-relative border rounded p-1 bg-white" style="width:100px; height:100px;">
          <img src="${url}" style="width:100%; height:100%; object-fit:contain;" alt="">
          <button type="button" class="btn btn-danger btn-xs position-absolute top-0 end-0 m-1 js-delete-img" data-url="${url}">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>
      `).join('');

      gallery.querySelectorAll('.js-delete-img').forEach(btn => {
        btn.addEventListener('click', async () => {
          if (!confirm('Bạn có chắc muốn xóa ảnh này khỏi hệ thống?')) return;
          const url = btn.dataset.url;
          try {
            const delRes = await fetch(`/api/manager/products/${productId}/variants/${variantId}/images?url=${encodeURIComponent(url)}`, {
              method: 'DELETE',
              headers: authHeaders(false)
            }).then(r => r.json());
            if (delRes.success) {
              notify('Đã xóa ảnh');
              loadVariantImages(productId, variantId);
              loadVariants(productId);
            } else {
              notify('Lỗi: ' + delRes.message);
            }
          } catch (e) {
            notify('Lỗi: ' + e.message);
          }
        });
      });
    } catch (e) {
      gallery.innerHTML = `<div class="text-danger small">Lỗi: ${e.message}</div>`;
    }
  }

  document.getElementById('btnUploadVariantImages')?.addEventListener('click', async () => {
    const filesInput = document.getElementById('variantImageFiles');
    if (!filesInput.files || filesInput.files.length === 0) {
      notify('Vui lòng chọn ít nhất một file ảnh');
      return;
    }

    const formData = new FormData();
    for (let i = 0; i < filesInput.files.length; i++) {
      formData.append('files', filesInput.files[i]);
    }

    try {
      const token = getJwtToken();
      const headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetch(`/api/manager/products/${currentProductId}/variants/${activeVariantId}/images`, {
        method: 'POST',
        headers: headers,
        body: formData
      }).then(r => r.json());

      if (res.success) {
        notify('Tải lên ảnh thành công');
        filesInput.value = '';
        loadVariantImages(currentProductId, activeVariantId);
        loadVariants(currentProductId);
        loadProducts(currentPage);
      } else {
        notify('Lỗi: ' + res.message);
      }
    } catch (e) {
      notify('Lỗi: ' + e.message);
    }
  });

  /* ---------- Pagination & Search Events ---------- */
  document.getElementById('btnPrevPage')?.addEventListener('click', () => {
    if (currentPage > 0) loadProducts(currentPage - 1);
  });
  document.getElementById('btnNextPage')?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) loadProducts(currentPage + 1);
  });
  document.getElementById('btnSearch')?.addEventListener('click', () => loadProducts(0));
  document.getElementById('searchInput')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') loadProducts(0);
  });
  document.getElementById('categoryFilter')?.addEventListener('change', () => loadProducts(0));
  document.getElementById('statusFilter')?.addEventListener('change', () => loadProducts(0));

  // Initial load
  loadOptions();
  loadProducts(0);
});
