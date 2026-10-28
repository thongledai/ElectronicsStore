/* ==========================================================================
   TechNova Electronics — Promotion Management Script (Employee / Manager)
   AJAX In-Place Render for Instant, Seamless State Updates
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  let totalCount = 0;
  const pageSize = 10;

  const isManager = window.location.pathname.startsWith('/manager');
  const API_BASE = isManager ? '/api/manager/promotions' : '/api/employee/promotions';

  // Elements
  const searchInput = document.getElementById('promoSearchInput');
  const typeFilter = document.getElementById('promoTypeFilter');
  const statusFilter = document.getElementById('promoStatusFilter');
  const btnSearch = document.getElementById('btnPromoSearch');
  const btnRefresh = document.getElementById('btnPromoRefresh');
  const btnOpenCreate = document.getElementById('btnOpenCreatePromo');

  const tableBody = document.getElementById('promoTableBody');
  const totalRecordsText = document.getElementById('promoTotalRecords');
  const pageInfoText = document.getElementById('promoPageInfo');
  const btnCurrentPage = document.getElementById('btnPromoCurrentPage');
  const btnPrevPage = document.getElementById('btnPromoPrevPage');
  const btnNextPage = document.getElementById('btnPromoNextPage');

  // Modal elements
  const promoModalEl = document.getElementById('promoFormModal');
  const promoModal = promoModalEl ? new bootstrap.Modal(promoModalEl) : null;
  const promoForm = document.getElementById('promoForm');
  const promoModalTitle = document.getElementById('promoModalTitle');
  const promoIdInput = document.getElementById('promoId');
  const promoCodeInput = document.getElementById('promoCode');
  const promoDescInput = document.getElementById('promoDescription');
  const promoTypeInput = document.getElementById('promoType');
  const promoPercentInput = document.getElementById('promoPercent');
  const promoMaxDiscountInput = document.getElementById('promoMaxDiscount');
  const promoMinOrderInput = document.getElementById('promoMinOrder');
  const promoQuantityInput = document.getElementById('promoQuantity');
  const promoStartDateInput = document.getElementById('promoStartDate');
  const promoEndDateInput = document.getElementById('promoEndDate');
  const promoIsActiveInput = document.getElementById('promoIsActive');
  const promoErrorAlert = document.getElementById('promoFormError');

  // Delete modal elements
  const deleteModalEl = document.getElementById('promoDeleteModal');
  const deleteModal = deleteModalEl ? new bootstrap.Modal(deleteModalEl) : null;
  const deletePromoCodeText = document.getElementById('deletePromoCodeText');
  const btnConfirmDelete = document.getElementById('btnConfirmDeletePromo');
  let deletingPromoId = null;

  /* ---------- Helpers ---------- */
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

  function notify(msg, type = 'info') {
    if (window.toast) {
      window.toast(msg);
    } else if (window.showToast) {
      const title = type === 'error' ? 'Lỗi' : (type === 'success' ? 'Thành công' : 'Thông báo');
      window.showToast(title, msg, type);
    } else {
      alert(msg);
    }
  }

  function formatMoney(amount) {
    if (amount === null || amount === undefined || Number(amount) === 0) {
      return 'Không giới hạn';
    }
    return Number(amount).toLocaleString('vi-VN') + ' ₫';
  }

  function formatMinOrder(amount) {
    if (amount === null || amount === undefined || Number(amount) === 0) {
      return '0 ₫';
    }
    return Number(amount).toLocaleString('vi-VN') + ' ₫';
  }

  function formatDateTime(isoString) {
    if (!isoString) return '--';
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    const pad = n => String(n).padStart(2, '0');
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function toLocalInputDate(isoString) {
    if (!isoString) return '';
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '';
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function getStatusBadge(status, statusText) {
    switch (status) {
      case 'ACTIVE':
        return `<span class="status green"><i class="fa-solid fa-circle-check me-1"></i>${statusText || 'Đang diễn ra'}</span>`;
      case 'UPCOMING':
        return `<span class="status blue"><i class="fa-solid fa-clock me-1"></i>${statusText || 'Sắp diễn ra'}</span>`;
      case 'EXPIRED':
        return `<span class="status text-muted bg-light border"><i class="fa-solid fa-calendar-xmark me-1"></i>${statusText || 'Đã kết thúc'}</span>`;
      case 'OUT_OF_STOCK':
        return `<span class="status orange"><i class="fa-solid fa-box-open me-1"></i>${statusText || 'Hết lượt dùng'}</span>`;
      case 'INACTIVE':
      default:
        return `<span class="status red"><i class="fa-solid fa-pause me-1"></i>${statusText || 'Tạm ngưng'}</span>`;
    }
  }

  function getTypeBadge(type, typeText) {
    if (type === 'SHIPPING_PERCENT') {
      return `<span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle py-1 px-2"><i class="fa-solid fa-truck-fast me-1"></i>${typeText || 'Vận chuyển'}</span>`;
    }
    return `<span class="badge bg-primary-subtle text-primary border border-primary-subtle py-1 px-2"><i class="fa-solid fa-tag me-1"></i>${typeText || 'Sản phẩm'}</span>`;
  }

  /* ---------- Render Single Row HTML Content ---------- */
  function renderPromotionRowInner(p) {
    const used = p.usedCount || 0;
    const total = p.quantity || 0;
    const remain = Math.max(0, total - used);
    const percent = Number(p.discountPercent).toFixed(0);

    return `
      <td>
        <div class="d-flex align-items-center gap-2">
          <span class="badge bg-dark-subtle text-dark font-monospace fs-6 px-2 py-1 user-select-all border" title="Mã voucher">${p.code}</span>
        </div>
        <small class="text-muted d-block text-truncate mt-1" style="max-width: 200px;" title="${p.description || ''}">${p.description || 'Chưa có mô tả'}</small>
      </td>
      <td>${getTypeBadge(p.type, p.typeText)}</td>
      <td>
        <div class="fw-bold text-danger">Giảm ${percent}%</div>
        <small class="text-muted d-block">Tối đa: ${formatMoney(p.maxDiscountAmount)}</small>
      </td>
      <td>
        <span class="fw-medium">${formatMinOrder(p.minOrderValue)}</span>
      </td>
      <td>
        <div class="small">
          <div>Đã dùng: <strong>${used}</strong> / ${total}</div>
          <div class="text-muted">Còn lại: <span class="badge ${remain > 0 ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'}">${remain}</span></div>
        </div>
      </td>
      <td>
        <div class="small">
          <div><i class="fa-regular fa-calendar-check text-muted me-1"></i>${formatDateTime(p.startDate)}</div>
          <div class="text-muted"><i class="fa-regular fa-calendar-xmark text-muted me-1"></i>${formatDateTime(p.endDate)}</div>
        </div>
      </td>
      <td class="promo-status-cell">${getStatusBadge(p.status, p.statusText)}</td>
      <td class="text-end">
        <div class="btn-group btn-group-sm">
          <button type="button" class="btn btn-outline-secondary js-toggle-promo" data-id="${p.id}"
                  title="${p.isActive ? 'Tạm dừng mã này' : 'Kích hoạt mã này'}">
            <i class="fa-solid ${p.isActive ? 'fa-toggle-on text-success' : 'fa-toggle-off text-muted'} fs-5"></i>
          </button>
          <button type="button" class="btn btn-outline-primary js-edit-promo" data-id="${p.id}" title="Chỉnh sửa">
            <i class="fa-solid fa-pen"></i>
          </button>
          <button type="button" class="btn btn-outline-danger js-delete-promo" data-id="${p.id}" data-code="${p.code}" title="Xóa">
            <i class="fa-solid fa-trash"></i>
          </button>
        </div>
      </td>
    `;
  }

  /* ---------- In-Place Row Update (Zero Flicker) ---------- */
  function updateRowInPlace(row, p) {
    if (!row) return;
    row.innerHTML = renderPromotionRowInner(p);
    attachRowEvents(row);

    // Subtle highlight transition for immediate user feedback
    row.classList.add('table-success');
    row.style.transition = 'background-color 0.8s ease';
    setTimeout(() => {
      row.classList.remove('table-success');
    }, 1000);
  }

  /* ---------- Load Promotions Table ---------- */
  async function loadPromotions(page = 0) {
    if (!tableBody) return;

    const search = searchInput ? searchInput.value.trim() : '';
    const type = typeFilter ? typeFilter.value : '';
    const statusVal = statusFilter ? statusFilter.value : '';

    let url = `${API_BASE}?page=${page}&size=${pageSize}&sortBy=createdAt&sortDir=DESC`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    if (statusVal === 'true' || statusVal === 'false') url += `&isActive=${statusVal}`;

    tableBody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm me-2" role="status"></div>Đang tải dữ liệu...</td></tr>`;

    try {
      const res = await fetch(url, { headers: authHeaders(false) }).then(r => r.json());
      if (!res.success) throw new Error(res.message || 'Không thể tải danh sách voucher');

      const data = res.data;
      currentPage = data.page;
      totalPages = data.totalPages || 1;
      totalCount = data.totalElements || 0;

      if (totalRecordsText) totalRecordsText.textContent = `${totalCount} mã giảm giá`;
      if (pageInfoText) pageInfoText.textContent = `Trang ${currentPage + 1}/${totalPages}`;
      if (btnCurrentPage) btnCurrentPage.textContent = `${currentPage + 1}`;
      if (btnPrevPage) btnPrevPage.disabled = currentPage <= 0;
      if (btnNextPage) btnNextPage.disabled = currentPage >= totalPages - 1;

      if (!data.content || data.content.length === 0) {
        tableBody.innerHTML = `
          <tr>
            <td colspan="8" class="text-center py-5 text-muted">
              <i class="fa-regular fa-ticket fs-2 d-block mb-2 opacity-50"></i>
              Không tìm thấy mã giảm giá nào phù hợp.
            </td>
          </tr>`;
        return;
      }

      tableBody.innerHTML = data.content.map(p => `
        <tr data-id="${p.id}" class="promo-row align-middle">
          ${renderPromotionRowInner(p)}
        </tr>
      `).join('');

      attachRowEvents(tableBody);
    } catch (e) {
      tableBody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger">Lỗi: ${e.message}</td></tr>`;
    }
  }

  /* ---------- Attach Row Events with Scope ---------- */
  function attachRowEvents(scope = document) {
    // Toggle active status (In-place AJAX render)
    scope.querySelectorAll('.js-toggle-promo').forEach(btn => {
      btn.onclick = () => handleTogglePromo(btn);
    });

    // Edit button
    scope.querySelectorAll('.js-edit-promo').forEach(btn => {
      btn.onclick = () => {
        const id = btn.dataset.id;
        openEditModal(id);
      };
    });

    // Delete button
    scope.querySelectorAll('.js-delete-promo').forEach(btn => {
      btn.onclick = () => {
        deletingPromoId = btn.dataset.id;
        const code = btn.dataset.code || '';
        if (deletePromoCodeText) deletePromoCodeText.textContent = code;
        if (deleteModal) deleteModal.show();
      };
    });
  }

  /* ---------- Handle Toggle Status (AJAX Single Voucher Update) ---------- */
  async function handleTogglePromo(btn) {
    const id = btn.dataset.id;
    const row = btn.closest('tr');
    const originalHtml = btn.innerHTML;

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';

    try {
      const res = await fetch(`${API_BASE}/${id}/toggle`, {
        method: 'PATCH',
        headers: authHeaders(false)
      }).then(r => r.json());

      if (!res.success) throw new Error(res.message);

      const updated = res.data;

      // Update ONLY this voucher's row via AJAX without page/table reloading
      if (row) {
        updateRowInPlace(row, updated);
      }

      notify(
        updated.isActive
          ? `Đã kích hoạt mã ${updated.code} (${updated.statusText})`
          : `Đã tạm ngưng mã ${updated.code}`,
        'success'
      );
    } catch (e) {
      notify(e.message || 'Lỗi khi cập nhật trạng thái', 'error');
      btn.disabled = false;
      btn.innerHTML = originalHtml;
    }
  }

  /* ---------- Open Create Modal ---------- */
  function openCreateModal() {
    if (!promoForm) return;
    promoForm.reset();
    if (promoIdInput) promoIdInput.value = '';
    if (promoModalTitle) promoModalTitle.textContent = 'Thêm mới mã giảm giá';
    if (promoErrorAlert) promoErrorAlert.classList.add('d-none');
    if (promoCodeInput) promoCodeInput.readOnly = false;

    // Set default dates: start now, end after 30 days
    const now = new Date();
    const thirtyDaysLater = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
    if (promoStartDateInput) promoStartDateInput.value = toLocalInputDate(now.toISOString());
    if (promoEndDateInput) promoEndDateInput.value = toLocalInputDate(thirtyDaysLater.toISOString());
    if (promoQuantityInput) promoQuantityInput.value = '100';
    if (promoPercentInput) promoPercentInput.value = '10';
    if (promoMaxDiscountInput) promoMaxDiscountInput.value = '50000';
    if (promoMinOrderInput) promoMinOrderInput.value = '0';
    if (promoIsActiveInput) promoIsActiveInput.checked = true;

    if (promoModal) promoModal.show();
  }

  /* ---------- Open Edit Modal ---------- */
  async function openEditModal(id) {
    if (!promoForm) return;
    promoForm.reset();
    if (promoErrorAlert) promoErrorAlert.classList.add('d-none');
    if (promoModalTitle) promoModalTitle.textContent = 'Cập nhật mã giảm giá';

    try {
      const res = await fetch(`${API_BASE}/${id}`, { headers: authHeaders(false) }).then(r => r.json());
      if (!res.success) throw new Error(res.message);

      const p = res.data;
      if (promoIdInput) promoIdInput.value = p.id;
      if (promoCodeInput) {
        promoCodeInput.value = p.code;
        promoCodeInput.readOnly = true; // Lock code on edit
      }
      if (promoDescInput) promoDescInput.value = p.description || '';
      if (promoTypeInput) promoTypeInput.value = p.type;
      if (promoPercentInput) promoPercentInput.value = p.discountPercent || '';
      if (promoMaxDiscountInput) promoMaxDiscountInput.value = p.maxDiscountAmount || '0';
      if (promoMinOrderInput) promoMinOrderInput.value = p.minOrderValue || '0';
      if (promoQuantityInput) promoQuantityInput.value = p.quantity || '1';
      if (promoStartDateInput) promoStartDateInput.value = toLocalInputDate(p.startDate);
      if (promoEndDateInput) promoEndDateInput.value = toLocalInputDate(p.endDate);
      if (promoIsActiveInput) promoIsActiveInput.checked = p.isActive !== false;

      if (promoModal) promoModal.show();
    } catch (e) {
      notify(e.message || 'Lỗi khi tải chi tiết voucher', 'error');
    }
  }

  /* ---------- Form Submit (Create / Update with AJAX In-Place Render) ---------- */
  if (promoForm) {
    promoForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (promoErrorAlert) promoErrorAlert.classList.add('d-none');

      const id = promoIdInput ? promoIdInput.value.trim() : '';
      const code = promoCodeInput ? promoCodeInput.value.trim().toUpperCase() : '';
      const description = promoDescInput ? promoDescInput.value.trim() : '';
      const type = promoTypeInput ? promoTypeInput.value : '';
      const discountPercent = promoPercentInput ? parseFloat(promoPercentInput.value) : null;
      const maxDiscountAmount = promoMaxDiscountInput && promoMaxDiscountInput.value ? parseFloat(promoMaxDiscountInput.value) : 0;
      const minOrderValue = promoMinOrderInput && promoMinOrderInput.value ? parseFloat(promoMinOrderInput.value) : 0;
      const quantity = promoQuantityInput ? parseInt(promoQuantityInput.value, 10) : null;
      const startDateVal = promoStartDateInput ? promoStartDateInput.value : '';
      const endDateVal = promoEndDateInput ? promoEndDateInput.value : '';
      const isActive = promoIsActiveInput ? promoIsActiveInput.checked : true;

      // Basic client validation
      if (!code || code.length < 3 || code.length > 50) {
        showFormError('Mã voucher phải có từ 3 đến 50 ký tự.');
        return;
      }
      if (!/^[A-Za-z0-9_-]+$/.test(code)) {
        showFormError('Mã voucher chỉ được chứa chữ cái, số, dấu gạch nối (-) hoặc gạch dưới (_).');
        return;
      }
      if (!type) {
        showFormError('Vui lòng chọn loại ưu đãi.');
        return;
      }
      if (isNaN(discountPercent) || discountPercent <= 0 || discountPercent > 100) {
        showFormError('Phần trăm giảm phải từ 0.01% đến 100%.');
        return;
      }
      if (isNaN(quantity) || quantity < 1) {
        showFormError('Số lượng voucher tối thiểu là 1.');
        return;
      }
      if (!startDateVal || !endDateVal) {
        showFormError('Vui lòng chọn đầy đủ thời gian bắt đầu và kết thúc.');
        return;
      }
      if (new Date(endDateVal) <= new Date(startDateVal)) {
        showFormError('Thời gian kết thúc phải lớn hơn thời gian bắt đầu.');
        return;
      }

      const payload = {
        code,
        description,
        type,
        discountPercent,
        maxDiscountAmount,
        minOrderValue,
        quantity,
        startDate: new Date(startDateVal).toISOString(),
        endDate: new Date(endDateVal).toISOString(),
        isActive,
        isEnabled: true
      };

      const btnSubmit = document.getElementById('btnSavePromo');
      if (btnSubmit) btnSubmit.disabled = true;

      try {
        let res;
        if (id) {
          // UPDATE VOUCHER VIA AJAX
          res = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: authHeaders(true),
            body: JSON.stringify(payload)
          }).then(r => r.json());

          if (!res.success) throw new Error(res.message);

          const updated = res.data;
          const row = tableBody ? tableBody.querySelector(`tr[data-id="${id}"]`) : null;
          if (row) {
            updateRowInPlace(row, updated);
          } else {
            loadPromotions(currentPage);
          }

          notify(`Cập nhật mã giảm giá ${updated.code} thành công!`, 'success');
          if (promoModal) promoModal.hide();
        } else {
          // CREATE NEW VOUCHER VIA AJAX
          res = await fetch(API_BASE, {
            method: 'POST',
            headers: authHeaders(true),
            body: JSON.stringify(payload)
          }).then(r => r.json());

          if (!res.success) throw new Error(res.message);

          const created = res.data;

          if (currentPage === 0 && tableBody) {
            // Remove empty placeholder row if exists
            const emptyRow = tableBody.querySelector('.text-muted.py-5');
            if (emptyRow) {
              tableBody.innerHTML = '';
            }

            // Prepend new row smoothly to table
            const newTr = document.createElement('tr');
            newTr.setAttribute('data-id', created.id);
            newTr.className = 'promo-row align-middle';
            newTr.innerHTML = renderPromotionRowInner(created);
            tableBody.prepend(newTr);
            attachRowEvents(newTr);

            newTr.classList.add('table-success');
            newTr.style.transition = 'background-color 0.8s ease';
            setTimeout(() => newTr.classList.remove('table-success'), 1200);

            totalCount++;
            if (totalRecordsText) totalRecordsText.textContent = `${totalCount} mã giảm giá`;
          } else {
            loadPromotions(0);
          }

          notify(`Tạo mới mã giảm giá ${created.code} thành công!`, 'success');
          if (promoModal) promoModal.hide();
        }
      } catch (err) {
        showFormError(err.message || 'Lỗi khi lưu mã giảm giá');
      } finally {
        if (btnSubmit) btnSubmit.disabled = false;
      }
    });
  }

  function showFormError(msg) {
    if (promoErrorAlert) {
      promoErrorAlert.textContent = msg;
      promoErrorAlert.classList.remove('d-none');
    } else {
      notify(msg, 'error');
    }
  }

  /* ---------- Confirm Delete with Smooth Removal ---------- */
  if (btnConfirmDelete) {
    btnConfirmDelete.addEventListener('click', async () => {
      if (!deletingPromoId) return;
      btnConfirmDelete.disabled = true;

      try {
        const res = await fetch(`${API_BASE}/${deletingPromoId}`, {
          method: 'DELETE',
          headers: authHeaders(false)
        }).then(r => r.json());

        if (!res.success) throw new Error(res.message);

        notify('Đã xóa / vô hiệu hóa mã giảm giá thành công!', 'success');
        if (deleteModal) deleteModal.hide();

        const row = tableBody ? tableBody.querySelector(`tr[data-id="${deletingPromoId}"]`) : null;
        if (row) {
          row.style.transition = 'all 0.35s ease';
          row.style.opacity = '0';
          row.style.transform = 'scale(0.96)';
          setTimeout(() => {
            row.remove();
            totalCount = Math.max(0, totalCount - 1);
            if (totalRecordsText) totalRecordsText.textContent = `${totalCount} mã giảm giá`;
            if (tableBody.children.length === 0) {
              loadPromotions(Math.max(0, currentPage - 1));
            }
          }, 350);
        }
      } catch (e) {
        notify(e.message || 'Lỗi khi xóa mã giảm giá', 'error');
      } finally {
        btnConfirmDelete.disabled = false;
        deletingPromoId = null;
      }
    });
  }

  /* ---------- Code auto uppercase ---------- */
  if (promoCodeInput) {
    promoCodeInput.addEventListener('input', () => {
      promoCodeInput.value = promoCodeInput.value.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    });
  }

  /* ---------- Search & Filters ---------- */
  if (btnSearch) {
    btnSearch.addEventListener('click', () => loadPromotions(0));
  }
  if (searchInput) {
    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        loadPromotions(0);
      }
    });
  }
  if (typeFilter) {
    typeFilter.addEventListener('change', () => loadPromotions(0));
  }
  if (statusFilter) {
    statusFilter.addEventListener('change', () => loadPromotions(0));
  }
  if (btnRefresh) {
    btnRefresh.addEventListener('click', () => {
      if (searchInput) searchInput.value = '';
      if (typeFilter) typeFilter.value = '';
      if (statusFilter) statusFilter.value = '';
      loadPromotions(0);
    });
  }
  if (btnOpenCreate) {
    btnOpenCreate.addEventListener('click', openCreateModal);
  }

  /* ---------- Pagination Controls ---------- */
  if (btnPrevPage) {
    btnPrevPage.addEventListener('click', () => {
      if (currentPage > 0) loadPromotions(currentPage - 1);
    });
  }
  if (btnNextPage) {
    btnNextPage.addEventListener('click', () => {
      if (currentPage < totalPages - 1) loadPromotions(currentPage + 1);
    });
  }

  // Initial load
  loadPromotions(0);
});
