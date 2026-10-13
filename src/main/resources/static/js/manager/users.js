/* ==========================================================================
   TechNova Electronics — Manager Users Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;
  let rolesList = [];

  const userFormModalEl = document.getElementById('userFormModal');
  const userDetailModalEl = document.getElementById('userDetailModal');
  const userFormModal = userFormModalEl ? new bootstrap.Modal(userFormModalEl) : null;
  const userDetailModal = userDetailModalEl ? new bootstrap.Modal(userDetailModalEl) : null;

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

  function notify(msg, type = 'info') {
    if (window.toast) {
      window.toast(msg);
    } else if (window.showToast) {
      window.showToast('Thông báo', msg, type);
    } else {
      alert(msg);
    }
  }

  function escapeHtml(text) {
    if (!text) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function formatCurrency(amount) {
    if (amount == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  }

  function formatDateTime(isoString) {
    if (!isoString) return '---';
    try {
      const d = new Date(isoString);
      if (isNaN(d.getTime())) return isoString;
      const pad = n => String(n).padStart(2, '0');
      const hours = pad(d.getHours());
      const minutes = pad(d.getMinutes());
      const seconds = pad(d.getSeconds());
      const day = pad(d.getDate());
      const month = pad(d.getMonth() + 1);
      const year = d.getFullYear();
      return `${hours}:${minutes}:${seconds} - ${day}/${month}/${year}`;
    } catch {
      return isoString;
    }
  }

  /* ---------- Fetch Roles List ---------- */
  async function loadRoles() {
    try {
      const res = await fetch('/api/manager/users/roles', { headers: authHeaders(false) }).then(r => r.json());
      if (res.success && Array.isArray(res.data)) {
        rolesList = res.data;
        const select = document.getElementById('userRole');
        if (select) {
          select.innerHTML = '<option value="">-- Chọn vai trò --</option>' +
            rolesList.map(r => `<option value="${r}">${r}</option>`).join('');
        }
      }
    } catch (e) {
      console.warn('Không thể tải danh sách vai trò:', e);
    }
  }

  /* ---------- Load Users Table ---------- */
  async function loadUsers(page = 0) {
    const search = document.getElementById('searchInput')?.value.trim() || '';
    let url = `/api/manager/users?page=${page}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;

    const tbody = document.getElementById('userTableBody');
    if (tbody) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>`;
    }

    try {
      const res = await fetch(url, { headers: authHeaders(false) }).then(r => r.json());
      if (!res.success) throw new Error(res.message);

      const data = res.data;
      currentPage = data.page;
      totalPages = data.totalPages || 1;

      const totalRecordsText = document.getElementById('totalRecordsText');
      if (totalRecordsText) {
        totalRecordsText.textContent = `${data.totalElements} bản ghi`;
      }

      const pageInfoText = document.getElementById('pageInfoText');
      if (pageInfoText) {
        pageInfoText.textContent = `Trang ${currentPage + 1}/${totalPages}`;
      }

      const btnCurrentPage = document.getElementById('btnCurrentPage');
      if (btnCurrentPage) {
        btnCurrentPage.textContent = `${currentPage + 1}`;
      }

      const btnPrev = document.getElementById('btnPrevPage');
      const btnNext = document.getElementById('btnNextPage');
      if (btnPrev) btnPrev.disabled = currentPage <= 0;
      if (btnNext) btnNext.disabled = currentPage >= totalPages - 1;

      if (!data.content || data.content.length === 0) {
        if (tbody) {
          tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Không tìm thấy người dùng nào</td></tr>`;
        }
        return;
      }

      if (tbody) {
        tbody.innerHTML = data.content.map(u => {
          const statusBadge = u.isEmailActive
            ? '<span class="status green">Hoạt động</span>'
            : '<span class="status red">Tạm khóa</span>';

          const avatarHtml = u.avatar
            ? `<img src="${escapeHtml(u.avatar)}" class="rounded-circle object-fit-cover border shadow-sm" style="width: 36px; height: 36px;" alt="avatar" onerror="this.onerror=null; this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(u.fullName || 'U')}&background=random';">`
            : `<div class="rounded-circle bg-secondary text-white d-inline-flex align-items-center justify-content-center fw-semibold shadow-sm" style="width: 36px; height: 36px; font-size: 13px;">${escapeHtml((u.fullName || 'U').charAt(0).toUpperCase())}</div>`;

          return `
            <tr>
              <td class="text-center align-middle">${avatarHtml}</td>
              <td class="align-middle"><strong>${escapeHtml(u.userCode || '---')}</strong></td>
              <td class="align-middle">${escapeHtml(u.fullName)}</td>
              <td class="align-middle">${escapeHtml(u.email)}</td>
              <td class="align-middle">${escapeHtml(u.role)}</td>
              <td class="align-middle">${statusBadge}</td>
              <td class="align-middle">
                <button class="action js-view-user" data-id="${u.id}" title="Xem chi tiết">
                  <i class="fa-regular fa-eye"></i>
                </button>
                <button class="action js-edit-user" data-id="${u.id}" title="Chỉnh sửa">
                  <i class="fa-solid fa-pen"></i>
                </button>
                <button class="action text-danger js-delete-user" data-id="${u.id}" title="Xóa">
                  <i class="fa-solid fa-trash"></i>
                </button>
              </td>
            </tr>
          `;
        }).join('');
      }

      attachTableEvents();
    } catch (e) {
      if (tbody) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Lỗi: ${escapeHtml(e.message)}</td></tr>`;
      }
    }
  }

  function attachTableEvents() {
    // View detail
    document.querySelectorAll('.js-view-user').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/users/${id}`, { headers: authHeaders(false) }).then(r => r.json());
          if (!res.success) throw new Error(res.message);
          const u = res.data;

          const detailAvatarImg = document.getElementById('detailAvatarImg');
          const fallbackAvatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(u.fullName || 'User')}&background=0d6efd&color=fff`;
          if (detailAvatarImg) {
            detailAvatarImg.onerror = () => {
              detailAvatarImg.onerror = null;
              detailAvatarImg.src = fallbackAvatar;
            };
            detailAvatarImg.src = (u.avatar && u.avatar.trim()) ? u.avatar.trim() : fallbackAvatar;
          }

          document.getElementById('detailFullName').textContent = u.fullName || '---';
          document.getElementById('detailUserCode').textContent = u.userCode || '---';
          document.getElementById('detailEmail').textContent = u.email || '---';
          document.getElementById('detailPhone').textContent = u.phone || '<chưa có>';
          document.getElementById('detailRole').textContent = u.role || '---';
          document.getElementById('detailStatus').innerHTML = u.isEmailActive
            ? '<span class="status green">Hoạt động</span>'
            : '<span class="status red">Tạm khóa</span>';
          document.getElementById('detailPoint').textContent = u.point != null ? u.point : 0;
          document.getElementById('detailEWallet').textContent = formatCurrency(u.eWallet);
          document.getElementById('detailCreatedAt').textContent = formatDateTime(u.createdAt);

          if (userDetailModal) userDetailModal.show();
        } catch (e) {
          notify(e.message, 'error');
        }
      });
    });

    // Edit user
    document.querySelectorAll('.js-edit-user').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          const res = await fetch(`/api/manager/users/${id}`, { headers: authHeaders(false) }).then(r => r.json());
          if (!res.success) throw new Error(res.message);
          const u = res.data;

          clearAllFormErrors(document.getElementById('userForm'));

          document.getElementById('userModalTitle').textContent = 'Chỉnh sửa người dùng';
          document.getElementById('userId').value = u.id;
          document.getElementById('userCodeGroup').style.display = 'block';
          document.getElementById('userCodeInput').value = u.userCode || '---';
          document.getElementById('userFullName').value = u.fullName || '';
          document.getElementById('userEmail').value = u.email || '';
          document.getElementById('userEmail').disabled = false;
          document.getElementById('userPhone').value = u.phone || '';
          document.getElementById('userRole').value = u.role || '';
          document.getElementById('userPassword').value = '';
          const phoneAsterisk = document.getElementById('phoneAsterisk');
          if (phoneAsterisk) phoneAsterisk.style.display = 'none';
          document.getElementById('pwdAsterisk').style.display = 'none';
          document.getElementById('pwdHelpText').style.display = 'block';
          document.getElementById('userIsActive').checked = Boolean(u.isEmailActive);
          updateIsActiveLabel();

          const avatarPreview = document.getElementById('avatarPreview');
          if (avatarPreview) {
            avatarPreview.src = u.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(u.fullName || 'User')}&background=6c757d&color=fff`;
          }
          const avatarInput = document.getElementById('userAvatarFile');
          if (avatarInput) avatarInput.value = '';

          if (userFormModal) userFormModal.show();
        } catch (e) {
          notify(e.message, 'error');
        }
      });
    });

    // Delete user
    document.querySelectorAll('.js-delete-user').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        if (!confirm('Bạn có chắc chắn muốn xóa người dùng này khỏi hệ thống?')) return;

        try {
          const res = await fetch(`/api/manager/users/${id}`, {
            method: 'DELETE',
            headers: authHeaders(false)
          }).then(r => r.json());

          if (!res.success) throw new Error(res.message);
          notify(res.message, 'success');
          loadUsers(currentPage);
        } catch (e) {
          notify(e.message, 'error');
        }
      });
    });
  }

  function updateIsActiveLabel() {
    const isChecked = document.getElementById('userIsActive')?.checked;
    const label = document.getElementById('userIsActiveLabel');
    if (label) {
      label.textContent = isChecked ? 'Hoạt động' : 'Tạm khóa';
    }
  }

  document.getElementById('userIsActive')?.addEventListener('change', updateIsActiveLabel);

  /* ---------- Validation Helpers (Inline beneath inputs) ---------- */
  const fieldMapping = {
    fullName: 'userFullName',
    email: 'userEmail',
    phone: 'userPhone',
    role: 'userRole',
    password: 'userPassword',
    newPassword: 'userPassword',
    avatar: 'userAvatarFile',
    avatarFile: 'userAvatarFile'
  };

  function setFieldError(fieldKey, message) {
    const inputId = fieldMapping[fieldKey] || fieldKey;
    const input = document.getElementById(inputId);
    if (!input) return false;

    input.classList.add('is-invalid');
    const parent = input.closest('.mb-3') || input.parentElement;
    const feedback = parent ? parent.querySelector('.invalid-feedback') : null;
    if (feedback) {
      feedback.textContent = message;
      feedback.style.display = 'block';
    }
    return true;
  }

  function clearFieldError(input) {
    if (!input) return;
    input.classList.remove('is-invalid');
    const parent = input.closest('.mb-3') || input.parentElement;
    const feedback = parent ? parent.querySelector('.invalid-feedback') : null;
    if (feedback) {
      feedback.textContent = '';
      feedback.style.display = 'none';
    }
  }

  function clearAllFormErrors(form) {
    if (!form) return;
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => {
      el.textContent = '';
      el.style.display = 'none';
    });
  }

  // Real-time clearing as user types or changes input
  ['userFullName', 'userEmail', 'userPhone', 'userRole', 'userPassword', 'userAvatarFile'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener('input', () => clearFieldError(el));
      el.addEventListener('change', () => clearFieldError(el));
    }
  });

  // Avatar file preview & instant validation
  document.getElementById('userAvatarFile')?.addEventListener('change', (e) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        setFieldError('avatar', 'File ảnh không được vượt quá 10MB');
        e.target.value = '';
        return;
      }
      clearFieldError(document.getElementById('userAvatarFile'));
      const reader = new FileReader();
      reader.onload = (evt) => {
        const preview = document.getElementById('avatarPreview');
        if (preview) preview.src = evt.target.result;
      };
      reader.readAsDataURL(file);
    }
  });

  function validateUserForm(isUpdate) {
    const fullName = document.getElementById('userFullName')?.value.trim() || '';
    const email = document.getElementById('userEmail')?.value.trim() || '';
    const phone = document.getElementById('userPhone')?.value.trim() || '';
    const role = document.getElementById('userRole')?.value || '';
    const password = document.getElementById('userPassword')?.value || '';

    const phoneRegex = /^(0|\+84)[35789][0-9]{8}$/;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    let isValid = true;

    // Full name
    if (!fullName) {
      setFieldError('fullName', 'Full name is required');
      isValid = false;
    } else if (fullName.length > 200) {
      setFieldError('fullName', 'Full name must not exceed 200 characters');
      isValid = false;
    }

    // Email (validated for both create and update)
    if (!email) {
      setFieldError('email', 'Email is required');
      isValid = false;
    } else if (!emailRegex.test(email)) {
      setFieldError('email', 'Invalid email format');
      isValid = false;
    }

    // Phone
    if (!isUpdate) {
      if (!phone) {
        setFieldError('phone', 'Phone number is required');
        isValid = false;
      } else if (!phoneRegex.test(phone)) {
        setFieldError('phone', 'Invalid Vietnamese phone number (10 digits starting with 03, 05, 07, 08, 09)');
        isValid = false;
      }
    } else {
      if (phone && !phoneRegex.test(phone)) {
        setFieldError('phone', 'Invalid Vietnamese phone number (10 digits starting with 03, 05, 07, 08, 09)');
        isValid = false;
      }
    }

    // Role
    if (!role) {
      setFieldError('role', 'Role is required');
      isValid = false;
    }

    // Password
    if (!isUpdate) {
      if (!password) {
        setFieldError('password', 'Password is required');
        isValid = false;
      } else if (password.length < 6) {
        setFieldError('password', 'Password must be at least 6 characters');
        isValid = false;
      }
    } else {
      if (password && password.length < 6) {
        setFieldError('password', 'Password must be at least 6 characters');
        isValid = false;
      }
    }

    return isValid;
  }

  /* ---------- Open Create Modal ---------- */
  document.getElementById('btnOpenCreateUserModal')?.addEventListener('click', () => {
    const userForm = document.getElementById('userForm');
    clearAllFormErrors(userForm);

    document.getElementById('userModalTitle').textContent = 'Thêm mới người dùng';
    document.getElementById('userId').value = '';
    document.getElementById('userCodeGroup').style.display = 'none';
    document.getElementById('userCodeInput').value = '';
    document.getElementById('userFullName').value = '';
    document.getElementById('userEmail').value = '';
    document.getElementById('userEmail').disabled = false;
    document.getElementById('userPhone').value = '';
    document.getElementById('userRole').value = 'CUSTOMER';
    document.getElementById('userPassword').value = '';
    const phoneAsterisk = document.getElementById('phoneAsterisk');
    if (phoneAsterisk) phoneAsterisk.style.display = 'inline';
    document.getElementById('pwdAsterisk').style.display = 'inline';
    document.getElementById('pwdHelpText').style.display = 'none';
    document.getElementById('userIsActive').checked = true;
    updateIsActiveLabel();

    const avatarPreview = document.getElementById('avatarPreview');
    if (avatarPreview) {
      avatarPreview.src = 'https://ui-avatars.com/api/?name=User&background=6c757d&color=fff';
    }
    const avatarInput = document.getElementById('userAvatarFile');
    if (avatarInput) avatarInput.value = '';

    if (userFormModal) userFormModal.show();
  });

  /* ---------- Form Submit (Create or Update) ---------- */
  const userForm = document.getElementById('userForm');
  userForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearAllFormErrors(userForm);

    const id = document.getElementById('userId').value;
    const isUpdate = Boolean(id);

    // Client-side validation check
    if (!validateUserForm(isUpdate)) {
      return;
    }

    const fullName = document.getElementById('userFullName').value.trim();
    const email = document.getElementById('userEmail').value.trim();
    const phone = document.getElementById('userPhone').value.trim();
    const role = document.getElementById('userRole').value;
    const password = document.getElementById('userPassword').value;
    const isActive = document.getElementById('userIsActive').checked;
    const avatarFile = document.getElementById('userAvatarFile')?.files?.[0];

    const btnSave = document.getElementById('btnSaveUser');
    if (btnSave) btnSave.disabled = true;

    try {
      let res;
      const formData = new FormData();
      formData.append('fullName', fullName);
      formData.append('email', email);
      formData.append('phone', phone);
      formData.append('role', role);
      formData.append('isActive', isActive);
      if (avatarFile) {
        formData.append('avatarFile', avatarFile);
      }

      if (isUpdate) {
        if (password.trim()) {
          formData.append('newPassword', password.trim());
        }
        res = await fetch(`/api/manager/users/${id}`, {
          method: 'PUT',
          headers: authHeaders(false),
          body: formData
        }).then(r => r.json());
      } else {
        formData.append('password', password);
        res = await fetch('/api/manager/users', {
          method: 'POST',
          headers: authHeaders(false),
          body: formData
        }).then(r => r.json());
      }

      if (!res.success) {
        let hasFieldErr = false;

        // Map structured field errors from backend (e.g. DTO validation)
        if (res.data && typeof res.data === 'object') {
          for (const [key, msg] of Object.entries(res.data)) {
            if (setFieldError(key, msg)) {
              hasFieldErr = true;
            }
          }
        }

        // Map backend business exceptions (e.g. USER_EMAIL_EXISTS, USER_PHONE_EXISTS)
        if (!hasFieldErr && res.message) {
          const lower = res.message.toLowerCase();
          if (lower.includes('email')) {
            setFieldError('email', res.message);
            hasFieldErr = true;
          } else if (lower.includes('phone') || lower.includes('điện thoại') || lower.includes('sdt')) {
            setFieldError('phone', res.message);
            hasFieldErr = true;
          } else if (lower.includes('họ tên') || lower.includes('full name') || lower.includes('fullname')) {
            setFieldError('fullName', res.message);
            hasFieldErr = true;
          } else if (lower.includes('mật khẩu') || lower.includes('password')) {
            setFieldError('password', res.message);
            hasFieldErr = true;
          } else if (lower.includes('vai trò') || lower.includes('role')) {
            setFieldError('role', res.message);
            hasFieldErr = true;
          } else if (lower.includes('ảnh') || lower.includes('avatar') || lower.includes('image')) {
            setFieldError('avatar', res.message);
            hasFieldErr = true;
          }
        }

        // Only pop up toast if the error could NOT be displayed under any input
        if (!hasFieldErr) {
          notify(res.message || 'Lỗi thao tác, vui lòng kiểm tra lại!', 'error');
        }
        return;
      }

      notify(res.message, 'success');
      if (userFormModal) userFormModal.hide();
      loadUsers(isUpdate ? currentPage : 0);
    } catch (e) {
      notify(e.message, 'error');
    } finally {
      if (btnSave) btnSave.disabled = false;
    }
  });

  /* ---------- Search & Pagination Events ---------- */
  let searchTimeout = null;
  document.getElementById('searchInput')?.addEventListener('input', () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      loadUsers(0);
    }, 350);
  });

  document.getElementById('btnPrevPage')?.addEventListener('click', () => {
    if (currentPage > 0) loadUsers(currentPage - 1);
  });

  document.getElementById('btnNextPage')?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) loadUsers(currentPage + 1);
  });

  /* ---------- Initial Load ---------- */
  loadRoles();
  loadUsers(0);
});
