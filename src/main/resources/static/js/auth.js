/* ==========================================================================
   UTE Shop — Unified Authentication & AJAX Client (auth.js)
   --------------------------------------------------------------------------
   • DTO-Driven Server Validation (Binding directly from Spring Boot @Valid)
   • AJAX endpoints at /auth/**
   • 5-minute OTP countdown timer with Resend OTP functionality
   • Complete logout cleanup (localStorage, cookies, session)
   • 100% English UI and messages
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. UTILITIES & HELPERS
   ========================================================================== */


/**
 * Modern Floating Toast Notification
 * - Injects a floating container at top-right corner.
 * - Supports 'success', 'danger' / 'error', 'warning', 'info'.
 * - Modern card styling: rounded-3, drop-shadow, font-awesome icons, close button, smooth CSS animation.
 * - Zero fallback to window.alert().
 */
function showAppToast(title, message = '', type = 'info', duration = 3500) {
  let stack = document.getElementById('app-toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.id = 'app-toast-stack';
    stack.setAttribute('aria-live', 'polite');
    stack.style.cssText = `
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 100000;
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-width: 400px;
      width: calc(100vw - 32px);
      pointer-events: none;
    `;
    document.body.appendChild(stack);
  }

  const icons = {
    success: 'fa-solid fa-circle-check text-success',
    danger: 'fa-solid fa-circle-xmark text-danger',
    error: 'fa-solid fa-circle-xmark text-danger',
    warning: 'fa-solid fa-triangle-exclamation text-warning',
    info: 'fa-solid fa-circle-info text-primary'
  };

  const borderColors = {
    success: '#20c997',
    danger: '#dc3545',
    error: '#dc3545',
    warning: '#ffc107',
    info: '#0d6efd'
  };

  const toastEl = document.createElement('div');
  toastEl.style.cssText = `
    pointer-events: auto;
    background: #ffffff;
    color: #1a202c;
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.06);
    border-left: 5px solid ${borderColors[type] || borderColors.info};
    padding: 14px 16px;
    display: flex;
    align-items: flex-start;
    gap: 12px;
    opacity: 0;
    transform: translateX(40px);
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  `;

  if (document.documentElement.getAttribute('data-theme') === 'dark') {
    toastEl.style.background = '#1e293b';
    toastEl.style.color = '#f8fafc';
    toastEl.style.boxShadow = '0 10px 30px rgba(0,0,0,0.5), 0 2px 8px rgba(0,0,0,0.3)';
  }

  const iconClass = icons[type] || icons.info;
  const mainTitle = title ? `<div style="font-weight: 700; font-size: 0.95rem; margin-bottom: 2px;">${title}</div>` : '';
  const bodyText = message ? `<div style="font-size: 0.88rem; line-height: 1.4; opacity: 0.9;">${message}</div>` : '';

  toastEl.innerHTML = `
    <div style="font-size: 1.3rem; line-height: 1; flex-shrink: 0; padding-top: 2px;">
      <i class="${iconClass}"></i>
    </div>
    <div style="flex-grow: 1;">
      ${mainTitle}
      ${bodyText}
    </div>
    <button type="button" aria-label="Đóng" style="
      background: none;
      border: none;
      padding: 0 4px;
      font-size: 1.2rem;
      color: inherit;
      opacity: 0.5;
      cursor: pointer;
      line-height: 1;
    ">&times;</button>
  `;

  stack.appendChild(toastEl);

  requestAnimationFrame(() => {
    toastEl.style.opacity = '1';
    toastEl.style.transform = 'translateX(0)';
  });

  const closeToast = () => {
    toastEl.style.opacity = '0';
    toastEl.style.transform = 'translateX(40px)';
    setTimeout(() => toastEl.remove(), 300);
  };

  toastEl.querySelector('button').addEventListener('click', closeToast);

  const autoDismiss = setTimeout(closeToast, duration);
  toastEl.addEventListener('mouseenter', () => clearTimeout(autoDismiss));
}

/** Shows modern toast notification across all environments. */
function notify(title, message, type = 'info', duration = 3500) {
  showAppToast(title, message, type, duration);
}

