/* ==========================================================================
   UTE Shop — Contact Form Validation
   --------------------------------------------------------------------------
   Pure JavaScript validation (no backend). Each field has its own rule, is
   checked live once the user leaves it, and is re-checked on submit.
   ========================================================================== */

'use strict';

/* ==========================================================================
   1. VALIDATION RULES
   --------------------------------------------------------------------------
   Each rule returns an error string, or '' when the value is acceptable.
   ========================================================================== */
const RULES = {

  name(value) {
    const trimmed = value.trim();
    if (!trimmed) return 'Vui lòng nhập họ và tên.';
    if (trimmed.length < 3) return 'Họ và tên phải có ít nhất 3 ký tự.';
    if (trimmed.length > 60) return 'Họ và tên không được vượt quá 60 ký tự.';
    if (!/^[A-Za-z][A-Za-z\s.'-]*$/.test(trimmed)) {
      return 'Tên chỉ được chứa chữ cái không dấu, khoảng trắng, dấu nháy đơn, dấu gạch ngang và dấu chấm.';
    }
    return '';
  },

  email(value) {
    const trimmed = value.trim();
    if (!trimmed) return 'Vui lòng nhập địa chỉ email.';
    // Standard "something@something.tld" check
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(trimmed)) {
      return 'Vui lòng nhập địa chỉ email hợp lệ, ví dụ ten@example.com.';
    }
    if (trimmed.length > 254) return 'Địa chỉ email không được vượt quá 254 ký tự.';
    return '';
  },

  phone(value) {
    const trimmed = value.trim();
    if (!trimmed) return '';                       // phone is optional
    if (!/^\d{10}$/.test(trimmed)) return 'Số điện thoại phải gồm 10 chữ số, không có khoảng trắng.';
    if (!/^[6-9]/.test(trimmed)) return 'Số điện thoại phải bắt đầu bằng 6, 7, 8 hoặc 9.';
    return '';
  },

  subject(value) {
    if (!value) return 'Vui lòng chọn chủ đề liên hệ.';
    return '';
  },

  message(value) {
    const trimmed = value.trim();
    if (!trimmed) return 'Vui lòng nhập nội dung tin nhắn.';
    if (trimmed.length < 15) return 'Nội dung cần có ít nhất 15 ký tự.';
    if (trimmed.length > 600) return 'Nội dung không được vượt quá 600 ký tự.';
    return '';
  },

  consent(checked) {
    if (!checked) return 'Vui lòng đồng ý để chúng tôi có thể phản hồi.';
    return '';
  }
};

/* Maps each field id to its rule and its error-message container */
const FIELDS = [
  { id: 'cfName',    rule: 'name',    error: 'errName' },
  { id: 'cfEmail',   rule: 'email',   error: 'errEmail' },
  { id: 'cfPhone',   rule: 'phone',   error: 'errPhone' },
  { id: 'cfSubject', rule: 'subject', error: 'errSubject' },
  { id: 'cfMessage', rule: 'message', error: 'errMessage' },
  { id: 'cfConsent', rule: 'consent', error: 'errConsent' }
];

/* ==========================================================================
   2. FIELD-LEVEL VALIDATION
   ========================================================================== */

/**
 * Validates one field and paints the valid/invalid state.
 * @returns {boolean} true when the field passes
 */
function validateField(field) {
  const input = document.getElementById(field.id);
  const errorBox = document.getElementById(field.error);
  if (!input) return true;

  const value = input.type === 'checkbox' ? input.checked : input.value;
  const error = RULES[field.rule](value);

  if (error) {
    input.classList.add('is-invalid');
    input.classList.remove('is-valid');
    input.setAttribute('aria-invalid', 'true');
    errorBox.textContent = error;
    errorBox.style.display = 'block';
    return false;
  }

  input.classList.remove('is-invalid');
  input.removeAttribute('aria-invalid');
  // Optional empty fields shouldn't show a green tick
  const isOptionalAndEmpty = field.rule === 'phone' && !input.value.trim();
  input.classList.toggle('is-valid', !isOptionalAndEmpty && input.type !== 'checkbox');
  errorBox.textContent = '';
  errorBox.style.display = 'none';
  return true;
}

