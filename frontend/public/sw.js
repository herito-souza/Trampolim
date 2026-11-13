const VERSION = 'trampolim-shell-v3';
const SHELL_CACHE = `${VERSION}-shell`;
const PUBLIC_CACHE = `${VERSION}-public`;
const SHELL = [
  '/',
  '/offline.html',
  '/manifest.webmanifest',
  '/favicon.svg',
  '/icons/trampolim-192.png',
  '/icons/trampolim-512.png',
  '/icons/trampolim-maskable-512.png',
];
const MAX_PUBLIC_ITEMS = 40;
const MAX_PUBLIC_BYTES = 1024 * 1024;
const MAX_STALE_AGE_MS = 7 * 24 * 60 * 60 * 1000;

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL_CACHE);
    await cache.addAll(SHELL);
    // Precache the production entry points referenced by Vite's HTML so the
    // first offline launch works even if the worker took control after load.
    const response = await fetch('/', { cache: 'reload' });
    if (!response.ok) throw new Error('App shell unavailable');
    await cache.put('/', response.clone());
    const html = await response.text();
    const paths = new Set(Array.from(html.matchAll(/(?:src|href)=["']([^"']*\/assets\/[^"']+)["']/g), match => match[1]));
    for (const path of paths) {
      const url = new URL(path, self.location.origin);
      if (url.origin !== self.location.origin) continue;
      const asset = await fetch(url);
      if (!asset.ok || Number(asset.headers.get('Content-Length') || 0) > 2 * MAX_PUBLIC_BYTES) continue;
      await cache.put(url, asset.clone());
      const body = await asset.arrayBuffer();
      if (body.byteLength > 2 * MAX_PUBLIC_BYTES) { await cache.delete(url); continue; }
      if ((asset.headers.get('Content-Type') || '').includes('text/css')) {
        const css = new TextDecoder().decode(body);
        const dependencies = new Set(Array.from(css.matchAll(/url\(["']?([^"')]+)["']?\)/g), match => match[1]));
        for (const dependency of dependencies) {
          const font = new URL(dependency, url);
          if (font.origin !== self.location.origin || !font.pathname.startsWith('/assets/')) continue;
          try { const fontResponse = await fetch(font); if (fontResponse.ok) await cache.put(font, fontResponse); } catch { /* A font may load later when online. */ }
        }
      }
    }
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const names = await caches.keys();
    await Promise.all(names.filter(name => name.startsWith('trampolim-shell-') || name.startsWith('trampolim-public-'))
      .filter(name => name !== SHELL_CACHE && name !== PUBLIC_CACHE).map(name => caches.delete(name)));
    await self.clients.claim();
  })());
});

self.addEventListener('message', event => {
  if (event.data?.type === 'SKIP_WAITING') void self.skipWaiting();
});

function isPublicLearningResource(url) {
  if (url.origin !== self.location.origin || url.search) return false;
  return url.pathname === '/api/capacita/tracks'
    || url.pathname === '/api/capacita/contents'
    || /^\/api\/capacita\/contents\/\d+$/.test(url.pathname);
}

async function trimPublicCache(cache) {
  const keys = await cache.keys();
  if (keys.length <= MAX_PUBLIC_ITEMS) return;
  await Promise.all(keys.slice(0, keys.length - MAX_PUBLIC_ITEMS).map(key => cache.delete(key)));
}

async function fetchPublicLearning(request) {
  const cache = await caches.open(PUBLIC_CACHE);
  try {
    const response = await fetch(request);
    const policy = response.headers.get('Cache-Control') || '';
    const type = response.headers.get('Content-Type') || '';
    const size = Number(response.headers.get('Content-Length') || 0);
    if (response.ok && /\bpublic\b/i.test(policy) && !/\b(?:private|no-store)\b/i.test(policy)
        && type.includes('application/json') && size <= MAX_PUBLIC_BYTES) {
      const body = await response.clone().arrayBuffer();
      if (body.byteLength <= MAX_PUBLIC_BYTES) {
        const headers = new Headers(response.headers);
        headers.set('X-Trampolim-Cached-At', String(Date.now()));
        await cache.put(request, new Response(body, { status: response.status, statusText: response.statusText, headers }));
        await trimPublicCache(cache);
      }
    }
    return response;
  } catch {
    const cached = await cache.match(request);
    if (!cached) throw new Error('Public content unavailable offline');
    const cachedAt = Number(cached.headers.get('X-Trampolim-Cached-At') || 0);
    if (!cachedAt || Date.now() - cachedAt > MAX_STALE_AGE_MS) {
      await cache.delete(request);
      throw new Error('Cached public content expired');
    }
    const headers = new Headers(cached.headers);
    headers.set('X-Trampolim-Offline', 'stale');
    return new Response(await cached.arrayBuffer(), { status: cached.status, statusText: cached.statusText, headers });
  }
}

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET' || request.headers.has('Authorization') || request.headers.has('Range')) return;
  const url = new URL(request.url);
  if (isPublicLearningResource(url)) {
    event.respondWith(fetchPublicLearning(request));
    return;
  }

  if (request.mode === 'navigate') {
    event.respondWith((async () => {
      try { return await fetch(request); }
      catch {
        const cache = await caches.open(SHELL_CACHE);
        return await cache.match('/') || await cache.match('/offline.html') || Response.error();
      }
    })());
    return;
  }

  // Production Vite assets are fingerprinted and contain no user-specific data.
  if (url.origin === self.location.origin && url.pathname.startsWith('/assets/')) {
    event.respondWith((async () => {
      const cache = await caches.open(SHELL_CACHE);
      // Vite's preview/development hosts may set Vary: Origin even though
      // fingerprinted build assets are identical for every origin.
      const saved = await cache.match(request, { ignoreVary: true });
      if (saved) return saved;
      const response = await fetch(request);
      if (response.ok && response.type === 'basic' && Number(response.headers.get('Content-Length') || 0) <= 2 * MAX_PUBLIC_BYTES) {
        const body = await response.clone().arrayBuffer();
        if (body.byteLength <= 2 * MAX_PUBLIC_BYTES) await cache.put(request, response.clone());
      }
      return response;
    })());
  }
});
