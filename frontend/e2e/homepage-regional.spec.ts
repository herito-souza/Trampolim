import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
for (const width of [320, 375, 768, 1024, 1440]) test(`homepage regional: busca e teclado em ${width}px`, async ({ page }) => {
  await page.setViewportSize({ width, height: 900 });
  const queries: URLSearchParams[] = [];
  const unwantedRequests: string[] = [];
  page.on('request', request => { if (/fonts\.googleapis|fonts\.gstatic|RegionalMap|pdf\.worker|\.mp4(?:$|\?)/.test(request.url())) unwantedRequests.push(request.url()); });
  await page.route('http://localhost:8080/api/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/geography/settings')) return route.fulfill({ json: { defaultRegion: 'irece-ba', referenceMunicipality: { code: '2914604', name: 'Irecê', state: 'BA' }, regions: [{ id: 'irece-ba', name: 'Irecê e região', municipalityCodes: ['2914604'] }] } });
    if (url.pathname.endsWith('/geography/municipalities')) return route.fulfill({ json: [{ code: '2919157', name: 'Lapão', state: 'BA' }] });
    if (url.pathname.endsWith('/regional-opportunities')) { expect(route.request().headers().authorization).toBeUndefined(); queries.push(url.searchParams); return route.fulfill({ json: { items: [], nextCursor: null } }); }
    return route.fulfill({ json: [] });
  });
  await page.goto('/#inicio');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Descubra seu potencial.Construa seu futuro.');
  await expect(page.getByRole('combobox', { name: 'Município', exact: true })).toContainText('Irecê — BA');
  expect(queries.length).toBe(0);
  expect(unwantedRequests).toEqual([]);
  await expect.poll(() => page.evaluate(() => document.fonts.check('800 24px "Barlow Condensed"'))).toBe(true);
  await page.getByRole('link', { name: 'Encontrar oportunidades', exact: true }).click();
  await page.getByLabel('Cargo ou área profissional').fill('atendimento');
  await page.getByRole('button', { name: 'Pesquisar oportunidades', exact: true }).click();
  await expect.poll(() => queries.length).toBeGreaterThan(0);
  expect(queries.at(-1)?.get('municipalityCode')).toBe('2914604');
  expect(queries.at(-1)?.get('q')).toBe('atendimento');
  await expect(page.getByText('Nenhuma vaga empresarial encontrada.')).toBeVisible();
  await page.getByRole('button', { name: 'Alterar município', exact: true }).click();
  await page.getByLabel('Buscar município pelo nome').fill('Lapão');
  await page.getByRole('button', { name: 'Lapão — BA', exact: true }).focus(); await page.keyboard.press('Enter');
  await page.getByRole('combobox', { name: 'Modalidade', exact: true }).selectOption('Híbrido');
  await page.getByRole('button', { name: 'Pesquisar oportunidades', exact: true }).click();
  await expect.poll(() => queries.at(-1)?.get('municipalityCode')).toBe('2919157');
  expect(queries.at(-1)?.get('modality')).toBe('Híbrido');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect((await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa']).analyze()).violations).toEqual([]);
  for (const [name, word] of [['Explorar oportunidades', 'oportunidades'], ['Acessar área empresarial', 'empresarial']]) {
    const lines = await page.getByRole('button', { name, exact: true }).evaluate((element, word) => {
      const text = element.firstChild!; const start = text.textContent!.indexOf(word); const range = document.createRange();
      range.setStart(text, start); range.setEnd(text, start + word.length); return range.getClientRects().length;
    }, word);
    expect(lines).toBe(1);
  }
  await page.screenshot({ path: `test-results/homepage-${width}.png`, fullPage: true });
  await page.getByRole('button', { name: 'Descobrir meu caminho profissional', exact: true }).click();
  await expect(page.locator('#diagnostico')).toBeInViewport();
});
