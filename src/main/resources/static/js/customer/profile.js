/* ==========================================================================
   TechNova Electronics — Customer Profile Script
   --------------------------------------------------------------------------
   (Forwarding to / reusing logic in common/profile.js)
   ========================================================================== */
'use strict';

// Load common profile implementation
if (typeof loadProfile === 'undefined') {
  const s = document.createElement('script');
  s.src = '/js/common/profile.js';
  document.head.appendChild(s);
}
