/**
 * AuraDock UI Preview — Utility Helpers
 * utils.js
 *
 * Provides:
 *  - toggleState()       — toggle active/inactive class on elements
 *  - mockInteraction()   — animates a tap/press feedback
 *  - initScrollSnap()    — enhances CSS scroll-snap carousels
 *  - updateClock()       — live clock for Standby screen
 *  - ripple()            — material-like press ripple
 *  - cycleTheme()        — demo multiple wallpaper/theme variants
 */

'use strict';

/* ── Toggle State ─────────────────────────────────────────── */
/**
 * Toggles an `active` class (or a custom class) on a target element.
 * Works with exclusive groups: passing `group` deactivates all siblings first.
 *
 * @param {HTMLElement|string} target  — element or CSS selector
 * @param {object} [opts]
 * @param {string} [opts.className='active']  — class to toggle
 * @param {string} [opts.group]               — CSS selector for exclusive group
 */
function toggleState(target, { className = 'active', group = null } = {}) {
  const el = typeof target === 'string' ? document.querySelector(target) : target;
  if (!el) return;

  if (group) {
    document.querySelectorAll(group).forEach(sibling => {
      if (sibling !== el) sibling.classList.remove(className);
    });
  }

  el.classList.toggle(className);
}

/* ── Mock Interaction (tap feedback) ─────────────────────── */
/**
 * Briefly scales an element to simulate a tap/press.
 *
 * @param {HTMLElement|string} target
 */
function mockInteraction(target) {
  const el = typeof target === 'string' ? document.querySelector(target) : target;
  if (!el) return;

  el.style.transition = 'transform 100ms ease, opacity 100ms ease';
  el.style.transform  = 'scale(0.96)';
  el.style.opacity    = '0.80';

  setTimeout(() => {
    el.style.transform = 'scale(1)';
    el.style.opacity   = '1';
  }, 120);
}

/* ── Ripple Effect ───────────────────────────────────────── */
/**
 * Attaches a ripple press animation to all elements matching `selector`.
 *
 * @param {string} selector  — CSS selector
 */
function ripple(selector) {
  document.querySelectorAll(selector).forEach(el => {
    el.style.position = 'relative';
    el.style.overflow = 'hidden';

    el.addEventListener('pointerdown', (e) => {
      const circle = document.createElement('span');
      const rect   = el.getBoundingClientRect();
      const size   = Math.max(rect.width, rect.height) * 1.5;

      circle.style.cssText = `
        position: absolute;
        width: ${size}px; height: ${size}px;
        left: ${e.clientX - rect.left - size / 2}px;
        top:  ${e.clientY - rect.top  - size / 2}px;
        background: rgba(255,255,255,0.12);
        border-radius: 50%;
        pointer-events: none;
        transform: scale(0);
        animation: _ripple-expand 500ms ease-out forwards;
      `;

      if (!document.getElementById('_ripple-style')) {
        const style = document.createElement('style');
        style.id = '_ripple-style';
        style.textContent = `
          @keyframes _ripple-expand {
            to { transform: scale(1); opacity: 0; }
          }
        `;
        document.head.appendChild(style);
      }

      el.appendChild(circle);
      circle.addEventListener('animationend', () => circle.remove());
    });
  });
}

/* ── Scroll Snap Enhancements ────────────────────────────── */
/**
 * Enhances a scroll-snap carousel.
 * Adds dot indicators and keyboard/swipe navigation.
 *
 * @param {object} opts
 * @param {string} opts.trackSelector    — scroll container
 * @param {string} opts.itemSelector     — individual card/slide selector
 * @param {string} [opts.dotsContainer]  — selector for dot indicator host
 */
function initScrollSnap({ trackSelector, itemSelector, dotsContainer = null }) {
  const track = document.querySelector(trackSelector);
  if (!track) return;

  const items = track.querySelectorAll(itemSelector);
  let currentIndex = 0;

  /* — Dot indicators — */
  let dots = [];
  if (dotsContainer) {
    const container = document.querySelector(dotsContainer);
    if (container) {
      items.forEach((_, i) => {
        const dot = document.createElement('span');
        dot.style.cssText = `
          width: 6px; height: 6px;
          border-radius: 50%;
          background: rgba(255,255,255,0.3);
          cursor: pointer;
          transition: all 200ms ease;
          display: inline-block;
        `;
        dot.addEventListener('click', () => scrollToIndex(i));
        container.appendChild(dot);
        dots.push(dot);
      });
      updateDots(0);
    }
  }

  function updateDots(index) {
    dots.forEach((d, i) => {
      d.style.background   = i === index ? '#fff' : 'rgba(255,255,255,0.3)';
      d.style.width        = i === index ? '18px' : '6px';
      d.style.borderRadius = '9999px';
    });
  }

  function scrollToIndex(index) {
    const item = items[index];
    if (!item) return;
    item.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    currentIndex = index;
    updateDots(index);
  }

  /* — Intersection observer to sync dots with scroll — */
  if (dots.length) {
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const idx = Array.from(items).indexOf(entry.target);
          if (idx !== -1) updateDots(idx);
        }
      });
    }, { root: track, threshold: 0.6 });

    items.forEach(item => observer.observe(item));
  }

  /* — Keyboard navigation — */
  document.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowRight') scrollToIndex(Math.min(currentIndex + 1, items.length - 1));
    if (e.key === 'ArrowLeft')  scrollToIndex(Math.max(currentIndex - 1, 0));
  });

  return { scrollToIndex };
}

/* ── Live Clock ──────────────────────────────────────────── */
/**
 * Updates an element with a live HH:MM clock string.
 *
 * @param {string} selector   — CSS selector of the clock element
 * @param {boolean} [showSeconds=false]
 */
function updateClock(selector, showSeconds = false) {
  const el = document.querySelector(selector);
  if (!el) return;

  function tick() {
    const now = new Date();
    const hh  = String(now.getHours()).padStart(2, '0');
    const mm  = String(now.getMinutes()).padStart(2, '0');
    const ss  = String(now.getSeconds()).padStart(2, '0');
    el.textContent = showSeconds ? `${hh}:${mm}:${ss}` : `${hh}:${mm}`;
  }

  tick();
  setInterval(tick, 1000);
}

/* ── Cycle Theme / Wallpaper Demo ────────────────────────── */
/**
 * Cycles through an array of background values on repeated calls.
 *
 * @param {string} targetSelector
 * @param {string[]} values  — array of CSS background values
 */
function cycleTheme(targetSelector, values) {
  const el = document.querySelector(targetSelector);
  if (!el || !values.length) return;

  let index = 0;
  return function next() {
    index = (index + 1) % values.length;
    el.style.background = values[index];
  };
}

/* ── Delegate click with ripple to .btn elements ─────────── */
document.addEventListener('DOMContentLoaded', () => {
  ripple('.btn');
  ripple('.glass-card');
});