/** Clears every validation state, used by the reset button. */
function clearValidation() {
  FIELDS.forEach(field => {
    const input = document.getElementById(field.id);
    const errorBox = document.getElementById(field.error);
    if (input) {
      input.classList.remove('is-invalid', 'is-valid');
      input.removeAttribute('aria-invalid');
    }
    if (errorBox) {
      errorBox.textContent = '';
      errorBox.style.display = 'none';
    }
  });
  document.getElementById('charCount').textContent = '0';
}

/* ==========================================================================
   3. FORM SUBMISSION
   ========================================================================== */
function handleSubmit(e) {
  e.preventDefault();

  // Validate every field and collect the failures
  const results = FIELDS.map(field => ({ field, valid: validateField(field) }));
  const firstInvalid = results.find(r => !r.valid);

  if (firstInvalid) {
    const count = results.filter(r => !r.valid).length;
    showToast(
      'Vui lòng kiểm tra biểu mẫu',
      `Có ${count} mục cần được kiểm tra lại.`,
      'danger'
    );

    // Focus and scroll to the first problem field
    const input = document.getElementById(firstInvalid.field.id);
    input.focus({ preventScroll: true });
    input.scrollIntoView({ behavior: 'smooth', block: 'center' });
    return;
  }

  // --- All valid: simulate a send ---------------------------------------
  const submitBtn = document.getElementById('cfSubmit');
  const originalHtml = submitBtn.innerHTML;
  submitBtn.disabled = true;
  submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang gửi…';

  // A short delay makes the demo feel like a real network request
  setTimeout(() => {
    const name = document.getElementById('cfName').value.trim();
    const email = document.getElementById('cfEmail').value.trim();
    const ref = 'TN-MSG-' + String(Date.now()).slice(-6);

    document.getElementById('successName').textContent = name.split(' ')[0];
    document.getElementById('successEmail').textContent = email;
    document.getElementById('successRef').textContent = ref;

    document.getElementById('contactForm').hidden = true;
    document.getElementById('contactSuccess').hidden = false;

    submitBtn.disabled = false;
    submitBtn.innerHTML = originalHtml;

    showToast(UiMessage.MESSAGE_SENT_TITLE, UiMessage.MESSAGE_SENT(name.split(' ')[0]), 'success', 4500);
  }, 900);
}

/* ==========================================================================
   4. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('contactForm');
  if (!form) return;

  form.addEventListener('submit', handleSubmit);

  FIELDS.forEach(field => {
    const input = document.getElementById(field.id);
    if (!input) return;

    // Validate when the user leaves the field
    input.addEventListener('blur', () => validateField(field));

    // Once a field is marked invalid, re-check as the user types so the
    // error clears the moment it is fixed
    const liveEvent = input.tagName === 'SELECT' || input.type === 'checkbox' ? 'change' : 'input';
    input.addEventListener(liveEvent, () => {
      if (input.classList.contains('is-invalid') || input.type === 'checkbox' || input.tagName === 'SELECT') {
        validateField(field);
      }
    });
  });

  // Phone: strip anything that isn't a digit as it's typed
  const phone = document.getElementById('cfPhone');
  phone.addEventListener('input', () => {
    phone.value = phone.value.replace(/\D/g, '').slice(0, 10);
  });

  // Live character counter on the message box
  const message = document.getElementById('cfMessage');
  const charCount = document.getElementById('charCount');
  message.addEventListener('input', () => {
    const length = message.value.length;
    charCount.textContent = length;
    charCount.parentElement.classList.toggle('text-danger', length > 600);
  });

  // Reset button clears the painted validation states too
  document.getElementById('cfReset').addEventListener('click', () => {
    setTimeout(clearValidation, 0);   // run after the native reset
    showToast(UiMessage.FORM_CLEARED_TITLE, UiMessage.FORM_CLEARED, 'info', 2000);
  });

  // "Send another message" returns to a blank form
  document.getElementById('sendAnotherBtn').addEventListener('click', () => {
    form.reset();
    clearValidation();
    document.getElementById('contactSuccess').hidden = true;
    form.hidden = false;
    document.getElementById('cfName').focus();
  });
});