/** Extracts JWT token from localStorage or cookie. */
function getAuthToken() {
  const token = localStorage.getItem('technova_jwt');
  if (token) return token;
  const match = document.cookie.match(/(?:^|;\s*)JWT_TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/** Completely clears all auth tokens, user info and auth cookies. */
function clearAuthSession() {
  try {
    localStorage.removeItem('technova_jwt');
    localStorage.removeItem('technova_role');
    localStorage.removeItem('technova_user');
    localStorage.removeItem('technova_last_active');
    localStorage.removeItem('technova_last_ping');
    localStorage.removeItem('technova_remember_me');
    if (typeof storageSet === 'function' && typeof STORE !== 'undefined') {
      storageSet(STORE.user, null);
    }
    document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'REFRESH_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
  } catch (e) {
    /* storage error */
  }
}

/** Clears all validation states inside a container or form. */
function clearAllErrors(form) {
  if (!form) return;
  form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
  form.querySelectorAll('.is-valid').forEach(el => el.classList.remove('is-valid'));
  form.querySelectorAll('.invalid-feedback').forEach(el => {
    el.textContent = '';
    el.style.display = 'none';
  });
  form.querySelectorAll('#alertBox, .alert, .dynamic-alert').forEach(el => el.remove());
}

/**
 * DTO-DRIVEN ERROR BINDER:
 * Takes the validation errors map from Spring Boot DTO (@Valid -> GlobalExceptionHandler)
 * and binds each error directly under its corresponding input field.
 */
function bindServerErrors(form, errorData, defaultMsg) {
  if (!form) return;
  clearAllErrors(form);

  let hasFieldError = false;
  let fieldErrorCount = 0;
  let singleFieldErrorMessage = '';

  if (errorData && typeof errorData === 'object' && Object.keys(errorData).length > 0) {
    // Map DTO field names to HTML input IDs for each form
    const fieldMapping = {
      fullName: ['rgName', 'fullName'],
      email: ['rgEmail', 'liEmail', 'fpEmail', 'otpEmail', 'email'],
      phone: ['rgPhone', 'phone', 'mobileNumber'],
      mobileNumber: ['rgPhone', 'mobileNumber'],
      password: ['rgPassword', 'liPassword', 'password'],
      confirmPassword: ['rgConfirm', 'confirmPassword', 'otpConfirmPassword'],
      oldPassword: ['oldPassword'],
      newPassword: ['newPassword', 'otpNewPassword'],
      confirmNewPassword: ['confirmNewPassword'],
      otp: ['otpCode', 'otp']
    };

    for (const [field, message] of Object.entries(errorData)) {
      const candidateIds = fieldMapping[field] || [field];
      let targetInput = null;

      for (const id of candidateIds) {
        const el = form.querySelector('#' + id);
        if (el) {
          targetInput = el;
          break;
        }
      }

      if (!targetInput) {
        targetInput = form.querySelector(`[name="${field}"]`) || document.getElementById(field);
      }

      if (targetInput) {
        hasFieldError = true;
        fieldErrorCount++;
        singleFieldErrorMessage = message;
        targetInput.classList.add('is-invalid');
        targetInput.setAttribute('aria-invalid', 'true');

        // Look for corresponding invalid-feedback container
        const parent = targetInput.closest('.mb-3, .mb-4') || targetInput.parentElement;
        const feedback = parent?.querySelector('.invalid-feedback');
        if (feedback) {
          feedback.textContent = message;
          feedback.style.display = 'block';
        }
      }
    }
  }

  // Smart resolution for backend business exceptions (e.g. RuntimeException("Current password is incorrect!"))
  const rawMsg = defaultMsg || 'Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại thông tin.';
  const lowerMsg = rawMsg.toLowerCase();

  if (!hasFieldError) {
    let target = null;
    if (lowerMsg.includes('current password') || lowerMsg.includes('old password') || lowerMsg.includes('mật khẩu hiện tại') || lowerMsg.includes('mật khẩu cũ')) {
      target = form.querySelector('#oldPassword');
    } else if (lowerMsg.includes('confirm') || lowerMsg.includes('not match') || lowerMsg.includes('không khớp')) {
      target = form.querySelector('#confirmNewPassword') || form.querySelector('#rgConfirm') || form.querySelector('#otpConfirmPassword');
    } else if (lowerMsg.includes('otp') || lowerMsg.includes('code')) {
      target = form.querySelector('#otpCode');
    } else if (lowerMsg.includes('email')) {
      target = form.querySelector('#liEmail') || form.querySelector('#rgEmail') || form.querySelector('#fpEmail') || form.querySelector('#otpEmail');
    } else if (lowerMsg.includes('phone') || lowerMsg.includes('số điện thoại')) {
      target = form.querySelector('#rgPhone');
    }

    if (target) {
      hasFieldError = true;
      target.classList.add('is-invalid');
      target.setAttribute('aria-invalid', 'true');
      const parent = target.closest('.mb-3, .mb-4') || target.parentElement;
      const feedback = parent?.querySelector('.invalid-feedback');
      if (feedback) {
        feedback.textContent = rawMsg;
        feedback.style.display = 'block';
      }
    }
  }

  // Display clear, logical Toast notification:
  if (fieldErrorCount >= 2) {
    // Multiple fields invalid -> Give comprehensive guidance
    notify(UiMessage.VALIDATION_ERROR_TITLE, UiMessage.VALIDATION_CHECK_FIELDS, 'danger');
  } else if (fieldErrorCount === 1) {
    // Single field invalid -> Report that specific field's error
    notify(UiMessage.VALIDATION_ERROR_TITLE, singleFieldErrorMessage, 'danger');
  } else {
    // Specific business error (e.g. "Current password is incorrect!", "Email hoặc mật khẩu không hợp lệ.")
    notify(UiMessage.NOTICE_TITLE, rawMsg, 'danger');
  }
}

/** Password strength calculator for UI indicator. */
function scorePassword(password) {
  let score = 0;
  if (!password) return { score: 0, label: 'Rất yếu', colour: '#e03131', width: '0%' };
  if (password.length >= 6) score++;
  if (password.length >= 8) score++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (score > 4) score = 4;

  const levels = [
    { label: 'Rất yếu', colour: '#e03131', width: '20%' },
    { label: 'Yếu', colour: '#f76707', width: '40%' },
    { label: 'Trung bình', colour: '#f59f00', width: '60%' },
    { label: 'Mạnh', colour: '#40c057', width: '80%' },
    { label: 'Rất mạnh', colour: '#12b886', width: '100%' }
  ];
  return { score, ...levels[score] };
}

let pendingActivationEmail = '';

function openActivationModal(email) {
  pendingActivationEmail = email;
  const emailEl = document.getElementById('activationModalEmail');
  if (emailEl) emailEl.textContent = email;

  const modalEl = document.getElementById('activationModal');
  if (modalEl && window.bootstrap?.Modal) {
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();
  }
}

async function handleModalVerifyNow() {
  if (!pendingActivationEmail) return;

  const btn = document.getElementById('btnModalVerifyNow');
  const originalHtml = btn ? btn.innerHTML : '';
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang gửi mã OTP…';
  }

  try {
    const res = await fetch('/auth/resend-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: pendingActivationEmail, type: 'REGISTER' })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      notify('OTP Sent', 'A verification code has been sent to your email.', 'success');
      setTimeout(() => {
        location.href = '/verify-otp?email=' + encodeURIComponent(pendingActivationEmail) + '&type=REGISTER';
      }, 700);
    } else {
      notify('Thất bại', data.message || 'Không thể gửi mã xác minh.', 'danger');
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
      }
    }
  } catch (err) {
    notify('Lỗi kết nối', 'Không thể kết nối máy chủ.', 'danger');
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = originalHtml;
    }
  }
}

