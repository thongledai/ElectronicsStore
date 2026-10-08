/* ==========================================================================
   TechNova Electronics — Manager Styles & Style Values Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;
  let activeStyleId = null;
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

  /* ---------- Load Category Options for Checkboxes ---------- */
  async function loadCategoryOptions() {
    try {
      const res = await fetch('/api/manager/categories/options', { headers: authHeaders(false) }).then(r => r.json());
      if (res.success) {
        categoryOptions = res.data || [];
      }
    } catch (e) {
      console.warn('Lỗi tải danh mục:', e);
    }
  }

  function renderCategoryCheckboxes(selectedIds = []) {
    const container = document.getElementById('styleCategoryCheckboxes');
    if (!container) return;
    container.innerHTML = categoryOptions.map(c => `
      <div class="form-check">
        <input class="form-check-input js-category-check" type="checkbox" value="${c.id}" id="cat_${c.id}" ${selectedIds.includes(c.id) ? 'checked' : ''}>
        <label class="form-check-label" for="cat_${c.id}">${c.name}</label>
      </div>
    `).join('');
  }

  /* ---------- Load Styles Table ---------- */
  async function loadStyles(page = 0) {
    const search = document.getElementById('searchInput')?.value.trim() || '';
    const status = document.getElementById('statusFilter')?.value || '';

    let isDeleted = '';
    if (status === 'active') isDeleted = 'false';
    if (status === 'deleted') isDeleted = 'true';

    let url = `/api/manager/styles?page=${page}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (isDeleted) url += `&isDeleted=${isDeleted}`;

    const tbody = document.getElementById('styleTableBody');
    if (tbody) tbody.innerHTML = `<tr><td colspan="4" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;

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
        tbody.innerHTML = `<tr><td colspan="4" class="text-center py-4 text-muted">Không tìm thấy kiểu thuộc tính nào</td></tr>`;
        return;
      }

      tbody.innerHTML = data.content.map(s => {
        const catBadges = (s.categoryNames && s.categoryNames.length > 0)
          ? s.categoryNames.map(name => `<span class="badge bg-secondary me-1 mb-1">${name}</span>`).join('')
          : '<span class="text-muted">Chưa gán danh mục</span>';

        const statusBadge = s.isDeleted
          ? '<span class="status red">Đã xóa mềm</span>'
          : '<span class="status green">Hoạt động</span>';

        return `
          <tr>
			<td>${catBadges}</td>
            <td><strong>${s.name}</strong></td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <button class="action js-manage-values" data-id="${s.id}" data-name="${s.name}" title="Quản lý giá trị"><i class="fa-solid fa-list-ul"></i></button>
              <button class="action js-edit-style" data-id="${s.id}" title="Chỉnh sửa"><i class="fa-solid fa-pen"></i></button>
              ${s.isDeleted
                ? `<button class="action text-success js-restore-style" data-id="${s.id}" title="Khôi phục"><i class="fa-solid fa-rotate-left"></i></button>`
                : `<button class="action text-danger js-delete-style" data-id="${s.id}" title="Xóa mềm"><i class="fa-solid fa-trash"></i></button>`
              }
            </td>
          </tr>
        `;
      }).join('');

      attachEvents();
    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="4" class="text-center py-4 text-danger">Lỗi: ${e.message}</td></tr>`;
    }
  }

  function attachEvents() {
    // Edit Style
    document.querySelectorAll('.js-edit-style').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/styles/${id}`, { headers: authHeaders(false) }).then(r => r.json());
          if (res.success && res.data) {
            const s = res.data;
            document.getElementById('styleId').value = s.id;
            document.getElementById('styleName').value = s.name;
            renderCategoryCheckboxes(s.categoryIds || []);
            document.getElementById('styleModalTitle').textContent = 'Chỉnh sửa kiểu thuộc tính';
            const modal = new bootstrap.Modal(document.getElementById('styleFormModal'));
            modal.show();
          }
        } catch (e) {
          notify('Lỗi tải kiểu thuộc tính: ' + e.message);
        }
      });
    });

    // Delete Style
    document.querySelectorAll('.js-delete-style').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc chắn muốn xóa mềm kiểu thuộc tính này cùng các giá trị của nó?')) return;
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/styles/${id}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã xóa mềm kiểu thuộc tính');
            loadStyles(currentPage);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Restore Style
    document.querySelectorAll('.js-restore-style').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/styles/${id}/restore`, {
            method: 'POST',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify('Đã khôi phục kiểu thuộc tính');
            loadStyles(currentPage);
          } else {
            notify('Lỗi: ' + res.message);
          }
        } catch (e) {
          notify('Lỗi: ' + e.message);
        }
      });
    });

    // Manage StyleValues
    document.querySelectorAll('.js-manage-values').forEach(btn => {
      btn.addEventListener('click', () => {
        activeStyleId = btn.dataset.id;
        document.getElementById('modalStyleName').textContent = btn.dataset.name;
        document.getElementById('newStyleValueName').value = '';
        loadStyleValues(activeStyleId);
        const modal = new bootstrap.Modal(document.getElementById('styleValuesModal'));
        modal.show();
      });
    });
  }

  /* ---------- Style Form Submit (Create / Edit) ---------- */
  const styleForm = document.getElementById('styleForm');
  if (styleForm) {
    styleForm.addEventListener('submit', async e => {
      e.preventDefault();
      const id = document.getElementById('styleId').value;
      const selectedCatIds = Array.from(document.querySelectorAll('.js-category-check:checked'))
        .map(el => el.value);

      const payload = {
        name: document.getElementById('styleName').value.trim(),
        categoryIds: selectedCatIds
      };

      try {
        const url = id ? `/api/manager/styles/${id}` : '/api/manager/styles';
        const method = id ? 'PUT' : 'POST';

        const res = await fetch(url, {
          method: method,
          headers: authHeaders(true),
          body: JSON.stringify(payload)
        }).then(r => r.json());

        if (res.success) {
          notify(id ? 'Cập nhật kiểu thuộc tính thành công' : 'Tạo mới kiểu thuộc tính thành công');
          bootstrap.Modal.getInstance(document.getElementById('styleFormModal'))?.hide();
          loadStyles(currentPage);
        } else {
          notify('Lỗi: ' + res.message);
        }
      } catch (err) {
        notify('Lỗi: ' + err.message);
      }
    });
  }

  document.getElementById('btnOpenCreateStyleModal')?.addEventListener('click', () => {
    document.getElementById('styleId').value = '';
    document.getElementById('styleForm').reset();
    renderCategoryCheckboxes([]);
    document.getElementById('styleModalTitle').textContent = 'Thêm mới kiểu thuộc tính';
    const modal = new bootstrap.Modal(document.getElementById('styleFormModal'));
    modal.show();
  });

  /* ---------- Style Values Logic ---------- */
  async function loadStyleValues(styleId) {
    const tbody = document.getElementById('styleValuesTableBody');
    tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-muted">Đang tải giá trị...</td></tr>`;

    try {
      const res = await fetch(`/api/manager/style-values?styleId=${styleId}&size=100`, {
        headers: authHeaders(false)
      }).then(r => r.json());

      if (!res.success || !res.data || !res.data.content || res.data.content.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-muted">Chưa có giá trị nào</td></tr>`;
        return;
      }

      tbody.innerHTML = res.data.content.map(sv => {
        const statusBadge = sv.isDeleted
          ? '<span class="status red">Đã xóa mềm</span>'
          : '<span class="status green">Hoạt động</span>';

        return `
          <tr>
            <td><strong>${sv.name}</strong></td>
            <td>${statusBadge}</td>
            <td class="text-end">
              ${sv.isDeleted
                ? `<button class="action text-success js-restore-sv" data-id="${sv.id}" title="Khôi phục"><i class="fa-solid fa-rotate-left"></i></button>`
                : `<button class="action text-danger js-delete-sv" data-id="${sv.id}" title="Xóa mềm"><i class="fa-solid fa-trash"></i></button>`
              }
            </td>
          </tr>
        `;
      }).join('');

      // Delete SV
      tbody.querySelectorAll('.js-delete-sv').forEach(btn => {
        btn.addEventListener('click', async () => {
          if (!confirm('Bạn có chắc muốn xóa mềm giá trị này?')) return;
          const id = btn.dataset.id;
          try {
            const delRes = await fetch(`/api/manager/style-values/${id}`, {
              method: 'DELETE',
              headers: authHeaders(false)
            }).then(r => r.json());
            if (delRes.success) {
              notify('Đã xóa giá trị');
              loadStyleValues(styleId);
            } else {
              notify('Lỗi: ' + delRes.message);
            }
          } catch (e) {
            notify('Lỗi: ' + e.message);
          }
        });
      });

      // Restore SV
      tbody.querySelectorAll('.js-restore-sv').forEach(btn => {
        btn.addEventListener('click', async () => {
          const id = btn.dataset.id;
          try {
            const res = await fetch(`/api/manager/style-values/${id}/restore`, {
              method: 'POST',
              headers: authHeaders(false)
            }).then(r => r.json());
            if (res.success) {
              notify('Đã khôi phục giá trị');
              loadStyleValues(styleId);
            } else {
              notify('Lỗi: ' + res.message);
            }
          } catch (e) {
            notify('Lỗi: ' + e.message);
          }
        });
      });
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-danger">Lỗi: ${e.message}</td></tr>`;
    }
  }

  // Add Style Value
  document.getElementById('btnAddStyleValue')?.addEventListener('click', async () => {
    const input = document.getElementById('newStyleValueName');
    const name = input.value.trim();
    if (!name) {
      notify('Vui lòng nhập tên giá trị');
      return;
    }

    try {
      const res = await fetch('/api/manager/style-values', {
        method: 'POST',
        headers: authHeaders(true),
        body: JSON.stringify({
          styleId: activeStyleId,
          name: name
        })
      }).then(r => r.json());

      if (res.success) {
        notify('Thêm giá trị thành công');
        input.value = '';
        loadStyleValues(activeStyleId);
      } else {
        notify('Lỗi: ' + res.message);
      }
    } catch (e) {
      notify('Lỗi: ' + e.message);
    }
  });

  /* ---------- Search & Filter Events ---------- */
  document.getElementById('btnPrevPage')?.addEventListener('click', () => {
    if (currentPage > 0) loadStyles(currentPage - 1);
  });
  document.getElementById('btnNextPage')?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) loadStyles(currentPage + 1);
  });
  document.getElementById('btnSearch')?.addEventListener('click', () => loadStyles(0));
  document.getElementById('searchInput')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') loadStyles(0);
  });
  document.getElementById('statusFilter')?.addEventListener('change', () => loadStyles(0));

  // Initial load
  loadCategoryOptions();
  loadStyles(0);
});
