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
    else if (window.showToast) window.showToast(UiMessage.NOTICE, msg, 'info');
    else alert(msg);
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, character => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    })[character]);
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

    let isActive = '';
    if (status === 'active') isActive = 'true';
    if (status === 'deleted') isActive = 'false';

    let url = `/api/manager/styles?page=${page}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (isActive) url += `&isActive=${isActive}`;

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

        const statusBadge = s.isActive
          ? '<span class="status green">Hoạt động</span>'
          : '<span class="status red">Đã xóa mềm</span>';

        return `
          <tr>
			<td>${catBadges}</td>
            <td><strong>${s.name}</strong></td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <button class="action js-manage-values" data-id="${s.id}" data-name="${s.name}" title="Quản lý giá trị"><i class="fa-solid fa-list-ul"></i></button>
              <button class="action js-edit-style" data-id="${s.id}" title="Chỉnh sửa"><i class="fa-solid fa-pen"></i></button>
              <button class="action text-danger js-delete-style" data-id="${s.id}" title="Xóa vĩnh viễn"><i class="fa-solid fa-trash"></i></button>
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
            document.getElementById('styleIsActive').checked = s.isActive;
            renderCategoryCheckboxes(s.categoryIds || []);
            document.getElementById('styleModalTitle').textContent = 'Chỉnh sửa kiểu thuộc tính';
            const modal = new bootstrap.Modal(document.getElementById('styleFormModal'));
            modal.show();
          }
        } catch (e) {
          notify(UiMessage.STYLE_LOAD_ERROR(e.message));
        }
      });
    });

    // Permanently delete Style
    document.querySelectorAll('.js-delete-style').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Bạn có chắc chắn muốn xóa vĩnh viễn kiểu thuộc tính và các giá trị của nó khỏi cơ sở dữ liệu?')) return;
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/styles/${id}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());
          if (res.success) {
            notify(UiMessage.STYLE_DELETED);
            loadStyles(currentPage);
          } else {
            notify(UiMessage.ERROR_PREFIX + res.message);
          }
        } catch (e) {
          notify(UiMessage.ERROR_PREFIX + e.message);
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
        categoryIds: selectedCatIds,
        isActive: document.getElementById('styleIsActive').checked
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
          notify(UiMessage.ERROR_PREFIX + res.message);
        }
      } catch (err) {
        notify(UiMessage.ERROR_PREFIX + err.message);
      }
    });
  }

  document.getElementById('btnOpenCreateStyleModal')?.addEventListener('click', () => {
    document.getElementById('styleId').value = '';
    document.getElementById('styleForm').reset();
    document.getElementById('styleIsActive').checked = true;
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
        const statusBadge = sv.isActive
          ? '<span class="status green">Hoạt động</span>'
          : '<span class="status red">Đã xóa mềm</span>';

        return `
          <tr>
            <td class="js-sv-name-cell"><strong>${escapeHtml(sv.name)}</strong></td>
            <td>${statusBadge}</td>
            <td class="text-end">
              <div class="form-check form-switch d-inline-block mb-0 js-sv-toggle-wrap">
                <input class="form-check-input js-toggle-sv" type="checkbox" data-id="${sv.id}" ${sv.isActive ? 'checked' : ''} aria-label="Trạng thái ${escapeHtml(sv.name)}">
              </div>
              <button class="action js-edit-sv" data-id="${sv.id}" title="Sửa tên"><i class="fa-solid fa-pen"></i></button>
              <button class="action text-success js-save-sv d-none" data-id="${sv.id}" title="Lưu"><i class="fa-solid fa-check"></i></button>
              <button class="action js-cancel-sv d-none" data-id="${sv.id}" title="Hủy"><i class="fa-solid fa-xmark"></i></button>
              <button class="action text-danger js-delete-sv" data-id="${sv.id}" title="Xóa vĩnh viễn"><i class="fa-solid fa-trash"></i></button>
            </td>
          </tr>
        `;
      }).join('');

      // Update the soft-delete state
      tbody.querySelectorAll('.js-toggle-sv').forEach(input => {
        input.addEventListener('change', async () => {
          const value = res.data.content.find(item => item.id === input.dataset.id);
          const isActive = input.checked;
          try {
            const updateRes = await fetch(`/api/manager/style-values/${input.dataset.id}`, {
              method: 'PUT',
              headers: authHeaders(true),
              body: JSON.stringify({ name: value.name, styleId, isActive })
            }).then(r => r.json());
            if (updateRes.success) {
              notify(isActive ? 'Đã kích hoạt giá trị' : 'Đã ngừng hoạt động giá trị');
              loadStyleValues(styleId);
            } else {
              input.checked = !isActive;
              notify(UiMessage.ERROR_PREFIX + updateRes.message);
            }
          } catch (e) {
            input.checked = !isActive;
            notify(UiMessage.ERROR_PREFIX + e.message);
          }
        });
      });

      tbody.querySelectorAll('.js-edit-sv').forEach(button => {
        button.addEventListener('click', () => {
          const row = button.closest('tr');
          const value = res.data.content.find(item => item.id === button.dataset.id);
          const input = document.createElement('input');
          input.type = 'text';
          input.maxLength = 100;
          input.className = 'form-control form-control-sm js-sv-name-input';
          input.value = value.name;
          row.querySelector('.js-sv-name-cell').replaceChildren(input);
          row.querySelector('.js-sv-toggle-wrap').classList.add('d-none');
          button.classList.add('d-none');
          row.querySelector('.js-save-sv').classList.remove('d-none');
          row.querySelector('.js-cancel-sv').classList.remove('d-none');
          input.addEventListener('keydown', event => {
            if (event.key === 'Enter') row.querySelector('.js-save-sv').click();
            if (event.key === 'Escape') row.querySelector('.js-cancel-sv').click();
          });
          input.focus();
          input.select();
        });
      });

      tbody.querySelectorAll('.js-save-sv').forEach(button => {
        button.addEventListener('click', async () => {
          const id = button.dataset.id;
          const row = button.closest('tr');
          const value = res.data.content.find(item => item.id === id);
          const name = row.querySelector('.js-sv-name-input').value.trim();
          if (!name) {
            notify(UiMessage.STYLE_VALUE_NAME_REQUIRED);
            return;
          }

          try {
            const updateRes = await fetch(`/api/manager/style-values/${id}`, {
              method: 'PUT',
              headers: authHeaders(true),
              body: JSON.stringify({ name, styleId, isActive: value.isActive })
            }).then(r => r.json());
            if (updateRes.success) {
              notify(UiMessage.STYLE_VALUE_UPDATED);
              loadStyleValues(styleId);
            } else {
              notify(UiMessage.ERROR_PREFIX + updateRes.message);
            }
          } catch (e) {
            notify(UiMessage.ERROR_PREFIX + e.message);
          }
        });
      });

      tbody.querySelectorAll('.js-cancel-sv').forEach(button => {
        button.addEventListener('click', () => loadStyleValues(styleId));
      });

      // Permanently delete SV
      tbody.querySelectorAll('.js-delete-sv').forEach(btn => {
        btn.addEventListener('click', async () => {
          if (!confirm('Bạn có chắc chắn muốn xóa vĩnh viễn giá trị này khỏi cơ sở dữ liệu?')) return;
          const id = btn.dataset.id;
          try {
            const delRes = await fetch(`/api/manager/style-values/${id}`, {
              method: 'DELETE',
              headers: authHeaders(false)
            }).then(r => r.json());
            if (delRes.success) {
              notify(UiMessage.STYLE_VALUE_DELETED);
              loadStyleValues(styleId);
            } else {
              notify(UiMessage.ERROR_PREFIX + delRes.message);
            }
          } catch (e) {
            notify(UiMessage.ERROR_PREFIX + e.message);
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
      notify(UiMessage.STYLE_VALUE_NAME_REQUIRED);
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
        notify(UiMessage.STYLE_VALUE_CREATED);
        input.value = '';
        loadStyleValues(activeStyleId);
      } else {
        notify(UiMessage.ERROR_PREFIX + res.message);
      }
    } catch (e) {
      notify(UiMessage.ERROR_PREFIX + e.message);
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