/* ==========================================================================
   2.0 GOOGLE SIGN-IN (/auth/google) - Preserves 100% Original Button UI
   ========================================================================== */
async function fetchGoogleClientId() {
  if (window.__GOOGLE_CLIENT_ID__) return window.__GOOGLE_CLIENT_ID__;
  try {
    const res = await fetch('/auth/google/client-id');
    if (res.ok) {
      const data = await res.json();
      const clientId = data?.data?.trim();
      if (clientId && clientId !== 'your_google_client_id_here') {
        window.__GOOGLE_CLIENT_ID__ = clientId;
        return clientId;
      }
    }
  } catch (err) {
    console.warn('Could not load Google Client ID from backend:', err);
  }
  return null;
}

async function handleGoogleLoginPayload(payload) {
  notify('Đang xác minh với Google...', 'Vui lòng chờ trong khi xác thực tài khoản.', 'info', 2500);

  try {
    const res = await fetch('/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (res.ok && data.success) {
      if (data.data?.token) {
        localStorage.setItem('technova_jwt', data.data.token);
      }
      if (data.data?.user?.role) {
        localStorage.setItem('technova_role', data.data.user.role);
      }
      localStorage.setItem('technova_remember_me', 'true');
      localStorage.setItem('technova_last_active', Date.now().toString());

      const googleUser = {
        name: data.data?.user?.fullName || 'User',
        email: data.data?.user?.email || '',
        role: data.data?.user?.role,
        avatar: data.data?.user?.avatar || null,
        hasPassword: data.data?.user?.hasPassword ?? false,
        loggedInAt: new Date().toISOString()
      };
      try { localStorage.setItem('technova_user', JSON.stringify(googleUser)); } catch (_) {}
      if (typeof storageSet === 'function' && typeof STORE !== 'undefined') {
        storageSet(STORE.user, googleUser);
      }

      notify('Đăng nhập thành công', `Chào mừng ${data.data?.user?.fullName || 'bạn'} quay lại!`, 'success');
      setTimeout(() => {
        location.href = data.data?.redirectUrl || '/customer/index';
      }, 900);
    } else {
      notify('Đăng nhập Google thất bại', data.message || 'Không thể hoàn tất xác thực bằng Google.', 'danger');
    }
  } catch (err) {
    notify('Lỗi kết nối', 'Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng.', 'danger');
  }
}

