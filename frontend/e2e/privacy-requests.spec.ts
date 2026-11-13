import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
for (const width of [320, 375, 1440]) test(`solicitação de privacidade no Capacita em ${width}px`, async ({ page }) => {
  await page.setViewportSize({ width, height: 900 });
  await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'synthetic-privacy-session'));
  let rows: { id: string; state: string; basis_code: string; revision: number; review_after: null }[] = [];
  const keys: string[] = [];
  await page.route('http://localhost:8080/api/**', async route => {
    const req = route.request(), path = new URL(req.url()).pathname;
    if (path.endsWith('/account/privacy/requests') && req.method() === 'POST') { const input = req.postDataJSON(); expect(input.confirmed).toBe(true); keys.push(input.clientKey); rows = [{ id: '12345678-1234-1234-1234-123456789abc', state: 'OPEN', basis_code: 'USER_REQUEST', revision: 0, review_after: null }]; return route.fulfill({ json: rows[0] }); }
    if (path.endsWith('/account/privacy/requests')) return route.fulfill({ json: rows });
    if (path.endsWith('/cancel')) { rows[0] = { ...rows[0], state: 'CANCELLED', revision: 1 }; return route.fulfill({ json: rows[0] }); }
    if (path.endsWith('/learning/me')) return route.fulfill({ json: { id: 1, name: 'Estudante sintético', teacher: false, admin: false } });
    if (path.endsWith('/age-declaration')) return route.fulfill({ json: { ageBand: 'AGE_18_PLUS' } });
    if (path.endsWith('/notifications')) return route.fulfill({ json: { items: [], unreadCount: 0 } });
    if (path.endsWith('/stream')) return route.fulfill({ contentType: 'text/event-stream', body: 'event: ready\ndata: {}\n\n' });
    if (path.endsWith('/geography/settings')) return route.fulfill({ json: { defaultRegion: 'irece-ba', referenceMunicipality: { code: '2914604', name: 'Irecê', state: 'BA' }, states: ['BA'], regions: [] } });
    return route.fulfill({ json: [] });
  });
  await page.goto('/#capacita/account');
  const panel = page.getByRole('region', { name: 'Solicitações de privacidade' });
  await expect(panel).toBeVisible();
  await expect(panel.getByRole('button', { name: 'Enviar solicitação' })).toBeDisabled();
  await panel.getByRole('checkbox').focus(); await page.keyboard.press('Space');
  await panel.getByRole('button', { name: 'Enviar solicitação' }).click();
  await expect(panel.getByText('Solicitação recebida', { exact: true })).toBeVisible();
  await panel.getByRole('button', { name: 'Cancelar solicitação' }).click();
  await expect(panel.getByText('Solicitação cancelada', { exact: true })).toBeVisible();
  await panel.getByRole('checkbox').check(); await panel.getByRole('button', { name: 'Enviar solicitação' }).click();
  await expect(panel.getByText('Solicitação recebida', { exact: true })).toBeVisible(); expect(keys).toHaveLength(2); expect(keys[0]).not.toBe(keys[1]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  const scan = await new AxeBuilder({ page }).include('section[aria-label="Solicitações de privacidade"]').withTags(['wcag2a', 'wcag2aa', 'wcag22aa']).analyze();
  expect(scan.violations.map(v => v.id)).toEqual([]);
});
