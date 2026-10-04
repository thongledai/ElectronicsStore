/* ==========================================================================
   TechNova Electronics — Demo Login & Registration
   --------------------------------------------------------------------------
   IMPORTANT: this is a frontend demonstration only. There is no server, no
   database and no real authentication. The "signed in" state is nothing more
   than a name stored in localStorage so the header can greet the user.
   Never use this pattern for a real login.
   ========================================================================== */

'use strict';

/* The one hard-coded account the demo accepts */
const DEMO_USER = {
  email: 'demo@technova.com',
  password: 'Demo@123',
  name: 'Demo User'
};

/* ==========================================================================
   1. SHARED VALIDATION HELPERS
   ========================================================================== */

/** Paints an error message under a field. */
function setError(inputId, errorId, message) {
  const input = document.getElementById(inputId);
  const errorBox = document.getElementById(errorId);

  input.classList.add('is-invalid');
  input.classList.remove('is-valid');
  input.setAttribute('aria-invalid', 'true');
  errorBox.textContent = message;
  errorBox.style.display = 'block';
}

/** Clears the error state on a field. */
function setValid(inputId, errorId, showTick = true) {
  const input = document.getElementById(inputId);
  const errorBox = document.getElementById(errorId);

  input.classList.remove('is-invalid');
  input.removeAttribute('aria-invalid');
  if (showTick && input.type !== 'checkbox') input.classList.add('is-valid');
  errorBox.textContent = '';
  errorBox.style.display = 'none';
}

const isValidEmail = value => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value.trim());

/* ==========================================================================
   2. SIGN IN
   ========================================================================== */
function validateLogin() {
  let valid = true;

  const email = document.getElementById('liEmail').value.trim();
  const password = document.getElementById('liPassword').value;

  // Email
  if (!email) {
    setError('liEmail', 'errLiEmail', 'Please enter your email address.');
    valid = false;
  } else if (!isValidEmail(email)) {
    setError('liEmail', 'errLiEmail', 'Enter a valid email address.');
    valid = false;
  } else {
    setValid('liEmail', 'errLiEmail');
  }

  // Password
  if (!password) {
    setError('liPassword', 'errLiPassword', 'Please enter your password.');
    valid = false;
  } else if (password.length < 6) {
    setError('liPassword', 'errLiPassword', 'Password must be at least 6 characters.');
    valid = false;
  } else {
    setValid('liPassword', 'errLiPassword');
  }

  return valid;
}

function handleLogin(e) {
  e.preventDefault();
  if (!validateLogin()) {
    showToast('Check your details', 'Please fix the highlighted fields.', 'danger');
    return;
  }

  const email = document.getElementById('liEmail').value.trim().toLowerCase();
  const password = document.getElementById('liPassword').value;
  const btn = document.getElementById('loginSubmit');
  const originalHtml = btn.innerHTML;

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Signing in…';

  // Simulated network delay
  setTimeout(() => {
    btn.disabled = false;
    btn.innerHTML = originalHtml;

    // Credential check against the single demo account
    if (email !== DEMO_USER.email || password !== DEMO_USER.password) {
      setError('liEmail', 'errLiEmail', '');
      setError('liPassword', 'errLiPassword',
        'Incorrect email or password. Use the demo credentials shown above.');
      showToast('Sign in failed', 'Those credentials do not match the demo account.', 'danger', 4000);
      return;
    }

    // "Log in" — store a name so the header can greet the user
    storageSet(STORE.user, {
      name: DEMO_USER.name,
      email: DEMO_USER.email,
      loggedInAt: new Date().toISOString()
    });

    showToast('Welcome back!', `Signed in as ${DEMO_USER.name}.`, 'success');
    setTimeout(() => { location.href = ROUTES.home; }, 1100);
  }, 850);
}

/* ==========================================================================
   3. REGISTRATION
   ========================================================================== */

