
/* ==========================================================================
   TechNova Electronics — Manager Users Script
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;
  let rolesList = [];
  let searchTimeout = null;

  const userFormModalEl = document.getElementById('userFormModal');
  const userDetailModalEl = document.getElementById('userDetailModal');

  const userFormModal = userFormModalEl && window.bootstrap
    ? new bootstrap.Modal(userFormModalEl)
    : null;

  const userDetailModal = userDetailModalEl && window.bootstrap
    ? new bootstrap.Modal(userDetailModalEl)
    : null;

  const getElement = (id) => document.getElementById(id);

  function getJwtToken() {
    let token = localStorage.getItem('technova_jwt');

    if (!token) {
      const match = document.cookie.match(/(?:^|;\s*)JWT_TOKEN=([^;]+)/);
      if (match) token = decodeURIComponent(match[1]);
    }

    return token;
  }

  function authHeaders(isJson = false) {
    const headers = { Accept: 'application/json' };
    const token = getJwtToken();

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    if (isJson) {
      headers['Content-Type'] = 'application/json';
    }

    return headers;
  }

  async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    const contentType = response.headers.get('content-type') || '';

    let result;

    if (contentType.includes('application/json')) {
      result = await response.json();
    } else {
      const text = await response.text();
      result = {
        success: false,
        message: text || `HTTP ${response.status}`
      };
    }

    if (!response.ok && result.success !== false) {
      result.success = false;
      result.message ||= `Yêu cầu thất bại (HTTP ${response.status})`;
    }

    if (!response.ok && !result.message) {
      result.message = `Yêu cầu thất bại (HTTP ${response.status})`;
    }

    return result;
  }

  function notify(message, type = 'info') {
    const msg = message || 'Đã xảy ra lỗi không xác định.';

    if (window.toast) {
      window.toast(msg);
    } else if (window.showToast) {
      window.showToast('Thông báo', msg, type);
    } else {
      alert(msg);
    }
  }

  function escapeHtml(value) {
    if (value == null) return '';

    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function formatCurrency(amount) {
    if (amount == null || amount === '') return '0 ₫';

    const value = Number(amount);

    if (!Number.isFinite(value)) return '0 ₫';

    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  }

  function formatDateTime(value) {
    if (!value) return '---';

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return date.toLocaleString('vi-VN');
  }

  function setDisplay(id, display) {
    const element = getElement(id);
    if (element) element.style.display = display;
  }

  function setText(id, value, fallback = '---') {
    const element = getElement(id);

    if (element) {
      element.textContent =
        value == null || value === '' ? fallback : String(value);
    }
  }

  function setValue(id, value) {
    const element = getElement(id);

    if (element) {
      element.value = value == null ? '' : String(value);
    }
  }

  function setDisabled(id, disabled) {
    const element = getElement(id);

    if (element) {
      element.disabled = disabled;
    }
  }

  function setReadOnly(id, readOnly) {
    const element = getElement(id);

    if (element) {
      element.readOnly = readOnly;
    }
  }

  function setRequired(id, required) {
    const element = getElement(id);

    if (element) {
      element.required = required;
    }
  }

  function updateIsActiveLabel() {
    const isActive = Boolean(getElement('userIsActive')?.checked);
    const label = getElement('userIsActiveLabel');

    if (label) {
      label.textContent = isActive ? 'Hoạt động' : 'Tạm khóa';
    }
  }

  function setAvatarPreview(avatar, fullName = 'User') {
    const preview = getElement('avatarPreview');

    if (!preview) return;

    const fallbackUrl =
      `https://ui-avatars.com/api/?name=${encodeURIComponent(fullName || 'User')}` +
      '&background=6c757d&color=fff';

    preview.onerror = () => {
      preview.onerror = null;
      preview.src = fallbackUrl;
    };

    preview.src = avatar || fallbackUrl;
  }

  function setDetailAvatar(avatar, fullName) {
    const image = getElement('detailAvatarImg');
    const fallback = getElement('detailAvatar');

    if (avatar) {
      if (image) {
        image.src = avatar;
        image.style.display = 'block';

        image.onerror = () => {
          image.onerror = null;
          image.style.display = 'none';

          if (fallback) {
            fallback.style.display = 'flex';
            fallback.textContent = (fullName || 'U').charAt(0).toUpperCase();
          }
        };
      }

      if (fallback) fallback.style.display = 'none';
      return;
    }

    if (image) {
      image.removeAttribute('src');
      image.style.display = 'none';
    }

    if (fallback) {
      fallback.style.display = 'flex';
      fallback.textContent = (fullName || 'U').charAt(0).toUpperCase();
    }
  }

  /* ---------- API: Roles ---------- */

  async function loadRoles() {
    try {
      const result = await fetchJson('/api/manager/users/roles', {
        headers: authHeaders()
      });

      if (!result.success || !Array.isArray(result.data)) {
        throw new Error(result.message || 'Không thể tải danh sách vai trò.');
      }

      rolesList = result.data;

      const select = getElement('userRole');
      if (!select) return;

      const previousValue = select.value;

      select.innerHTML =
        '<option value="">-- Chọn vai trò --</option>' +
        rolesList.map(role => {
          const safeRole = escapeHtml(role);

          return `<option value="${safeRole}">${safeRole}</option>`;
        }).join('');

      if (rolesList.includes(previousValue)) {
        select.value = previousValue;
      }
    } catch (error) {
      console.error('Không thể tải danh sách vai trò:', error);
      notify(error.message || 'Không thể tải danh sách vai trò.', 'error');
    }
  }

  /* ---------- API: Users Table ---------- */

  async function loadUsers(page = 0) {
    const search = getElement('searchInput')?.value.trim() || '';

    let url = `/api/manager/users?page=${page}&size=${pageSize}`;

    if (search) {
      url += `&search=${encodeURIComponent(search)}`;
    }

    const tbody = getElement('userTableBody');

    if (tbody) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7" class="text-center py-4 text-muted">
            Đang tải dữ liệu...
          </td>
        </tr>
      `;
    }

    try {
      const result = await fetchJson(url, {
        headers: authHeaders()
      });

      if (!result.success) {
        throw new Error(result.message || 'Không thể tải danh sách người dùng.');
      }

      const data = result.data || {};
      const content = Array.isArray(data.content) ? data.content : [];

      currentPage = Number(data.page ?? page);
      totalPages = Math.max(1, Number(data.totalPages) || 1);

      const totalRecordsText = getElement('totalRecordsText');

      if (totalRecordsText) {
        totalRecordsText.textContent =
          `${Number(data.totalElements) || 0} bản ghi`;
      }

      const pageInfoText = getElement('pageInfoText');

      if (pageInfoText) {
        pageInfoText.textContent = `Trang ${currentPage + 1}/${totalPages}`;
      }

      const currentPageButton = getElement('btnCurrentPage');

      if (currentPageButton) {
        currentPageButton.textContent = String(currentPage + 1);
      }

      const previousButton = getElement('btnPrevPage');
      const nextButton = getElement('btnNextPage');

      if (previousButton) {
        previousButton.disabled = currentPage <= 0;
      }

      if (nextButton) {
        nextButton.disabled = currentPage >= totalPages - 1;
      }

      if (!tbody) return;

      if (content.length === 0) {
        tbody.innerHTML = `
          <tr>
            <td colspan="7" class="text-center py-4 text-muted">
              Không tìm thấy người dùng nào
            </td>
          </tr>
        `;
        return;
      }

      tbody.innerHTML = content.map(user => {
        const id = escapeHtml(user.id);
        const userCode = escapeHtml(user.userCode || '---');
        const fullName = escapeHtml(user.fullName || '---');
        const email = escapeHtml(user.email || '---');
        const role = escapeHtml(user.role || '---');
        const isActive = Boolean(user.isEmailActive);

        const statusBadge = isActive
          ? '<span class="status green">Hoạt động</span>'
          : '<span class="status red">Tạm khóa</span>';

        const avatarHtml = user.avatar
          ? `
            <img
              src="${escapeHtml(user.avatar)}"
              class="rounded-circle object-fit-cover border shadow-sm"
              style="width: 36px; height: 36px;"
              alt="avatar"
              onerror="this.onerror=null;this.style.display='none';this.nextElementSibling.style.display='inline-flex';"
            >
            <div
              class="rounded-circle bg-secondary text-white align-items-center justify-content-center fw-semibold shadow-sm"
              style="display:none;width:36px;height:36px;font-size:13px;"
            >${escapeHtml((user.fullName || 'U').charAt(0).toUpperCase())}</div>
          `
          : `
            <div
              class="rounded-circle bg-secondary text-white d-inline-flex align-items-center justify-content-center fw-semibold shadow-sm"
              style="width:36px;height:36px;font-size:13px;"
            >${escapeHtml((user.fullName || 'U').charAt(0).toUpperCase())}</div>
          `;

        return `
          <tr>
            <td class="text-center align-middle">${avatarHtml}</td>
            <td class="align-middle"><strong>${userCode}</strong></td>
            <td class="align-middle">${fullName}</td>
            <td class="align-middle">${email}</td>
            <td class="align-middle">${role}</td>
            <td class="align-middle">${statusBadge}</td>
            <td class="align-middle">
              <button
                type="button"
                class="action js-view-user"
                data-id="${id}"
                title="Xem chi tiết"
                aria-label="Xem chi tiết"
              >
                <i class="fa-regular fa-eye"></i>
              </button>
              <button
                type="button"
                class="action js-edit-user"
                data-id="${id}"
                title="Chỉnh sửa"
                aria-label="Chỉnh sửa"
              >
                <i class="fa-solid fa-pen"></i>
              </button>
              <button
                type="button"
                class="action text-danger js-delete-user"
                data-id="${id}"
                title="Xóa"
                aria-label="Xóa"
              >
                <i class="fa-solid fa-trash"></i>
              </button>
            </td>
          </tr>
        `;
      }).join('');

      // The action buttons are rebuilt on each page/search render, so bind
      // their handlers only after the new rows exist.
      attachTableEvents();
    } catch (error) {
      console.error('Lỗi tải danh sách người dùng:', error);

      if (tbody) {
        tbody.innerHTML = `
          <tr>
            <td colspan="7" class="text-center py-4 text-danger">
              ${escapeHtml(error.message || 'Không thể tải danh sách người dùng.')}
            </td>
          </tr>
        `;
      }
    }
  }

  /* ---------- Modal Mode: Create / Update ---------- */

  function resetFormForCreate() {
    const form = getElement('userForm');

    clearAllFormErrors(form);

    if (form) form.reset();

    setText('userModalTitle', 'Thêm mới người dùng');
    setText('userModalDescription', 'Nhập thông tin người dùng mới.');

    setValue('userId', '');
    setValue('userCodeInput', '');

    setDisplay('userCodeGroup', 'none');

    // Cho phép nhập thông tin cá nhân khi tạo mới.
    setReadOnly('userFullName', false);
    setReadOnly('userEmail', false);
    setReadOnly('userPhone', false);

    setDisabled('userFullName', false);
    setDisabled('userEmail', false);
    setDisabled('userPhone', false);
    setDisabled('userRole', false);

    // Khôi phục phần chọn avatar.
    setDisplay('avatarEditControls', '');
    setDisplay('avatarUploadInfo', '');

    const avatarInput = getElement('userAvatarFile');

    if (avatarInput) {
      avatarInput.disabled = false;
      avatarInput.value = '';
    }

    setAvatarPreview('', 'User');

    // Hiện lại mật khẩu khi tạo mới.
    setDisplay('userPasswordGroup', '');

    setValue('userPassword', '');
    setDisabled('userPassword', false);
    setRequired('userPassword', true);
    setDisplay('pwdAsterisk', 'inline');
    setDisplay('pwdHelpText', 'none');

    // Khôi phục trạng thái bắt buộc của số điện thoại.
    setRequired('userPhone', true);
    setDisplay('phoneAsterisk', 'inline');

    // Vai trò mặc định.
    const roleSelect = getElement('userRole');

    if (roleSelect) {
      roleSelect.value = rolesList.includes('CUSTOMER') ? 'CUSTOMER' : '';
    }

    const activeSwitch = getElement('userIsActive');

    if (activeSwitch) {
      activeSwitch.disabled = false;
      activeSwitch.checked = true;
    }

    updateIsActiveLabel();
  }

  function configureFormForUpdate(user) {
    const form = getElement('userForm');

    clearAllFormErrors(form);

    setText('userModalTitle', 'Chỉnh sửa người dùng');
    setText(
      'userModalDescription',
      'Chỉ có thể thay đổi vai trò và trạng thái tài khoản.'
    );

    setValue('userId', user.id);
    setValue('userCodeInput', user.userCode || '');

    setDisplay('userCodeGroup', 'block');

    // Thông tin cá nhân chỉ được xem, không được chỉnh sửa.
    setValue('userFullName', user.fullName || '');
    setValue('userEmail', user.email || '');
    setValue('userPhone', user.phone || '');

    setReadOnly('userFullName', true);
    setReadOnly('userEmail', true);
    setReadOnly('userPhone', true);

    // Dùng readOnly thay vì disabled để các trường vẫn hiển thị bình thường.
    setDisabled('userFullName', false);
    setDisabled('userEmail', false);
    setDisabled('userPhone', false);

    // Chỉ vai trò được phép chỉnh sửa.
    const roleSelect = getElement('userRole');

    if (roleSelect) {
      roleSelect.value = user.role || '';
      roleSelect.disabled = false;
    }

    // Trạng thái tài khoản được phép chỉnh sửa.
    const activeSwitch = getElement('userIsActive');

    if (activeSwitch) {
      activeSwitch.checked = Boolean(user.isEmailActive);
      activeSwitch.disabled = false;
    }

    updateIsActiveLabel();

    // Avatar chỉ hiển thị, không cho tải ảnh mới.
    setAvatarPreview(user.avatar, user.fullName || 'User');

    setDisplay('avatarEditControls', 'none');
    setDisplay('avatarUploadInfo', 'none');

    const avatarInput = getElement('userAvatarFile');

    if (avatarInput) {
      avatarInput.value = '';
      avatarInput.disabled = true;
    }

    // Ẩn toàn bộ khu vực mật khẩu trong chế độ cập nhật.
    setDisplay('userPasswordGroup', 'none');
    setValue('userPassword', '');
    setDisabled('userPassword', true);
    setRequired('userPassword', false);

    setDisplay('pwdAsterisk', 'none');
    setDisplay('pwdHelpText', 'none');

    // Số điện thoại không bắt buộc nhập trong chế độ cập nhật.
    setRequired('userPhone', false);
    setDisplay('phoneAsterisk', 'none');
  }

  /* ---------- Table Events ---------- */

  function attachTableEvents() {
    document.querySelectorAll('.js-view-user').forEach(button => {
      button.addEventListener('click', async () => {
        const id = button.dataset.id;

        if (!id) {
          notify('Không tìm thấy mã người dùng.', 'error');
          return;
        }

        button.disabled = true;

        try {
          const result = await fetchJson(
            `/api/manager/users/${encodeURIComponent(id)}`,
            { headers: authHeaders() }
          );

          if (!result.success) {
            throw new Error(result.message || 'Không thể tải chi tiết người dùng.');
          }

          const user = result.data || {};

          setDetailAvatar(user.avatar, user.fullName || 'U');

          setText('detailFullName', user.fullName);
          setText('detailUserCode', user.userCode);
          setText('detailEmail', user.email);
          setText('detailPhone', user.phone, '<chưa có>');
          setText('detailRole', user.role);
          setText('detailPoint', user.point ?? 0, '0');
          setText('detailEWallet', formatCurrency(user.eWallet));
          setText('detailCreatedAt', formatDateTime(user.createdAt));

          const statusElement = getElement('detailStatus');

          if (statusElement) {
            statusElement.innerHTML = user.isEmailActive
              ? '<span class="status green">Hoạt động</span>'
              : '<span class="status red">Tạm khóa</span>';
          }

          if (userDetailModal) {
            userDetailModal.show();
          }
        } catch (error) {
          notify(error.message || 'Không thể xem chi tiết người dùng.', 'error');
        } finally {
          button.disabled = false;
        }
      });
    });

    document.querySelectorAll('.js-edit-user').forEach(button => {
      button.addEventListener('click', async () => {
        const id = button.dataset.id;

        if (!id) {
          notify('Không tìm thấy mã người dùng.', 'error');
          return;
        }

        button.disabled = true;

        try {
          const result = await fetchJson(
            `/api/manager/users/${encodeURIComponent(id)}`,
            { headers: authHeaders() }
          );

          if (!result.success) {
            throw new Error(result.message || 'Không thể tải thông tin người dùng.');
          }

          const user = result.data || {};

          configureFormForUpdate(user);

          if (userFormModal) {
            userFormModal.show();
          }
        } catch (error) {
          notify(error.message || 'Không thể mở biểu mẫu chỉnh sửa.', 'error');
        } finally {
          button.disabled = false;
        }
      });
    });

    document.querySelectorAll('.js-delete-user').forEach(button => {
      button.addEventListener('click', async () => {
        const id = button.dataset.id;

        if (!id) {
          notify('Không tìm thấy mã người dùng.', 'error');
          return;
        }

        if (!confirm('Bạn có chắc chắn muốn xóa người dùng này khỏi hệ thống?')) {
          return;
        }

        button.disabled = true;

        try {
          const result = await fetchJson(
            `/api/manager/users/${encodeURIComponent(id)}`,
            {
              method: 'DELETE',
              headers: authHeaders()
            }
          );

          if (!result.success) {
            throw new Error(result.message || 'Không thể xóa người dùng.');
          }

          notify(result.message || 'Xóa người dùng thành công.', 'success');

          const targetPage = currentPage > 0 ? currentPage : 0;

          await loadUsers(targetPage);

          // If deleting the last row removed this page, return to the previous page.
          if (currentPage > 0 && currentPage >= totalPages) {
            await loadUsers(currentPage - 1);
          }
        } catch (error) {
          notify(error.message || 'Không thể xóa người dùng.', 'error');
        } finally {
          button.disabled = false;
        }
      });
    });
  }

  /* ---------- Validation Helpers ---------- */

  const fieldMapping = {
    fullName: 'userFullName',
    email: 'userEmail',
    phone: 'userPhone',
    role: 'userRole',
    password: 'userPassword',
    newPassword: 'userPassword',
    avatar: 'userAvatarFile',
    avatarFile: 'userAvatarFile',
    isActive: 'userIsActive'
  };

  function setFieldError(fieldKey, message) {
    const inputId = fieldMapping[fieldKey] || fieldKey;
    const input = getElement(inputId);

    if (!input) return false;

    // Không hiển thị lỗi trên trường đang bị ẩn hoặc bị khóa.
    if (input.disabled || input.readOnly) return false;

    const group = input.closest('.mb-3') || input.parentElement;

    input.classList.add('is-invalid');

    const feedback = group?.querySelector('.invalid-feedback');

    if (feedback) {
      feedback.textContent = message || 'Dữ liệu không hợp lệ.';
      feedback.style.display = 'block';
    }

    return true;
  }

  function clearFieldError(input) {
    if (!input) return;

    input.classList.remove('is-invalid');

    const group = input.closest('.mb-3') || input.parentElement;
    const feedback = group?.querySelector('.invalid-feedback');

    if (feedback) {
      feedback.textContent = '';
      feedback.style.display = 'none';
    }
  }

  function clearAllFormErrors(form) {
    if (!form) return;

    form.querySelectorAll('.is-invalid').forEach(element => {
      element.classList.remove('is-invalid');
    });

    form.querySelectorAll('.invalid-feedback').forEach(element => {
      element.textContent = '';
      element.style.display = 'none';
    });
  }

  [
    'userFullName',
    'userEmail',
    'userPhone',
    'userRole',
    'userPassword',
    'userAvatarFile'
  ].forEach(id => {
    const element = getElement(id);

    if (!element) return;

    element.addEventListener('input', () => clearFieldError(element));
    element.addEventListener('change', () => clearFieldError(element));
  });

  /* ---------- Avatar Preview ---------- */

  getElement('userAvatarFile')?.addEventListener('change', event => {
    const input = event.target;
    const file = input.files?.[0];

    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
      setFieldError('avatar', 'File ảnh không được vượt quá 10MB.');
      input.value = '';
      return;
    }

    if (!file.type.startsWith('image/')) {
      setFieldError('avatar', 'Vui lòng chọn tệp hình ảnh hợp lệ.');
      input.value = '';
      return;
    }

    clearFieldError(input);

    const reader = new FileReader();

    reader.onload = eventResult => {
      const preview = getElement('avatarPreview');

      if (preview && typeof eventResult.target?.result === 'string') {
        preview.src = eventResult.target.result;
      }
    };

    reader.onerror = () => {
      notify('Không thể đọc tệp ảnh đã chọn.', 'error');
    };

    reader.readAsDataURL(file);
  });

  /* ---------- Form Validation ---------- */

  function validateUserForm(isUpdate) {
    const fullName = getElement('userFullName')?.value.trim() || '';
    const email = getElement('userEmail')?.value.trim() || '';
    const phone = getElement('userPhone')?.value.trim() || '';
    const role = getElement('userRole')?.value || '';
    const password = getElement('userPassword')?.value || '';

    const phoneRegex = /^(0|\+84)[35789][0-9]{8}$/;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    let isValid = true;

    // Khi cập nhật, chỉ cần kiểm tra vai trò.
    if (isUpdate) {
      if (!role) {
        setFieldError('role', 'Vui lòng chọn vai trò.');
        return false;
      }

      return true;
    }

    if (!fullName) {
      setFieldError('fullName', 'Vui lòng nhập họ tên.');
      isValid = false;
    } else if (fullName.length > 200) {
      setFieldError('fullName', 'Họ tên không được vượt quá 200 ký tự.');
      isValid = false;
    }

    if (!email) {
      setFieldError('email', 'Vui lòng nhập email.');
      isValid = false;
    } else if (!emailRegex.test(email)) {
      setFieldError('email', 'Định dạng email không hợp lệ.');
      isValid = false;
    }

    if (!phone) {
      setFieldError('phone', 'Vui lòng nhập số điện thoại.');
      isValid = false;
    } else if (!phoneRegex.test(phone)) {
      setFieldError(
        'phone',
        'Số điện thoại phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09.'
      );
      isValid = false;
    }

    if (!role) {
      setFieldError('role', 'Vui lòng chọn vai trò.');
      isValid = false;
    }

    if (!password) {
      setFieldError('password', 'Vui lòng nhập mật khẩu.');
      isValid = false;
    } else if (password.length < 6) {
      setFieldError('password', 'Mật khẩu phải có ít nhất 6 ký tự.');
      isValid = false;
    }

    return isValid;
  }

  /* ---------- Open Create Modal ---------- */

  getElement('btnOpenCreateUserModal')?.addEventListener('click', () => {
    resetFormForCreate();

    if (userFormModal) {
      userFormModal.show();
    }
  });

  /* ---------- Form Submit: Create / Update ---------- */

  const userForm = getElement('userForm');

  userForm?.addEventListener('submit', async event => {
    event.preventDefault();

    clearAllFormErrors(userForm);

    const id = getElement('userId')?.value.trim() || '';
    const isUpdate = Boolean(id);

    if (!validateUserForm(isUpdate)) {
      return;
    }

    const role = getElement('userRole')?.value || '';
    const isActive = Boolean(getElement('userIsActive')?.checked);

    const btnSave = getElement('btnSaveUser');

    if (btnSave) {
      btnSave.disabled = true;
    }

    try {
      let result;

      if (isUpdate) {
        // Identity fields stay read-only; the API DTO still requires their values.
        const formData = new FormData();

        formData.append('fullName', getElement('userFullName')?.value.trim() || '');
        formData.append('email', getElement('userEmail')?.value.trim() || '');
        formData.append('phone', getElement('userPhone')?.value.trim() || '');
        formData.append('role', role);
        formData.append('isActive', String(isActive));

        result = await fetchJson(
          `/api/manager/users/${encodeURIComponent(id)}`,
          {
            method: 'PUT',
            headers: authHeaders(),
            body: formData
          }
        );
      } else {
        const fullName = getElement('userFullName')?.value.trim() || '';
        const email = getElement('userEmail')?.value.trim() || '';
        const phone = getElement('userPhone')?.value.trim() || '';
        const password = getElement('userPassword')?.value || '';
        const avatarFile = getElement('userAvatarFile')?.files?.[0];

        const formData = new FormData();

        formData.append('fullName', fullName);
        formData.append('email', email);
        formData.append('phone', phone);
        formData.append('role', role);
        formData.append('password', password);
        formData.append('isActive', String(isActive));

        if (avatarFile) {
          formData.append('avatarFile', avatarFile);
        }

        result = await fetchJson('/api/manager/users', {
          method: 'POST',
          headers: authHeaders(),
          body: formData
        });
      }

      if (!result.success) {
        let hasFieldError = false;

        // Ánh xạ lỗi validation từ backend.
        if (result.data && typeof result.data === 'object') {
          for (const [key, message] of Object.entries(result.data)) {
            if (setFieldError(key, String(message))) {
              hasFieldError = true;
            }
          }
        }

        // Ánh xạ lỗi nghiệp vụ từ backend.
        if (!hasFieldError && result.message) {
          const message = result.message;
          const lower = message.toLowerCase();

          if (lower.includes('email')) {
            hasFieldError = setFieldError('email', message);
          } else if (
            lower.includes('phone') ||
            lower.includes('điện thoại') ||
            lower.includes('sdt') ||
            lower.includes('số điện thoại')
          ) {
            hasFieldError = setFieldError('phone', message);
          } else if (
            lower.includes('họ tên') ||
            lower.includes('full name') ||
            lower.includes('fullname')
          ) {
            hasFieldError = setFieldError('fullName', message);
          } else if (
            lower.includes('mật khẩu') ||
            lower.includes('password')
          ) {
            hasFieldError = setFieldError('password', message);
          } else if (
            lower.includes('vai trò') ||
            lower.includes('role')
          ) {
            hasFieldError = setFieldError('role', message);
          } else if (
            lower.includes('ảnh') ||
            lower.includes('avatar') ||
            lower.includes('image')
          ) {
            hasFieldError = setFieldError('avatar', message);
          }
        }

        if (!hasFieldError) {
          notify(
            result.message || 'Thao tác thất bại. Vui lòng kiểm tra lại.',
            'error'
          );
        }

        return;
      }

      notify(
        result.message ||
          (isUpdate
            ? 'Cập nhật người dùng thành công.'
            : 'Thêm người dùng thành công.'),
        'success'
      );

      if (userFormModal) {
        userFormModal.hide();
      }

      await loadUsers(isUpdate ? currentPage : 0);
    } catch (error) {
      console.error('Lỗi lưu người dùng:', error);

      notify(error.message || 'Không thể lưu thông tin người dùng.', 'error');
    } finally {
      if (btnSave) {
        btnSave.disabled = false;
      }
    }
  });

  /* ---------- Search & Pagination ---------- */

  getElement('searchInput')?.addEventListener('input', () => {
    clearTimeout(searchTimeout);

    searchTimeout = setTimeout(() => {
      loadUsers(0);
    }, 350);
  });

  getElement('btnPrevPage')?.addEventListener('click', () => {
    if (currentPage > 0) {
      loadUsers(currentPage - 1);
    }
  });

  getElement('btnNextPage')?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) {
      loadUsers(currentPage + 1);
    }
  });

  getElement('userIsActive')?.addEventListener('change', updateIsActiveLabel);

  // Khi đóng modal, xóa trạng thái lỗi để lần mở tiếp theo sạch sẽ.
  userFormModalEl?.addEventListener('hidden.bs.modal', () => {
    clearAllFormErrors(getElement('userForm'));
  });

  /* ---------- Initial Load ---------- */

  loadRoles();
  loadUsers(0);
});
