/**
 * ============================================================================
 * TechNova Electronics — Global Auth Fetch Interceptor & Session Sync
 * ============================================================================
 * Loaded across Customer, Manager, Employee, and Shipper interfaces.
 * Functions:
 *   1. Intercepts window.fetch to automatically include JWT Authorization header.
 *   2. Synchronizes renewed tokens from response header 'X-New-Access-Token'.
 *   3. Catches HTTP 401s to perform silent token refresh via /auth/refresh-token
 *      with concurrency queueing and automatic request retry.
 *   4. Updates user activity timestamp on any network communication.
 * ============================================================================
 */
(function () {
  'use strict';

  if (window.__technovaAuthInterceptorInstalled || window.__AUTH_INTERCEPTOR_SETUP__) {
    return;
  }
  window.__technovaAuthInterceptorInstalled = true;
  window.__AUTH_INTERCEPTOR_SETUP__ = true;

  const originalFetch = window.fetch;
  let isRefreshing = false;
  let refreshQueue = [];

  function processQueue(error, newToken) {
    refreshQueue.forEach(item => {
      if (error) {
        item.reject(error);
      } else {
        item.resolve(newToken);
      }
    });
    refreshQueue = [];
  }

  function markUserActive() {
    try {
      localStorage.setItem('technova_last_active', Date.now().toString());
    } catch (_) {}
  }

  function clearAllSessionData() {
    try {
      localStorage.removeItem('technova_jwt');
      localStorage.removeItem('technova_role');
      localStorage.removeItem('technova_user');
      localStorage.removeItem('technova_last_active');
      localStorage.removeItem('technova_last_ping');
      localStorage.removeItem('technova_remember_me');
      document.cookie = 'JWT_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      document.cookie = 'REFRESH_TOKEN=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      document.cookie = 'JSESSIONID=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    } catch (_) {}
  }

  async function silentRefreshToken() {
    try {
      const res = await originalFetch('/auth/refresh-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin'
      });
      const data = await res.json();
      if (res.ok && data.success && data.data?.token) {
        const newToken = data.data.token;
        try {
          localStorage.setItem('technova_jwt', newToken);
          if (data.data?.user?.role) {
            localStorage.setItem('technova_role', data.data.user.role);
          }
          markUserActive();
        } catch (_) {}
        return newToken;
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  window.fetch = async function (resource, options = {}) {
    options = options || {};
    let token = null;
    try {
      token = localStorage.getItem('technova_jwt');
    } catch (_) {}

    // 1. Tự động gắn Bearer Token nếu chưa có
    if (token) {
      if (!options.headers) {
        options.headers = {};
      }
      const isHeadersInstance = typeof Headers !== 'undefined' && options.headers instanceof Headers;
      const hasAuth = isHeadersInstance
        ? options.headers.has('Authorization')
        : (options.headers['Authorization'] || options.headers['authorization']);

      if (!hasAuth) {
        if (isHeadersInstance) {
          options.headers.set('Authorization', 'Bearer ' + token);
        } else {
          options.headers['Authorization'] = 'Bearer ' + token;
        }
      }
    }

    // Đảm bảo cookie luôn được gửi cùng các request same-origin
    if (!options.credentials) {
      options.credentials = 'same-origin';
    }

    markUserActive();

    let response;
    try {
      response = await originalFetch(resource, options);
    } catch (err) {
      throw err;
    }

    // 2. Kiểm tra xem Server có vừa tự động làm mới token qua header X-New-Access-Token không
    try {
      const serverNewToken = response.headers.get('X-New-Access-Token');
      if (serverNewToken) {
        localStorage.setItem('technova_jwt', serverNewToken);
        markUserActive();
      }
    } catch (_) {}

    // 3. Xử lý khi nhận mã lỗi 401 Unauthorized
    const urlStr = typeof resource === 'string' ? resource : (resource?.url || '');
    const isAuthApi = urlStr.includes('/auth/refresh-token') || urlStr.includes('/auth/login') || urlStr.includes('/auth/logout');

    if (response.status === 401 && !isAuthApi) {
      // Đang có một tiến trình refresh token khác chạy -> chờ và dùng chung kết quả
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({
            resolve: (newToken) => {
              if (typeof Headers !== 'undefined' && options.headers instanceof Headers) {
                options.headers.set('Authorization', 'Bearer ' + newToken);
              } else {
                options.headers['Authorization'] = 'Bearer ' + newToken;
              }
              resolve(originalFetch(resource, options));
            },
            reject: (err) => reject(err)
          });
        });
      }

      isRefreshing = true;
      const newToken = await silentRefreshToken();
      isRefreshing = false;

      if (newToken) {
        processQueue(null, newToken);
        if (typeof Headers !== 'undefined' && options.headers instanceof Headers) {
          options.headers.set('Authorization', 'Bearer ' + newToken);
        } else {
          options.headers['Authorization'] = 'Bearer ' + newToken;
        }
        return originalFetch(resource, options);
      } else {
        processQueue(new Error('Session expired'), null);
        clearAllSessionData();

        const currentPath = window.location.pathname;
        if (currentPath.startsWith('/manager') ||
            currentPath.startsWith('/employee') ||
            currentPath.startsWith('/shipper') ||
            currentPath.startsWith('/profile') ||
            currentPath.startsWith('/cart') ||
            currentPath.startsWith('/checkout')) {
          window.location.replace('/login?error=session_expired');
        }
      }
    }

    return response;
  };
})();
