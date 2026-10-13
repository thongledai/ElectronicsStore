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
        toast(UiMessage.SELECTED_DEMO(sel.options[sel.selectedIndex].text));
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
          localStorage.removeItem('technova_last_active');
          localStorage.removeItem('technova_last_ping');
          document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
          document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        } catch (e) { /* storage error */ }
      });
    });
  }

  /* ---------- Session Inactivity & Heartbeat Manager ---------- */
  function initSessionManager() {
    const TIMEOUT_MS = 2 * 60 * 1000; // 2 phút
    const token = localStorage.getItem('technova_jwt');

    const clearAllSessionData = () => {
      try {
        localStorage.removeItem('technova_jwt');
        localStorage.removeItem('technova_role');
        localStorage.removeItem('technova_user');
        localStorage.removeItem('technova_last_active');
        localStorage.removeItem('technova_last_ping');
        document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      } catch (e) {}
    };

    if (token) {
      const lastActiveStr = localStorage.getItem('technova_last_active');
      const lastActive = lastActiveStr ? parseInt(lastActiveStr, 10) : 0;
      const now = Date.now();

      if (lastActive > 0 && (now - lastActive) > TIMEOUT_MS) {
        clearAllSessionData();
        window.location.replace('/login?error=session_expired');
        return;
      } else {
        localStorage.setItem('technova_last_active', now.toString());
      }
    }

    let lastTouch = Date.now();
    const touchActive = () => {
      const now = Date.now();
      if (now - lastTouch > 5000) {
        lastTouch = now;
        if (localStorage.getItem('technova_jwt')) {
          localStorage.setItem('technova_last_active', now.toString());
        }
      }
    };
    ['mousedown', 'keydown', 'scroll', 'touchstart'].forEach(evt => {
      window.addEventListener(evt, touchActive, { passive: true });
    });

    const onPageUnload = () => {
      if (localStorage.getItem('technova_jwt')) {
        localStorage.setItem('technova_last_active', Date.now().toString());
      }
    };
    window.addEventListener('beforeunload', onPageUnload);
    window.addEventListener('pagehide', onPageUnload);

    setInterval(() => {
      const currentToken = localStorage.getItem('technova_jwt');
      if (!currentToken) return;

      const now = Date.now();
      const lastPing = parseInt(localStorage.getItem('technova_last_ping') || '0', 10);
      if (now - lastPing < 25000) {
        localStorage.setItem('technova_last_active', now.toString());
        return;
      }

      localStorage.setItem('technova_last_ping', now.toString());
      localStorage.setItem('technova_last_active', now.toString());

      fetch('/auth/ping', {
        method: 'GET',
        headers: { 'Authorization': 'Bearer ' + currentToken }
      }).then(res => {
        if (res.status === 401 || res.status === 403) {
          clearAllSessionData();
          window.location.replace('/login?error=session_expired');
        }
      }).catch(() => {});
    }, 35000);
  }

  /* ---------- User Avatar & Identity in Dashboard Topbar ---------- */
  function initUserAvatar() {
    let user = null;
    try {
      user = JSON.parse(localStorage.getItem('technova_user') || 'null');
    } catch (_) {}

    const token = localStorage.getItem('technova_jwt');
    let role = (localStorage.getItem('technova_role') || (user && user.role) || document.documentElement.getAttribute('data-role') || 'Manager').toUpperCase();

    const fullName = user && (user.name || user.fullName) ? (user.name || user.fullName) : role;
    const defaultAvatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(fullName)}&background=0d6efd&color=fff`;
    const avatarUrl = user && user.avatar ? user.avatar : defaultAvatar;

    document.querySelectorAll('.js-topbar-username').forEach(el => {
      el.textContent = fullName;
    });
    document.querySelectorAll('.js-topbar-name').forEach(el => {
      el.textContent = 'Xin chào, ' + fullName;
    });
    document.querySelectorAll('.js-topbar-role').forEach(el => {
      el.textContent = role.replace('ROLE_', '');
    });

    document.querySelectorAll('.js-topbar-avatar').forEach(img => {
      img.onerror = () => {
        img.onerror = null;
        img.src = defaultAvatar;
      };
      img.src = avatarUrl;
    });

    document.querySelectorAll('.role-avatar').forEach(el => {
      if (user && user.avatar) {
        el.innerHTML = `<img src="${user.avatar}" class="w-100 h-100 rounded-circle object-fit-cover" alt="avatar" onerror="this.onerror=null; this.parentElement.textContent='${role.charAt(0)}';">`;
      } else {
        el.textContent = role.charAt(0);
      }
    });

    // Background fetch if user data is missing in cache
    if (token && (!user || !user.avatar || !user.name)) {
      fetch('/api/customer/profile', {
        headers: { 'Accept': 'application/json', 'Authorization': 'Bearer ' + token }
      }).then(r => r.ok ? r.json() : null)
        .then(res => {
          if (res && res.success && res.data) {
            const u = res.data;
            const updated = {
              ...(user || {}),
              name: u.fullName || user?.name || role,
              email: u.email || user?.email || '',
              role: u.role || user?.role || role,
              avatar: u.avatar || null
            };
            try { localStorage.setItem('technova_user', JSON.stringify(updated)); } catch (_) {}
            initUserAvatar();
          }
        }).catch(() => {});
    }
  }

  window.updateDashboardUserAvatar = function(avatarUrl, newName) {
    let user = {};
    try { user = JSON.parse(localStorage.getItem('technova_user') || '{}'); } catch (_) {}
    if (avatarUrl !== undefined) user.avatar = avatarUrl;
    if (newName) user.name = newName;
    try { localStorage.setItem('technova_user', JSON.stringify(user)); } catch (_) {}
    initUserAvatar();
  };

  initSessionManager();

  document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initTableSearch();
    initDemoActions();
    initLogoutHandler();
    initUserAvatar();
  });

  window.toast = toast;
})();
