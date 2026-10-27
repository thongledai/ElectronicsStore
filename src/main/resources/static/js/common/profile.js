/* ==========================================================================
   TechNova Electronics — Common Profile Script
   --------------------------------------------------------------------------
   Shared profile script located in common directory.
   ========================================================================== */
'use strict';

document.addEventListener('DOMContentLoaded', () => {
  function getJwtToken() {
    let token = localStorage.getItem('technova_jwt');
    if (!token) {
      const match = document.cookie.match(new RegExp('(^| )JWT_TOKEN=([^;]+)'));
      if (match) token = match[2];
    }
    return token;
  }

  function authHeaders() {
    const headers = { 'Accept': 'application/json' };
    const token = getJwtToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
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

  function formatCurrency(amount) {
    if (amount == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  }

  // Error helpers
  const fieldMapping = {
    fullName: 'profileFullName',
    phone: 'profilePhone',
    addresses: 'profileAddresses',
    avatar: 'profileAvatarFile',
    avatarFile: 'profileAvatarFile'
  };

  function setFieldError(fieldKey, message) {
    const inputId = fieldMapping[fieldKey] || fieldKey;
    const input = document.getElementById(inputId);
    if (!input) return false;

    input.classList.add('is-invalid');
    const parent = input.closest('.mb-3, .mb-4') || input.parentElement;
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
    const parent = input.closest('.mb-3, .mb-4') || input.parentElement;
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

  // Real-time clearing
  ['profileFullName', 'profilePhone', 'profileAddresses', 'profileAvatarFile'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener('input', () => clearFieldError(el));
      el.addEventListener('change', () => clearFieldError(el));
    }
  });

  // Avatar file preview
  const avatarFileInput = document.getElementById('profileAvatarFile');
  avatarFileInput?.addEventListener('change', (e) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        setFieldError('avatar', 'File ảnh không được vượt quá 10MB');
        e.target.value = '';
        return;
      }
      clearFieldError(avatarFileInput);
      const reader = new FileReader();
      reader.onload = (evt) => {
        const preview = document.getElementById('avatarPreview');
        if (preview) preview.src = evt.target.result;
      };
      reader.readAsDataURL(file);
    }
  });

  /* ---------- Preload cached user to eliminate flashing ---------- */
  function populateFromCache() {
    try {
      const cached = JSON.parse(localStorage.getItem('technova_user') || 'null');
      if (cached) {
        if (cached.name) {
          const summaryFullName = document.getElementById('summaryFullName');
          const profileFullName = document.getElementById('profileFullName');
          if (summaryFullName) summaryFullName.textContent = cached.name;
          if (profileFullName && !profileFullName.value) profileFullName.value = cached.name;
        }
        if (cached.email) {
          const summaryEmail = document.getElementById('summaryEmail');
          const profileEmail = document.getElementById('profileEmail');
          if (summaryEmail) summaryEmail.textContent = cached.email;
          if (profileEmail && !profileEmail.value) profileEmail.value = cached.email;
        }
        if (cached.role) {
          const summaryRole = document.getElementById('summaryRole');
          const roleUpper = cached.role.replace('ROLE_', '').toUpperCase();
          if (summaryRole) summaryRole.textContent = roleUpper;

          const mgmtLink = document.querySelector('.js-mgmt-link');
          if (mgmtLink && roleUpper !== 'CUSTOMER') {
            mgmtLink.style.display = 'flex';
            const rolePath = roleUpper === 'MANAGER' ? '/manager/index' : (roleUpper === 'EMPLOYEE' ? '/employee/orders' : '/shipper/index');
            mgmtLink.setAttribute('href', rolePath);
          }
        }
        const avatarPreview = document.getElementById('avatarPreview');
        if (avatarPreview) {
          if (cached.avatar) {
            avatarPreview.src = cached.avatar;
          } else if (cached.name) {
            avatarPreview.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(cached.name)}&background=0d6efd&color=fff`;
          }
        }
      }
    } catch (_) {}
  }

  /* ---------- Fetch Profile ---------- */
  async function loadProfile() {
    try {
      const res = await fetch('/api/profile', { headers: authHeaders() }).then(r => r.json());
      if (!res.success) {
        if (res.message && res.message.includes('login')) {
          location.href = '/login?redirect=/profile';
          return;
        }
        throw new Error(res.message);
      }

      const u = res.data;
      // Summary card
      const summaryFullName = document.getElementById('summaryFullName');
      if (summaryFullName) summaryFullName.textContent = u.fullName || 'User';

      const summaryEmail = document.getElementById('summaryEmail');
      if (summaryEmail) summaryEmail.textContent = u.email || '---';

      const roleUpper = (u.role || 'CUSTOMER').replace('ROLE_', '').toUpperCase();
      const summaryRole = document.getElementById('summaryRole');
      if (summaryRole) summaryRole.textContent = roleUpper;

      const summaryEWalletCard = document.getElementById('summaryEWalletCard');
      const isCustomer = roleUpper === 'CUSTOMER';
      if (summaryEWalletCard) summaryEWalletCard.hidden = !isCustomer;
      const summaryPointCard = document.getElementById('summaryPointCard');
      if (summaryPointCard) {
        summaryPointCard.classList.toggle('col-6', isCustomer);
        summaryPointCard.classList.toggle('col-12', !isCustomer);
      }

      const mgmtLink = document.querySelector('.js-mgmt-link');
      if (mgmtLink && roleUpper !== 'CUSTOMER') {
        mgmtLink.style.display = 'flex';
        const rolePath = roleUpper === 'MANAGER' ? '/manager/index' : (roleUpper === 'EMPLOYEE' ? '/employee/orders' : '/shipper/index');
        mgmtLink.setAttribute('href', rolePath);
      }

      const summaryUserCode = document.getElementById('summaryUserCode');
      if (summaryUserCode) summaryUserCode.textContent = `Mã: ${u.userCode || '---'}`;

      const summaryPoint = document.getElementById('summaryPoint');
      if (summaryPoint) summaryPoint.textContent = u.point != null ? u.point : 0;

      const summaryEWallet = document.getElementById('summaryEWallet');
      if (summaryEWallet) summaryEWallet.textContent = formatCurrency(u.eWallet);

      const avatarSrc = u.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(u.fullName || 'User')}&background=0d6efd&color=fff`;
      const avatarPreview = document.getElementById('avatarPreview');
      if (avatarPreview) avatarPreview.src = avatarSrc;

      // Form inputs
      const profileFullName = document.getElementById('profileFullName');
      if (profileFullName) profileFullName.value = u.fullName || '';

      const profileEmail = document.getElementById('profileEmail');
      if (profileEmail) profileEmail.value = u.email || '';

      const profilePhone = document.getElementById('profilePhone');
      if (profilePhone) profilePhone.value = u.phone || '';

      const profileAddresses = document.getElementById('profileAddresses');
      if (profileAddresses) profileAddresses.value = u.addresses || '';

      // Update cached user
      try {
        const cached = JSON.parse(localStorage.getItem('technova_user') || '{}');
        cached.name = u.fullName;
        cached.email = u.email;
        cached.role = u.role;
        cached.avatar = u.avatar;
        localStorage.setItem('technova_user', JSON.stringify(cached));
      } catch (_) {}

      // Sync to header / dashboard if available
      if (typeof window.updateHeaderUserAvatar === 'function') {
        window.updateHeaderUserAvatar(u.avatar, u.fullName);
      }
      if (typeof window.updateDashboardUserAvatar === 'function') {
        window.updateDashboardUserAvatar(u.avatar, u.fullName);
      }
    } catch (e) {
      notify('Không thể tải thông tin cá nhân: ' + e.message, 'error');
    }
  }

  /* ---------- Client Validation ---------- */
  function validateProfileForm() {
    const fullName = document.getElementById('profileFullName')?.value.trim() || '';
    const phone = document.getElementById('profilePhone')?.value.trim() || '';
    const addresses = document.getElementById('profileAddresses')?.value.trim() || '';

    const phoneRegex = /^(0|\+84)[35789][0-9]{8}$/;
    let isValid = true;

    if (!fullName) {
      setFieldError('fullName', 'Họ và tên không được để trống');
      isValid = false;
    } else if (fullName.length > 200) {
      setFieldError('fullName', 'Họ và tên không được vượt quá 200 ký tự');
      isValid = false;
    }

    if (phone && !phoneRegex.test(phone)) {
      setFieldError('phone', 'Số điện thoại không hợp lệ (10 chữ số bắt đầu bằng 03, 05, 07, 08, 09)');
      isValid = false;
    }

    if (addresses && addresses.length > 1200) {
      setFieldError('addresses', 'Địa chỉ không được vượt quá 1200 ký tự');
      isValid = false;
    }

    return isValid;
  }

  /* ---------- Submit Profile Form ---------- */
  const profileForm = document.getElementById('profileForm');
  profileForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearAllFormErrors(profileForm);

    if (!validateProfileForm()) {
      return;
    }

    const fullName = document.getElementById('profileFullName').value.trim();
    const phone = document.getElementById('profilePhone').value.trim();
    const addresses = document.getElementById('profileAddresses').value.trim();
    const avatarFile = document.getElementById('profileAvatarFile')?.files?.[0];

    const btnSave = document.getElementById('btnSaveProfile');
    if (btnSave) {
      btnSave.disabled = true;
      btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang lưu...';
    }

    try {
      const formData = new FormData();
      formData.append('fullName', fullName);
      formData.append('phone', phone);
      formData.append('addresses', addresses);
      if (avatarFile) {
        formData.append('avatarFile', avatarFile);
      }

      const res = await fetch('/api/profile', {
        method: 'PUT',
        headers: authHeaders(),
        body: formData
      }).then(r => r.json());

      if (!res.success) {
        let hasFieldErr = false;

        // Structured field errors
        if (res.data && typeof res.data === 'object') {
          for (const [key, msg] of Object.entries(res.data)) {
            if (setFieldError(key, msg)) hasFieldErr = true;
          }
        }

        // Business messages
        if (!hasFieldErr && res.message) {
          const lower = res.message.toLowerCase();
          if (lower.includes('phone') || lower.includes('điện thoại') || lower.includes('sdt')) {
            setFieldError('phone', res.message);
            hasFieldErr = true;
          } else if (lower.includes('họ tên') || lower.includes('full name')) {
            setFieldError('fullName', res.message);
            hasFieldErr = true;
          } else if (lower.includes('địa chỉ') || lower.includes('address')) {
            setFieldError('addresses', res.message);
            hasFieldErr = true;
          } else if (lower.includes('ảnh') || lower.includes('avatar') || lower.includes('image')) {
            setFieldError('avatar', res.message);
            hasFieldErr = true;
          }
        }

        if (!hasFieldErr) {
          notify(res.message || 'Lỗi khi cập nhật hồ sơ, vui lòng kiểm tra lại!', 'error');
        }
        return;
      }

      const updated = res.data;
      notify('Cập nhật thông tin thành công!', 'success');

      // Update summary card
      const summaryFullName = document.getElementById('summaryFullName');
      if (summaryFullName) summaryFullName.textContent = updated.fullName || 'User';

      if (updated.avatar) {
        const preview = document.getElementById('avatarPreview');
        if (preview) preview.src = updated.avatar;
      }

      // Update header navbar avatar and name immediately
      if (typeof window.updateHeaderUserAvatar === 'function') {
        window.updateHeaderUserAvatar(updated.avatar, updated.fullName);
      }
      if (typeof window.updateDashboardUserAvatar === 'function') {
        window.updateDashboardUserAvatar(updated.avatar, updated.fullName);
      }

      // Clear file input
      if (avatarFileInput) avatarFileInput.value = '';
    } catch (e) {
      notify('Lỗi: ' + e.message, 'error');
    } finally {
      if (btnSave) {
        btnSave.disabled = false;
        btnSave.innerHTML = '<i class="fas fa-floppy-disk me-2"></i>Lưu thay đổi';
      }
    }
  });

  // Initial load
  populateFromCache();
  loadProfile();
});
