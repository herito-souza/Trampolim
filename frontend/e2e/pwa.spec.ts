import { expect, test } from '@playwright/test';

test('manifesto, Service Worker e app shell continuam disponíveis offline', async ({ page, context }) => {
  await page.goto('/');
  const manifestPath = await page.locator('link[rel="manifest"]').getAttribute('href');
  expect(manifestPath).toBe('/manifest.webmanifest');
  const manifest = await page.request.get('/manifest.webmanifest');
  expect(manifest.ok()).toBeTruthy();
  expect((await manifest.json()).icons).toHaveLength(3);

  await page.evaluate(() => navigator.serviceWorker.register('/sw.js', { scope: '/' }).then(() => undefined));
  await page.evaluate(() => navigator.serviceWorker.ready);
  await expect.poll(() => page.evaluate(() => Boolean(navigator.serviceWorker.controller))).toBeTruthy();

  await context.setOffline(true);
  // Chromium's offline emulation blocks requests; dispatch the browser event too
  // so the accessible connection notice can be checked deterministically.
  await page.evaluate(() => window.dispatchEvent(new Event('offline')));
  await expect(page.getByRole('status').filter({ hasText: 'Sem conexão.' })).toBeVisible();
  await context.setOffline(false);
  await page.evaluate(() => window.dispatchEvent(new Event('online')));
  await expect(page.getByRole('status').filter({ hasText: 'Conexão restabelecida.' })).toBeVisible();
  await context.setOffline(true);
  await page.reload();
  await expect(page.locator('#root > *')).toHaveCount(1);
  await expect(page.getByRole('heading', { name: 'Descubra seu potencial. Construa seu futuro.' })).toBeVisible();
});

test('atualização aguarda escolha do usuário e oferece recarga explícita', async ({ page }) => {
  await page.goto('/');
  await page.evaluate(async () => {
    const registration = await navigator.serviceWorker.register('/sw.js', { scope: '/' });
    await navigator.serviceWorker.ready;
    await new Promise<void>((resolve, reject) => {
      if (registration.waiting) { resolve(); return; }
      registration.addEventListener('updatefound', () => {
        const worker = registration.installing;
        if (!worker) { reject(new Error('Service Worker de atualização não iniciou.')); return; }
        worker.addEventListener('statechange', () => {
          if (worker.state === 'installed') resolve();
        });
      }, { once: true });
      void navigator.serviceWorker.register('/sw.js?pwa-test-update=2', { scope: '/' }).catch(reject);
    });
  });
  await expect(page.getByText('Há uma atualização do Trampolim pronta.')).toBeVisible();
  await page.getByRole('button', { name: 'Preparar atualização' }).click();
  await expect(page.getByRole('button', { name: 'Recarregar agora' })).toBeVisible();
});