async function initGoogleAuth() {
  const btnGoogle = document.getElementById('btnGoogleLogin');
  if (!btnGoogle) return;

  // Pre-fetch Google Client ID in background
  fetchGoogleClientId();

  // Attach click listener to the original UI button
  btnGoogle.addEventListener('click', async (e) => {
    e.preventDefault();

    const clientId = window.__GOOGLE_CLIENT_ID__ || await fetchGoogleClientId();
    if (!clientId || clientId === 'your_google_client_id_here') {
      notify('Thông báo đăng nhập Google', 'Google Client ID chưa được cấu hình. Vui lòng thêm GOOGLE_CLIENT_ID vào biến môi trường (.env).', 'warning', 5000);
      return;
    }

    if (typeof window.google === 'undefined' || !window.google.accounts || !window.google.accounts.oauth2) {
      notify('Đang khởi tạo Google SDK', 'Dịch vụ xác thực Google đang khởi tạo. Vui lòng thử lại sau ít phút.', 'warning', 4000);
      return;
    }

    // Initialize Google OAuth2 Token Client to open official Google popup
    if (!window.__GOOGLE_TOKEN_CLIENT__) {
      window.__GOOGLE_TOKEN_CLIENT__ = window.google.accounts.oauth2.initTokenClient({
        client_id: clientId,
        scope: 'email profile openid',
        callback: async (tokenResponse) => {
          if (tokenResponse.error) {
            console.error('Google OAuth error:', tokenResponse);
            notify('Google Sign-In Cancelled', 'Google sign-in was cancelled or closed.', 'warning');
            return;
          }
          if (tokenResponse.access_token) {
            await handleGoogleLoginPayload({ accessToken: tokenResponse.access_token });
          }
        }
      });
    }

    // Trigger official Google account chooser popup
    window.__GOOGLE_TOKEN_CLIENT__.requestAccessToken();
  });
}




/* ==========================================================================
   2. FORM: SIGN IN (/auth/login)
   ========================================================================== */
