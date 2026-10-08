/* ==========================================================================
   TechNova Electronics — Manager Brands Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;

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

  /* ---------- Load Brands Table ---------- */
  async function loadBrands(page = 0) {
    const search = document.getElementById('searchInput')?.value.trim() || '';
    const status = document.getElementById('statusFilter')?.value || '';

    let isActive = '';
    if (status === 'active') isActive = 'true';
    if (status === 'inactive') isActive = 'false';

    let url = `/api/manager/brands?page=${page}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (isActive) url += `&isActive=${isActive}`;

    const tbody = document.getElementById('brandTableBody');
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
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Không tìm thấy thương hiệu nào</td></tr>`;
        return;
      }

      tbody.innerHTML = data.content.map(b => {
        const statusBadge = b.isActive
          ? '<span class="status green">Hoạt động</span>'
          : '<span class="status red">Ngừng hoạt động</span>';

        return `
          <tr>
            <td>
              <img src="${b.logoUrl || '/images/placeholder.webp'}" alt="" width="36" height="36" class="rounded border" style="object-fit:contain; background:#f8fafc;" onerror="this.src='/images/placeholder.webp'">
            </td>
            <td><strong>${b.name}</strong></td>
            <td>${b.description || '<span class="text-muted">-</span>'}</td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <button class="action js-edit-brand" data-id="${b.id}" title="Chỉnh sửa"><i class="fa-solid fa-pen"></i></button>
              ${b.isActive
            ? `<button class="action text-danger js-delete-brand" data-id="${b.id}" title="Ngừng hoạt động"><i class="fa-solid fa-ban"></i></button>`
            : `<button class="action text-success js-restore-brand" data-id="${b.id}" title="Kích hoạt lại"><i class="fa-solid fa-rotate-left"></i></button>`
          }
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
    // Edit Brand
    document.querySelectorAll('.js-edit-brand').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/brands/${id}`, { headers: authHeaders(false) }).then(r => r.json());
          if (res.success && res.data) {
            const b = res.data;
            document.getElementById('brandId').value = b.id;
            document.getElementById('brandName').value = b.name;
            document.getElementById('brandDescription').value = b.description || '';
            document.getElementById('brandIsActive').checked = b.isActive;
            document.getElementById('brandLogoFile').value = '';

            const preview = document.getElementById('currentBrandLogoPreview');
            const previewImg = document.getElementById('brandPreviewImg');
            if (b.logoUrl) {
              previewImg.src = b.logoUrl;
              preview.style.display = 'block';
            } else {
              preview.style.display = 'none';
            }

            document.getElementById('brandModalTitle').textContent = 'Chỉnh sửa thương hiệu';
            const modal = new bootstrap.Modal(document.getElementById('brandFormModal'));
            modal.show();
          }
        } catch (e) {
          notify('Lỗi tải thông tin thương hiệu: ' + e.message);
        }
      });
    });

    // Delete Brand (Ngừng hoạt động)
    document.querySelectorAll('.js-delete-brand').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc chắn muốn ngừng hoạt động thương hiệu này?')) return;
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/brands/${id}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã ngừng hoạt động thương hiệu');
            loadBrands(currentPage);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Restore Brand (Kích hoạt lại)
    document.querySelectorAll('.js-restore-brand').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/brands/${id}/restore`, {
            method: 'POST',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã kích hoạt lại thương hiệu');
            loadBrands(currentPage);
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
  const brandForm = document.getElementById('brandForm');
  if (brandForm) {
    brandForm.addEventListener('submit', async e => {
      e.preventDefault();
      const id = document.getElementById('brandId').value;
      const formData = new FormData();
      formData.append('name', document.getElementById('brandName').value.trim());

      const desc = document.getElementById('brandDescription').value.trim();
      if (desc) formData.append('description', desc);

      formData.append('isActive', document.getElementById('brandIsActive').checked);

      const fileInput = document.getElementById('brandLogoFile');
      if (fileInput.files && fileInput.files[0]) {
        formData.append('logoFile', fileInput.files[0]);
      }

      try {
        const url = id ? `/api/manager/brands/${id}` : '/api/manager/brands';
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
          notify(id ? 'Cập nhật thương hiệu thành công' : 'Tạo mới thương hiệu thành công');
          bootstrap.Modal.getInstance(document.getElementById('brandFormModal'))?.hide();
          loadBrands(currentPage);
        } else {
          notify('Lỗi: ' + res.message);
        }
      } catch (err) {
        notify('Lỗi: ' + err.message);
      }
    });
  }

  document.getElementById('btnOpenCreateBrandModal')?.addEventListener('click', () => {
    document.getElementById('brandId').value = '';
    document.getElementById('brandForm').reset();
    document.getElementById('brandIsActive').checked = true;
    document.getElementById('currentBrandLogoPreview').style.display = 'none';
    document.getElementById('brandModalTitle').textContent = 'Thêm mới thương hiệu';
    const modal = new bootstrap.Modal(document.getElementById('brandFormModal'));
    modal.show();
  });

  /* ---------- Search & Filter Events ---------- */
  document.getElementById('btnPrevPage')?.addEventListener('click', () => {
    if (currentPage > 0) loadBrands(currentPage - 1);
  });
  document.getElementById('btnNextPage')?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) loadBrands(currentPage + 1);
  });
  document.getElementById('btnSearch')?.addEventListener('click', () => loadBrands(0));
  document.getElementById('searchInput')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') loadBrands(0);
  });
  document.getElementById('statusFilter')?.addEventListener('change', () => loadBrands(0));

  // Initial load
  loadBrands(0);
});
