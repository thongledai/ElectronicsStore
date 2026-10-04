/* ==========================================================================
   TechNova Electronics — About Page
   --------------------------------------------------------------------------
   Only one interactive touch on this page: the statistics counters animate
   from zero to their target value the first time they scroll into view.
   ========================================================================== */

'use strict';

/**
 * Animates a single number from 0 up to its data-count-to value.
 * Uses requestAnimationFrame with an ease-out curve so it decelerates.
 */
function animateCounter(el) {
  const target = Number(el.dataset.countTo) || 0;
  const suffix = el.dataset.suffix || '';
  const duration = 1600;
  const startTime = performance.now();

  function step(now) {
    const progress = Math.min((now - startTime) / duration, 1);
    // easeOutCubic
    const eased = 1 - Math.pow(1 - progress, 3);
    const value = Math.floor(eased * target);

    el.textContent = value.toLocaleString('en-IN') + suffix;

    if (progress < 1) {
      requestAnimationFrame(step);
    } else {
      el.textContent = target.toLocaleString('en-IN') + suffix;
    }
  }

  requestAnimationFrame(step);
}

/** Starts each counter the first time it becomes visible. */
function initCounters() {
  const counters = document.querySelectorAll('[data-count-to]');
  if (!counters.length) return;

  // Fallback for browsers without IntersectionObserver: just set the values
  if (!('IntersectionObserver' in window)) {
    counters.forEach(animateCounter);
    return;
  }

  const observer = new IntersectionObserver((entries, obs) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        animateCounter(entry.target);
        obs.unobserve(entry.target);   // animate once only
      }
    });
  }, { threshold: 0.4 });

  counters.forEach(counter => observer.observe(counter));
}

document.addEventListener('DOMContentLoaded', initCounters);
