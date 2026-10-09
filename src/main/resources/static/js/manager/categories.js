/* ==========================================================================
   TechNova Electronics — Manager Categories Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;
  let categoryOptions = [];

  function getJwtToken() {
    let token = localStorage.getItem('technova_jwt');
    if (!token) {
      const match = document.cookie.match(new RegExp('(^| )JWT_TOKEN=([^;]+)'));
      if (match) token = match[2];
    }
    return token;
  }

  function authHeaders(isJson = false) {
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

  /* ---------- Load Parent Category Options ---------- */
  async function loadCategoryOptions() {
    try {
      const res = await fetch('/api/manager/categories/options', { headers: authHeaders(false) }).then(r => r.json());
      if (res.success) {
        categoryOptions = res.data || [];
        const parentSelect = document.getElementById('categoryParent');
        if (parentSelect) {
          parentSelect.innerHTML = '<option value="">-- Không có (Danh mục gốc) --</option>' +
            categoryOptions.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
      }
    } catch (e) {
      console.warn('Lỗi tải danh mục cha:', e);
    }
  }

  /* ---------- Load Categories Table ---------- */
  async function loadCategories(page = 0) {
    const search = document.getElementById('searchInput')?.value.trim() || '';
    const status = document.getElementById('statusFilter')?.value || '';

    let isActive = '';
    if (status === 'active') isActive = 'true';
    if (status === 'deleted') isActive = 'false';

    let url = `/api/manager/categories?page=${page}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (isActive) url += `&isActive=${isActive}`;

    const tbody = document.getElementById('categoryTableBody');
    if (tbody) tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;

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
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Không tìm thấy danh mục nào</td></tr>`;
        return;
      }

      tbody.innerHTML = data.content.map(c => {
        const statusBadge = c.isActive
          ? '<span class="status green">Hoạt động</span>'
          : '<span class="status red">Đã xóa mềm</span>';

        return `
          <tr>
            <td>
              <img src="${c.image || '/images/placeholder.webp'}" alt="" width="36" height="36" class="rounded border" style="object-fit:cover;" onerror="this.src='/images/placeholder.webp'">
            </td>
            <td><strong>${c.name}</strong></td>
            <td>${c.parentName || '<span class="text-muted">Danh mục gốc</span>'}</td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <button class="action js-edit-category" data-id="${c.id}" title="Chỉnh sửa"><i class="fa-solid fa-pen"></i></button>
              <button class="action text-danger js-delete-category" data-id="${c.id}" title="Xóa vĩnh viễn"><i class="fa-solid fa-trash"></i></button>
            </td>
          </tr>
        `;
      }).join('');

      attachEvents();
    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-danger">Lỗi: ${e.message}</td></tr>`;
    }
  }

  function attachEvents() {
    // Edit Category
    document.querySelectorAll('.js-edit-category').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/categories/${id}`, { headers: authHeaders(false) }).then(r => r.json());
          if (res.success && res.data) {
            const c = res.data;
            document.getElementById('categoryId').value = c.id;
            document.getElementById('categoryName').value = c.name;
            document.getElementById('categoryParent').value = c.parentId || '';
            document.getElementById('categoryIsActive').checked = c.isActive;
            document.getElementById('categoryImageFile').value = '';

            const preview = document.getElementById('currentCategoryImgPreview');
            const previewImg = document.getElementById('categoryPreviewImg');
            if (c.image) {
              previewImg.src = c.image;
              preview.style.display = 'block';
            } else {
              preview.style.display = 'none';
            }

            document.getElementById('categoryModalTitle').textContent = 'Chỉnh sửa danh mục';
            const modal = new bootstrap.Modal(document.getElementById('categoryFormModal'));
            modal.show();
          }
        } catch (e) {
          notify('Lỗi tải danh mục: ' + e.message);
        }
      });
    });

    // Permanently delete Category
    document.querySelectorAll('.js-delete-category').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc chắn muốn xóa vĩnh viễn danh mục này khỏi cơ sở dữ liệu?')) return;
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/categories/${id}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã xóa vĩnh viễn danh mục');
            loadCategories(currentPage);
            loadCategoryOptions();
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

  }

  /* ---------- Form Submit (Create / Edit via multipart) ---------- */
  const categoryForm = document.getElementById('categoryForm');
  if (categoryForm) {
    categoryForm.addEventListener('submit', async e => {
      e.preventDefault();
      const id = document.getElementById('categoryId').value;
      const formData = new FormData();
      formData.append('name', document.getElementById('categoryName').value.trim());

      const parentId = document.getElementById('categoryParent').value;
      if (parentId) formData.append('parentId', parentId);

      const fileInput = document.getElementById('categoryImageFile');
      if (fileInput.files && fileInput.files[0]) {
        formData.append('imageFile', fileInput.files[0]);
      }
      formData.append('isActive', document.getElementById('categoryIsActive').checked);

      try {
        const url = id ? `/api/manager/categories/${id}` : '/api/manager/categories';
        const method = id ? 'PUT' : 'POST';

        const token = getJwtToken();
        const headers = { 'Accept': 'application/json' };
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const res = await fetch(url, {
          method: method,
          headers: headers,
          body: formData
        }).then(r => r.json());

        if (res.success) {
          notify(id ? 'Cập nhật danh mục thành công' : 'Tạo mới danh mục thành công');
          bootstrap.Modal.getInstance(document.getElementById('categoryFormModal'))?.hide();
          loadCategories(currentPage);
          loadCategoryOptions();
        } else {
          notify('Lỗi: ' + res.message);
        }
      } catch (err) {
        notify('Lỗi: ' + err.message);
      }
    });
  }

  document.getElementById('btnOpenCreateCategoryModal')?.addEventListener('click', () => {
    document.getElementById('categoryId').value = '';
    document.getElementById('categoryForm').reset();
    document.getElementById('categoryIsActive').checked = true;
    document.getElementById('currentCategoryImgPreview').style.display = 'none';
    document.getElementById('categoryModalTitle').textContent = 'Thêm mới danh mục';
    const modal = new bootstrap.Modal(document.getElementById('categoryFormModal'));
    modal.show();
  });

  /* ---------- Search & Filter Events ---------- */
  document.getElementById('btnPrevPage')?.addEventListener('click', () => {
    if (currentPage > 0) loadCategories(currentPage - 1);
  });
  document.getElementById('btnNextPage')?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) loadCategories(currentPage + 1);
  });
  document.getElementById('btnSearch')?.addEventListener('click', () => loadCategories(0));
  document.getElementById('searchInput')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') loadCategories(0);
  });
  document.getElementById('statusFilter')?.addEventListener('change', () => loadCategories(0));

  // Initial load
  loadCategoryOptions();
  loadCategories(0);
});