async function handleLogin(e) {
  e.preventDefault();
  const form = document.getElementById('loginForm');
  clearAllErrors(form);

  const email = document.getElementById('liEmail')?.value.trim() || '';
  const password = document.getElementById('liPassword')?.value || '';
  const rememberMe = document.getElementById('rememberMe')?.checked || false;
  const btn = document.getElementById('loginSubmit');
  const originalHtml = btn.innerHTML;

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang đăng nhập…';

  try {
    const res = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, rememberMe })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      if (data.data?.token) {
        localStorage.setItem('technova_jwt', data.data.token);
      }
      if (data.data?.user?.role) {
        localStorage.setItem('technova_role', data.data.user.role);
      }
      if (rememberMe) {
        localStorage.setItem('technova_remember_me', 'true');
      } else {
        localStorage.removeItem('technova_remember_me');
      }
      localStorage.setItem('technova_last_active', Date.now().toString());

      const loginUser = {
        name: data.data?.user?.fullName || 'User',
        email: data.data?.user?.email || email,
        role: data.data?.user?.role,
        avatar: data.data?.user?.avatar || null,
        hasPassword: data.data?.user?.hasPassword ?? true,
        loggedInAt: new Date().toISOString()
      };
      try { localStorage.setItem('technova_user', JSON.stringify(loginUser)); } catch (_) {}
      if (typeof storageSet === 'function' && typeof STORE !== 'undefined') {
        storageSet(STORE.user, loginUser);
      }

      notify(UiMessage.SIGN_IN_SUCCESS_TITLE, UiMessage.WELCOME_BACK(data.data?.user?.fullName || ''), 'success');
      setTimeout(() => {
        location.href = data.data?.redirectUrl || '/customer/index';
      }, 900);
    } else {
      if (data.message === 'ACCOUNT_NOT_ACTIVATED' || data.data?.pendingActivation) {
        openActivationModal(data.data?.email || email);
        return;
      }
      bindServerErrors(form, data.data, data.message || 'Email hoặc mật khẩu không hợp lệ.');
    }
  } catch (err) {
    notify(UiMessage.CONNECTION_ERROR_TITLE, UiMessage.CONNECTION_ERROR, 'danger');
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalHtml;
  }
}

/* ==========================================================================
   3. FORM: REGISTER (/auth/register)
   ========================================================================== */
async function handleRegister(e) {
  e.preventDefault();
  const form = document.getElementById('registerForm');
  clearAllErrors(form);

  const fullName = document.getElementById('rgName')?.value.trim() || '';
  const email = document.getElementById('rgEmail')?.value.trim() || '';
  const phone = document.getElementById('rgPhone')?.value.trim() || '';
  const password = document.getElementById('rgPassword')?.value || '';
  const confirmPassword = document.getElementById('rgConfirm')?.value || '';
  const terms = document.getElementById('rgTerms')?.checked;

  if (document.getElementById('rgTerms') && !terms) {
    const feedback = document.getElementById('errRgTerms');
    if (feedback) {
      feedback.textContent = 'Bạn cần đồng ý với Điều khoản dịch vụ để tiếp tục.';
      feedback.style.display = 'block';
    }
    return;
  }

  const btn = document.getElementById('registerSubmit');
  const originalHtml = btn.innerHTML;

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang tạo tài khoản…';

  try {
    const res = await fetch('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, phone, password, confirmPassword })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      notify(UiMessage.REGISTRATION_SUCCESS_TITLE, data.message || UiMessage.OTP_SENT, 'success', 3500);
      setTimeout(() => {
        location.href = '/verify-otp?email=' + encodeURIComponent(email) + '&type=REGISTER';
      }, 1200);
    } else {
      if (data.message === 'ACCOUNT_NOT_ACTIVATED' || data.data?.pendingActivation) {
        openActivationModal(data.data?.email || email);
        return;
      }
      bindServerErrors(form, data.data, data.message || 'Đăng ký không thành công.');
    }
  } catch (err) {
    notify(UiMessage.CONNECTION_ERROR_TITLE, UiMessage.CONNECTION_ERROR, 'danger');
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalHtml;
  }
}

/* ==========================================================================
   4. FORM: FORGOT PASSWORD (/auth/forgot-password)
   ========================================================================== */
async function handleForgotPassword(e) {
  e.preventDefault();
  const form = document.getElementById('forgotForm');
  clearAllErrors(form);

  const email = document.getElementById('fpEmail')?.value.trim() || '';
  const btn = document.getElementById('fpSubmit');
  const originalHtml = btn.innerHTML;

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang gửi mã OTP…';

  try {
    const res = await fetch('/auth/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      notify(UiMessage.SUCCESS_TITLE, data.message || UiMessage.OTP_SENT_ALT, 'success');
      setTimeout(() => {
        location.href = '/verify-otp?email=' + encodeURIComponent(email) + '&type=FORGOT_PASSWORD';
      }, 1200);
    } else {
      bindServerErrors(form, data.data, data.message || 'Không tìm thấy tài khoản với email này.');
    }
  } catch (err) {
    notify(UiMessage.CONNECTION_ERROR_TITLE, UiMessage.CONNECTION_ERROR_RETRY, 'danger');
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalHtml;
  }
}

/* ==========================================================================
   5. FORM: VERIFY OTP (/auth/verify-otp) & 5-MINUTE COUNTDOWN + RESEND
   ========================================================================== */
