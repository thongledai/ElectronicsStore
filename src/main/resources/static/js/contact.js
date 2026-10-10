/* ==========================================================================
   TechNova Electronics — Contact Form Validation
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
    if (!trimmed) return 'Please enter your name.';
    if (trimmed.length < 3) return 'Name must be at least 3 characters long.';
    if (trimmed.length > 60) return 'Name cannot exceed 60 characters.';
    if (!/^[A-Za-z][A-Za-z\s.'-]*$/.test(trimmed)) {
      return 'Name can only contain letters, spaces, apostrophes, hyphens and full stops.';
    }
    return '';
  },

  email(value) {
    const trimmed = value.trim();
    if (!trimmed) return 'Please enter your email address.';
    // Standard "something@something.tld" check
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(trimmed)) {
      return 'Enter a valid email address, for example name@example.com.';
    }
    if (trimmed.length > 254) return 'That email address is too long.';
    return '';
  },

  phone(value) {
    const trimmed = value.trim();
    if (!trimmed) return '';                       // phone is optional
    if (!/^\d{10}$/.test(trimmed)) return 'Enter a 10-digit mobile number without spaces.';
    if (!/^[6-9]/.test(trimmed)) return 'Indian mobile numbers start with 6, 7, 8 or 9.';
    return '';
  },

  subject(value) {
    if (!value) return 'Please choose what your message is about.';
    return '';
  },

  message(value) {
    const trimmed = value.trim();
    if (!trimmed) return 'Please write your message.';
    if (trimmed.length < 15) return 'Please give us a bit more detail (at least 15 characters).';
    if (trimmed.length > 600) return 'Message cannot exceed 600 characters.';
    return '';
  },

  consent(checked) {
    if (!checked) return 'Please tick the box so we can reply to you.';
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
      'Please check the form',
      `${count} field${count === 1 ? '' : 's'} need${count === 1 ? 's' : ''} your attention.`,
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
  submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Sending…';

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