/**
 * Scores a password from 0-4 and returns a label + colour for the meter.
 * Checks length, mixed case, digits and symbols.
 */
function scorePassword(password) {
  let score = 0;
  if (password.length >= 8) score++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (password.length >= 12 && score >= 3) score = 4;

  const levels = [
    { label: 'Too weak',   colour: '#e03131', width: '20%' },
    { label: 'Weak',       colour: '#f76707', width: '40%' },
    { label: 'Fair',       colour: '#f59f00', width: '60%' },
    { label: 'Strong',     colour: '#40c057', width: '80%' },
    { label: 'Very strong', colour: '#12b886', width: '100%' }
  ];

  return { score, ...levels[score] };
}

function validateRegister() {
  let valid = true;

  const name = document.getElementById('rgName').value.trim();
  const email = document.getElementById('rgEmail').value.trim();
  const phone = document.getElementById('rgPhone').value.trim();
  const password = document.getElementById('rgPassword').value;
  const confirm = document.getElementById('rgConfirm').value;
  const terms = document.getElementById('rgTerms').checked;

  // Name
  if (!name) {
    setError('rgName', 'errRgName', 'Please enter your full name.');
    valid = false;
  } else if (name.length < 3) {
    setError('rgName', 'errRgName', 'Name must be at least 3 characters.');
    valid = false;
  } else if (!/^[A-Za-z][A-Za-z\s.'-]*$/.test(name)) {
    setError('rgName', 'errRgName', 'Name can only contain letters, spaces, hyphens and apostrophes.');
    valid = false;
  } else {
    setValid('rgName', 'errRgName');
  }

  // Email
  if (!email) {
    setError('rgEmail', 'errRgEmail', 'Please enter your email address.');
    valid = false;
  } else if (!isValidEmail(email)) {
    setError('rgEmail', 'errRgEmail', 'Enter a valid email address.');
    valid = false;
  } else {
    setValid('rgEmail', 'errRgEmail');
  }

  // Phone
  if (!phone) {
    setError('rgPhone', 'errRgPhone', 'Please enter your mobile number.');
    valid = false;
  } else if (!/^\d{10}$/.test(phone)) {
    setError('rgPhone', 'errRgPhone', 'Enter a 10-digit mobile number.');
    valid = false;
  } else if (!/^[6-9]/.test(phone)) {
    setError('rgPhone', 'errRgPhone', 'Indian mobile numbers start with 6, 7, 8 or 9.');
    valid = false;
  } else {
    setValid('rgPhone', 'errRgPhone');
  }

  // Password
  if (!password) {
    setError('rgPassword', 'errRgPassword', 'Please create a password.');
    valid = false;
  } else if (password.length < 8) {
    setError('rgPassword', 'errRgPassword', 'Password must be at least 8 characters.');
    valid = false;
  } else if (scorePassword(password).score < 2) {
    setError('rgPassword', 'errRgPassword', 'Password is too weak — mix upper case, lower case, numbers and symbols.');
    valid = false;
  } else {
    setValid('rgPassword', 'errRgPassword');
  }

  // Confirm password
  if (!confirm) {
    setError('rgConfirm', 'errRgConfirm', 'Please confirm your password.');
    valid = false;
  } else if (confirm !== password) {
    setError('rgConfirm', 'errRgConfirm', 'Passwords do not match.');
    valid = false;
  } else {
    setValid('rgConfirm', 'errRgConfirm');
  }

  // Terms
  if (!terms) {
    setError('rgTerms', 'errRgTerms', 'You must accept the terms to create an account.');
    valid = false;
  } else {
    setValid('rgTerms', 'errRgTerms', false);
  }

  return valid;
}

function handleRegister(e) {
  e.preventDefault();
  if (!validateRegister()) {
    showToast('Check your details', 'Please fix the highlighted fields.', 'danger');
    return;
  }

  const name = document.getElementById('rgName').value.trim();
  const email = document.getElementById('rgEmail').value.trim();
  const btn = document.getElementById('registerSubmit');
  const originalHtml = btn.innerHTML;

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating account…';

  setTimeout(() => {
    btn.disabled = false;
    btn.innerHTML = originalHtml;

    // No server exists, so we just record the name locally
    storageSet(STORE.user, { name, email, loggedInAt: new Date().toISOString() });

    showToast('Account created', `Welcome to TechNova, ${name.split(' ')[0]}!`, 'success', 4000);
    setTimeout(() => { location.href = ROUTES.home; }, 1300);
  }, 900);
}

/* ==========================================================================
   4. INIT
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

  // --- Form submissions --------------------------------------------------
  document.getElementById('loginForm').addEventListener('submit', handleLogin);
  document.getElementById('registerForm').addEventListener('submit', handleRegister);

  // --- Show / hide password ----------------------------------------------
  document.querySelectorAll('.toggle-pass').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = document.getElementById(btn.dataset.target);
      const showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      btn.querySelector('i').className = showing ? 'fas fa-eye' : 'fas fa-eye-slash';
      btn.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
    });
  });

  // --- Auto-fill the demo credentials ------------------------------------
  document.getElementById('fillDemoBtn').addEventListener('click', () => {
    document.getElementById('liEmail').value = DEMO_USER.email;
    document.getElementById('liPassword').value = DEMO_USER.password;
    setValid('liEmail', 'errLiEmail');
    setValid('liPassword', 'errLiPassword');
    showToast('Credentials filled', 'Press Sign In to continue.', 'info', 2200);
  });

  // --- Password strength meter -------------------------------------------
  const passwordInput = document.getElementById('rgPassword');
  const strengthBar = document.getElementById('strengthBar');
  const strengthText = document.getElementById('strengthText');

  passwordInput.addEventListener('input', () => {
    const value = passwordInput.value;

    if (!value) {
      strengthBar.style.width = '0';
      strengthText.textContent = 'Use 8+ characters with upper case, lower case, a number and a symbol.';
      strengthText.style.color = '';
      return;
    }

    const { label, colour, width } = scorePassword(value);
    strengthBar.style.width = width;
    strengthBar.style.background = colour;
    strengthText.textContent = `Password strength: ${label}`;
    strengthText.style.color = colour;
  });

  // --- Phone: digits only -------------------------------------------------
  const phoneInput = document.getElementById('rgPhone');
  phoneInput.addEventListener('input', () => {
    phoneInput.value = phoneInput.value.replace(/\D/g, '').slice(0, 10);
  });

  // --- Clear the invalid state as the user corrects a field ---------------
  document.querySelectorAll('#loginForm input, #registerForm input').forEach(input => {
    input.addEventListener('input', () => {
      if (input.classList.contains('is-invalid')) {
        input.classList.remove('is-invalid');
        const errorBox = input.closest('.mb-3, .mb-4')?.querySelector('.invalid-feedback');
        if (errorBox) errorBox.style.display = 'none';
      }
    });
  });

  // --- Placeholder actions -----------------------------------------------
  document.getElementById('forgotLink').addEventListener('click', e => {
    e.preventDefault();
    showToast('Password reset', 'Password recovery needs a backend, so it is disabled in this demo.', 'info', 4000);
  });

  document.querySelectorAll('.js-social').forEach(btn => {
    btn.addEventListener('click', () => {
      showToast(
        `${btn.dataset.provider} sign-in`,
        'Social login requires a backend OAuth flow — not available in this frontend demo.',
        'info',
        4000
      );
    });
  });

  // --- Already signed in? Offer to continue or sign out -------------------
  const user = storageGet(STORE.user, null);
  if (user && user.name) {
    showToast('Already signed in', `You are logged in as ${user.name}.`, 'info', 3500);
    document.getElementById('liEmail').value = user.email || '';
  }
});