let otpTimerInterval = null;

function startOtpCountdown(durationSeconds = 300) {
  const timerText = document.getElementById('otpTimerText');
  const resendBtn = document.getElementById('btnResendOtp');
  if (!timerText) return;

  if (otpTimerInterval) {
    clearInterval(otpTimerInterval);
  }

  let remaining = durationSeconds;
  if (resendBtn) resendBtn.disabled = true;

  function updateDisplay() {
    const minutes = Math.floor(remaining / 60);
    const seconds = remaining % 60;
    const formatted = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    timerText.textContent = formatted;

    if (remaining <= 0) {
      clearInterval(otpTimerInterval);
      timerText.textContent = 'Đã hết hạn';
      if (resendBtn) {
        resendBtn.disabled = false;
      }
      notify(UiMessage.OTP_EXPIRED_TITLE, UiMessage.OTP_EXPIRED, 'warning');
    }
    remaining--;
  }

  updateDisplay();
  otpTimerInterval = setInterval(updateDisplay, 1000);
}

async function handleResendOtp() {
  const urlParams = new URLSearchParams(window.location.search);
  const email = document.getElementById('otpEmail')?.value.trim() || urlParams.get('email') || '';
  const type = urlParams.get('type') || 'REGISTER';
  const resendBtn = document.getElementById('btnResendOtp');

  if (!email) {
    notify(UiMessage.ERROR_TITLE, UiMessage.RESEND_EMAIL_MISSING, 'danger');
    return;
  }

  const originalText = resendBtn ? resendBtn.innerHTML : '';
  if (resendBtn) {
    resendBtn.disabled = true;
    resendBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang gửi…';
  }

  try {
    const res = await fetch('/auth/resend-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, type })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      notify(UiMessage.SUCCESS_TITLE, data.message || UiMessage.OTP_RESENT, 'success');
      startOtpCountdown(300);
    } else {
      notify(UiMessage.FAILED_TITLE, data.message || UiMessage.OTP_RESEND_FAILED, 'danger');
      if (resendBtn) resendBtn.disabled = false;
    }
  } catch (err) {
    notify(UiMessage.CONNECTION_ERROR_TITLE, UiMessage.CONNECTION_ERROR_RESEND, 'danger');
    if (resendBtn) resendBtn.disabled = false;
  } finally {
    if (resendBtn) resendBtn.innerHTML = originalText;
  }
}

async function handleVerifyOtp(e) {
  e.preventDefault();
  const form = document.getElementById('otpForm');
  clearAllErrors(form);

  const urlParams = new URLSearchParams(window.location.search);
  const typeParam = urlParams.get('type') || 'REGISTER';
  const email = document.getElementById('otpEmail')?.value.trim() || urlParams.get('email') || '';
  const otp = document.getElementById('otpCode')?.value.trim() || '';
  const btn = document.getElementById('otpSubmit');
  const originalHtml = btn.innerHTML;

  let payload = {
    email: email,
    otp: otp,
    type: typeParam
  };

  if (typeParam === 'FORGOT_PASSWORD') {
    payload.newPassword = document.getElementById('otpNewPassword')?.value || '';
    payload.confirmPassword = document.getElementById('otpConfirmPassword')?.value || '';
  }

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xác minh…';

  try {
    const res = await fetch('/auth/verify-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (res.ok && data.success) {
      if (otpTimerInterval) clearInterval(otpTimerInterval);
      notify(UiMessage.SUCCESS_TITLE, data.message || UiMessage.OTP_VERIFIED, 'success');
      setTimeout(() => {
        location.href = '/login';
      }, 1400);
    } else {
      bindServerErrors(form, data.data, data.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.');
    }
  } catch (err) {
    notify(UiMessage.CONNECTION_ERROR_TITLE, UiMessage.CONNECTION_ERROR, 'danger');
  } finally {
    btn.disabled = false;
    btn.innerHTML = originalHtml;
  }
}

/* ==========================================================================
   6. FORMS: CHANGE PASSWORD & SET PASSWORD (/customer/change-password & /customer/set-password)
   ========================================================================== */
async function handleChangePassword(e) {
  e.preventDefault();
  const form = e.target;
  clearAllErrors(form);

  const oldPassword = form.querySelector('#oldPassword')?.value || '';
  const newPassword = form.querySelector('#newPassword')?.value || '';
  const confirmNewPassword = form.querySelector('#confirmNewPassword')?.value || '';
  const btn = form.querySelector('button[type="submit"]');
  const originalHtml = btn ? btn.innerHTML : '';

  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu thay đổi…';
  }

  try {
    const token = getAuthToken();
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch('/auth/reset-password', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ oldPassword, newPassword, confirmNewPassword })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      notify(UiMessage.SUCCESS_TITLE, data.message || UiMessage.PASSWORD_CHANGED, 'success');
      form.reset();
    } else {
      bindServerErrors(form, data.data, data.message || 'Không thể đổi mật khẩu.');
    }
  } catch (err) {
    notify(UiMessage.CONNECTION_ERROR_TITLE, UiMessage.CONNECTION_ERROR, 'danger');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = originalHtml;
    }
  }
}

