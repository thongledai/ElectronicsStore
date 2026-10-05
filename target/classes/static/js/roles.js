/* ==========================================================================
   TechNova Electronics — Dashboard Script (Manager / Employee / Shipper)
   --------------------------------------------------------------------------
   Loaded by layouts/dashboard-layout.html. Static demo behaviour only:
     • Dark / light theme toggle (same "technova_theme" key as the storefront)
     • Table search            [data-search-table="#tableId"]
     • Demo buttons            [data-demo-action="Label"]
     • Demo status selects     [data-demo-status]
   Remove the demo helpers once the real controllers/endpoints exist.
   ========================================================================== */
'use strict';

(function () {
  const THEME_KEY = 'technova_theme';

  /* ---------- Toast ---------- */
  function toast(message) {
    let stack = document.querySelector('.role-toast-stack');
    if (!stack) {
      stack = document.createElement('div');
      stack.className = 'role-toast-stack';
      document.body.appendChild(stack);
    }
    const el = document.createElement('div');
    el.className = 'role-toast';
    el.textContent = message;
    stack.appendChild(el);
    setTimeout(() => el.classList.add('show'), 10);
    setTimeout(() => { el.classList.remove('show'); setTimeout(() => el.remove(), 300); }, 2400);
  }

  /* ---------- Theme ---------- */
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    try { localStorage.setItem(THEME_KEY, JSON.stringify(theme)); } catch (e) { /* storage unavailable */ }
    document.querySelectorAll('[data-theme-toggle] i').forEach(i => {
      i.className = theme === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    });
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'dark' ? '#0a1220' : '#0b1b34');
  }

  function initTheme() {
    applyTheme(document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light');
    document.querySelectorAll('[data-theme-toggle]').forEach(btn => {
      btn.addEventListener('click', () => {
        applyTheme(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
      });
    });
  }

  /* ---------- Table search ---------- */
  function initTableSearch() {
    document.querySelectorAll('[data-search-table]').forEach(input => {
      const table = document.querySelector(input.getAttribute('data-search-table'));
      if (!table) return;
      input.addEventListener('input', () => {
        const q = input.value.trim().toLowerCase();
        table.querySelectorAll('tbody tr').forEach(row => {
          row.hidden = q !== '' && !row.textContent.toLowerCase().includes(q);
        });
      });
    });
  }

  /* ---------- Demo actions ---------- */
  function initDemoActions() {
    document.addEventListener('click', e => {
      const btn = e.target.closest('[data-demo-action]');
      if (btn) toast(btn.getAttribute('data-demo-action') + ': demo feature — not connected to backend.');
    });
    document.querySelectorAll('[data-demo-status]').forEach(sel => {
      sel.addEventListener('change', () => {
        toast('Selected "' + sel.options[sel.selectedIndex].text + '" (demo — not saved to database).');
      });
    });
  }

  /* ---------- Complete Logout Handler ---------- */
  function initLogoutHandler() {
    document.querySelectorAll('form[action*="/logout"], .js-logout-form').forEach(form => {
      form.addEventListener('submit', () => {
        try {
          localStorage.removeItem('technova_jwt');
          localStorage.removeItem('technova_role');
          localStorage.removeItem('technova_user');
          document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
          document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        } catch (e) { /* storage error */ }
      });
    });
  }

  document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initTableSearch();
    initDemoActions();
    initLogoutHandler();
  });

  window.toast = toast;
})();
