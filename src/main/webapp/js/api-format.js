// API Format helper
const API_FORMAT_STORAGE_KEY = 'filmsApi.format';

function getQueryParam(name) {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

function setQueryParam(name, value) {
  const url = new URL(window.location.href);
  url.searchParams.set(name, value);
  window.location.href = url.toString();
}

function getSelectedApiFormat() {
  const fromQuery = getQueryParam('format');
  if (fromQuery) return fromQuery.toLowerCase();

  try {
    const fromStorage = localStorage.getItem(API_FORMAT_STORAGE_KEY);
    if (fromStorage) return fromStorage.toLowerCase();
  } catch (e) {
    // ignore storage access failures
  }

  return 'json';
}

function persistApiFormat(format) {
  try {
    localStorage.setItem(API_FORMAT_STORAGE_KEY, String(format || '').toLowerCase());
  } catch (e) {
    // ignore storage access failures
  }
}

function ensureFormatFieldOnForms(format) {
  const forms = Array.from(document.querySelectorAll('form'));
  forms.forEach(form => {
    let formatInput = form.querySelector('input[name="format"]');
    if (!formatInput) {
      formatInput = document.createElement('input');
      formatInput.type = 'hidden';
      formatInput.name = 'format';
      form.appendChild(formatInput);
    }
    formatInput.value = format;
  });
}

document.addEventListener('DOMContentLoaded', function() {
  const select = document.getElementById('apiFormatSelect');
  const current = getSelectedApiFormat();
  persistApiFormat(current);
  ensureFormatFieldOnForms(current);

  if (select) {
    // initialize select value
    for (const opt of select.options) {
      if (opt.value.toLowerCase() === current) {
        opt.selected = true;
        break;
      }
    }

    select.addEventListener('change', function(e) {
      const val = e.target.value;
      persistApiFormat(val);
      setQueryParam('format', val);
    });
  }

  const display = document.getElementById('currentApiFormat');
  if (display) {
    display.textContent = current.toUpperCase();
  }
  // Append format param to internal links so selected format persists across navigation
  try {
    const anchors = Array.from(document.querySelectorAll('a[href]'));
    anchors.forEach(a => {
      const href = a.getAttribute('href');
      if (!href) return;
      const lower = href.toLowerCase();
      if (lower.startsWith('javascript:') || lower.startsWith('mailto:') || lower.startsWith('#')) return;
      // Only rewrite relative or same-origin links
      const isAbsolute = /^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(href);
      if (isAbsolute) {
        try {
          const url = new URL(href, window.location.href);
          if (url.origin !== window.location.origin) return;
          if (!url.searchParams.has('format')) url.searchParams.set('format', current);
          a.setAttribute('href', url.pathname + url.search + url.hash);
        } catch (e) {
          return;
        }
      } else {
        // relative link
        const url = new URL(href, window.location.href);
        if (!url.searchParams.has('format')) url.searchParams.set('format', current);
        a.setAttribute('href', url.pathname + url.search + url.hash);
      }
    });
  } catch (e) {
    // ignore
  }
});

// expose for other scripts
window.__apiFormat = {
  getSelectedApiFormat
};