async function handleSetPassword(e) {
  e.preventDefault();
  const form = e.target;
  clearAllErrors(form);

  const newPassword = form.querySelector('#newPassword')?.value || '';
  const confirmNewPassword = form.querySelector('#confirmNewPassword')?.value || '';
  const btn = form.querySelector('button[type="submit"]');
  const originalHtml = btn ? btn.innerHTML : '';

  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang tạo mật khẩu…';
  }

  try {
    const token = getAuthToken();
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch('/auth/set-password', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ newPassword, confirmNewPassword })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      notify('Thành công', data.message || 'Tạo mật khẩu thành công. Đang chuyển trang...', 'success');
      form.reset();

      // Update cached user state
      if (typeof storageGet === 'function' && typeof storageSet === 'function' && typeof STORE !== 'undefined') {
        const user = storageGet(STORE.user, null);
        if (user) {
          user.hasPassword = true;
          storageSet(STORE.user, user);
        }
      }

      // Update navbar links immediately
      if (typeof initAccountLabel === 'function') {
        initAccountLabel();
      }

      // Smoothly redirect to customer-index URL
      setTimeout(() => {
        location.href = '/customer/index';
      }, 1000);
    } else {
      bindServerErrors(form, data.data, data.message || 'Không thể tạo mật khẩu.');
    }
  } catch (err) {
    notify('Lỗi kết nối', 'Không thể kết nối máy chủ.', 'danger');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = originalHtml;
    }
  }
}

/* ==========================================================================
   7. INITIALIZATION (DOM CONTENT LOADED)
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

  // --- Auto-clean session if redirected from /login?logout=true ---
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('logout') === 'true') {
    clearAuthSession();
  }

  // --- Complete Logout Handlers across all views ---
  document.querySelectorAll('form[action*="/logout"], .js-logout-form').forEach(form => {
    form.addEventListener('submit', () => {
      clearAuthSession();
    });
  });

  // --- Dynamic Tab Sync for /login and /register ---
  const loginTabBtn = document.querySelector('button[data-bs-target="#pane-login"]');
  const registerTabBtn = document.querySelector('button[data-bs-target="#pane-register"]');

  function switchTabByPath() {
    if (window.location.pathname === '/register' || urlParams.get('tab') === 'register') {
      if (registerTabBtn) {
        if (window.bootstrap?.Tab) {
          bootstrap.Tab.getOrCreateInstance(registerTabBtn).show();
        } else {
          registerTabBtn.click();
        }
        document.title = 'Tạo tài khoản — UTE Shop';
      }
    } else if (window.location.pathname === '/login') {
      if (loginTabBtn) {
        if (window.bootstrap?.Tab) {
          bootstrap.Tab.getOrCreateInstance(loginTabBtn).show();
        } else {
          loginTabBtn.click();
        }
        document.title = 'Đăng nhập — UTE Shop';
      }
    }
  }

  switchTabByPath();

  registerTabBtn?.addEventListener('shown.bs.tab', () => {
    if (window.location.pathname !== '/register') {
      history.pushState(null, '', '/register');
      document.title = 'Tạo tài khoản — UTE Shop';
    }
  });

  loginTabBtn?.addEventListener('shown.bs.tab', () => {
    if (window.location.pathname !== '/login') {
      history.pushState(null, '', '/login');
      document.title = 'Đăng nhập — UTE Shop';
    }
  });

  window.addEventListener('popstate', switchTabByPath);

  // --- Attach Form Submit Listeners ---
  document.getElementById('loginForm')?.addEventListener('submit', handleLogin);
  document.getElementById('registerForm')?.addEventListener('submit', handleRegister);
  document.getElementById('forgotForm')?.addEventListener('submit', handleForgotPassword);
  document.getElementById('otpForm')?.addEventListener('submit', handleVerifyOtp);
  document.getElementById('resetPassForm')?.addEventListener('submit', handleChangePassword);
  document.getElementById('setPassForm')?.addEventListener('submit', handleSetPassword);

  // --- Resend OTP Button & Modal Verify Now ---
  document.getElementById('btnResendOtp')?.addEventListener('click', handleResendOtp);
  document.getElementById('btnModalVerifyNow')?.addEventListener('click', handleModalVerifyNow);

  // --- Toggle Show / Hide Password ---
  document.querySelectorAll('.toggle-pass').forEach(btn => {
    btn.addEventListener('click', () => {
      const target = document.getElementById(btn.dataset.target);
      if (!target) return;
      const showing = target.type === 'text';
      target.type = showing ? 'password' : 'text';
      const icon = btn.querySelector('i');
      if (icon) {
        icon.className = showing
          ? (icon.className.includes('fa-solid') ? 'fa-solid fa-eye' : 'fas fa-eye')
          : (icon.className.includes('fa-solid') ? 'fa-solid fa-eye-slash' : 'fas fa-eye-slash');
      }
    });
  });

  // --- Password Strength Meter ---
  const rgPass = document.getElementById('rgPassword');
  const strengthBar = document.getElementById('strengthBar');
  const strengthText = document.getElementById('strengthText');
  if (rgPass && strengthBar && strengthText) {
    rgPass.addEventListener('input', () => {
      const val = rgPass.value;
      if (!val) {
        strengthBar.style.width = '0';
        strengthText.textContent = 'Mật khẩu cần có ít nhất 6 ký tự, bao gồm chữ và số.';
        strengthText.style.color = '';
        return;
      }
      const { label, colour, width } = scorePassword(val);
      strengthBar.style.width = width;
      strengthBar.style.background = colour;
      strengthText.textContent = `Độ mạnh mật khẩu: ${label}`;
      strengthText.style.color = colour;
    });
  }

  // --- Auto-clear field error on typing ---
  document.querySelectorAll('form input').forEach(input => {
    input.addEventListener('input', () => {
      if (input.classList.contains('is-invalid')) {
        input.classList.remove('is-invalid');
        input.removeAttribute('aria-invalid');
        const parent = input.closest('.mb-3, .mb-4') || input.parentElement;
        const box = parent?.querySelector('.invalid-feedback');
        if (box) box.style.display = 'none';
      }
    });
  });

  // --- Initialize verify-otp page view if present ---
  const otpForm = document.getElementById('otpForm');
  if (otpForm) {
    const emailParam = urlParams.get('email') || '';
    const typeParam = urlParams.get('type') || 'REGISTER';

    const hiddenEmail = document.getElementById('otpEmail');
    if (hiddenEmail) hiddenEmail.value = emailParam;

    const title = document.getElementById('otpTitle');
    const subtitle = document.getElementById('otpSubtitle');
    const forgotFields = document.getElementById('forgotPasswordFields');

    if (typeParam === 'FORGOT_PASSWORD') {
      if (title) title.textContent = 'Đặt lại mật khẩu';
      if (subtitle) {
        subtitle.innerHTML = emailParam
          ? `Mã OTP đã được gửi đến: <strong class="text-dark">${emailParam}</strong>. Nhập mã OTP và mật khẩu mới để khôi phục tài khoản.`
          : 'Nhập mã OTP và mật khẩu mới để khôi phục tài khoản.';
      }
      if (forgotFields) forgotFields.style.display = 'block';
    } else {
      if (title) title.textContent = 'Kích hoạt tài khoản';
      if (subtitle) {
        subtitle.innerHTML = emailParam
          ? `Mã OTP đã được gửi đến: <strong class="text-dark">${emailParam}</strong>. Nhập mã OTP để kích hoạt tài khoản.`
          : 'Nhập mã OTP để kích hoạt tài khoản.';
      }
      if (forgotFields) forgotFields.style.display = 'none';
    }

    // Start 5-minute (300 seconds) countdown timer
    startOtpCountdown(300);
  }

  // --- Initialize Google Sign-In ---
  initGoogleAuth();
});


