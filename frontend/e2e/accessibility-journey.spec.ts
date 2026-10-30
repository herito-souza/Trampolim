import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { mkdirSync, writeFileSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
test.setTimeout(60000);
const course = { id: 10, title: 'Curso sintético acessível', category: 'Matemática', description: 'Fixture de auditoria', teacher_name: 'Professor de teste', teacher_id: 1, status: 'PUBLISHED', subjects: [{ id: 20, title: 'Disciplina', description: '', modules: [{ id: 30, title: 'Módulo', description: '', lessons: [] }] }] };
async function fixture(page: import('@playwright/test').Page, authenticated = true, teacher = false) {
  if (authenticated) await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'accessibility-fixture'));
  await page.route('http://localhost:8080/api/**', async route => { const request = route.request(); const url = new URL(request.url()); const pathname = url.pathname;
    if (pathname.endsWith('/inclusion/preferences')) return route.fulfill(request.method() === 'PUT' ? { status: 204 } : { json: { saved: false } });
    if (pathname.endsWith('/learning/me')) return route.fulfill({ json: { id: teacher ? 1 : 2, teacher, admin: teacher } });
    if (pathname.endsWith('/learning/courses')) return route.fulfill({ json: [course] }); if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course });
    if (pathname.endsWith('/notifications')) return route.fulfill({ json: { items: [], unreadCount: 0 } }); if (pathname.endsWith('/stream')) return route.fulfill({ contentType: 'text/event-stream', body: 'event: ready\ndata: {}\n\n' });
    if (pathname.endsWith('/capacita/tracks')) return route.fulfill({ json: [{ id: 1, code: 'HIGH_SCHOOL', title: 'Ensino médio', categories: [{ id: 1, name: 'Matemática', description: 'Materiais', contentsCount: 1 }] }] });
    if (pathname.endsWith('/capacita/contents')) return route.fulfill({ json: [{ id: 1, title: 'Material sintético', summary: 'Conteúdo para testar o diálogo.', stageCode: 'HIGH_SCHOOL', categoryId: 1, categoryName: 'Matemática', sourceUrl: 'https://example.test/material' }] });
    if (pathname.endsWith('/performance')) return route.fulfill({ json: { courses: [], coursesStarted: 0, coursesCompleted: 0, lessonsCompleted: 0, modulesCompleted: 0, assessmentsSubmitted: 0, assessmentsPassed: 0, competenciesEarned: 0 } });
    if (pathname.endsWith('/discussion/config')) return route.fulfill({ json: { maxBytes: 26214400, audioMaxSeconds: 120, videoMaxSeconds: 120 } });
    if (pathname.endsWith('/library/config')) return route.fulfill({ json: { maxBytes: 20971520, maxPages: 300 } });
    return route.fulfill({ json: [] });
  });
}
async function audit(page: import('@playwright/test').Page, name: string) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']).analyze();
  if (process.env.LEARNING_SCREENSHOTS_DIR) { mkdirSync(process.env.LEARNING_SCREENSHOTS_DIR, { recursive: true }); writeFileSync(path.join(process.env.LEARNING_SCREENSHOTS_DIR, `axe-${name}.json`), JSON.stringify({ violations: results.violations, incomplete: results.incomplete, passes: results.passes.map(item => item.id) }, null, 2)); }
  expect(results.violations.map(item => ({ id: item.id, impact: item.impact, nodes: item.nodes.map(node => node.target) }))).toEqual([]);
}
test('login, cadastro, atalho de conteúdo e diálogo têm teclado e semântica acessíveis', async ({ page }) => {
  await fixture(page, false); await page.goto('/#capacita'); await audit(page, 'entrada');
  const skip = page.getByRole('link', { name: 'Pular para os conteúdos' }); await skip.focus(); await page.keyboard.press('Enter'); await expect(page.locator('#conteudos')).toBeFocused(); expect(new URL(page.url()).hash).toBe('#capacita');
  const details = page.getByRole('button', { name: 'Ver detalhes', exact: true }); await details.click(); const dialog = page.getByRole('dialog'); const close = dialog.getByRole('button', { name: 'Fechar detalhes' }); await expect(close).toBeFocused(); await page.keyboard.press('Shift+Tab'); await expect(dialog.getByRole('link', { name: 'Abrir material', exact: false })).toBeFocused(); await page.keyboard.press('Tab'); await expect(close).toBeFocused(); await page.keyboard.press('Escape'); await expect(dialog).toHaveCount(0); await expect(details).toBeFocused();
  await page.getByRole('button', { name: 'Entrar', exact: true }).click(); await audit(page, 'login'); await page.getByRole('link', { name: 'Pular para o formulário' }).focus(); await page.keyboard.press('Enter'); await expect(page.locator('#auth-form')).toBeFocused();
  await page.getByRole('button', { name: 'Criar conta', exact: true }).click(); await audit(page, 'cadastro');
});
test('preferências persistem e ampliação com espaçamento não corta conteúdo em celular', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 }); await fixture(page, false); await page.goto('/#capacita'); await page.getByText('Acessibilidade e preferências', { exact: true }).click(); const preferences = page.getByRole('complementary', { name: 'Preferências de acessibilidade do Capacita' });
  await preferences.getByLabel('Tamanho da fonte').selectOption('150'); await preferences.getByLabel('Aumentar espaçamento do texto').check(); await preferences.getByLabel('Alto contraste').check(); await preferences.getByLabel('Reduzir animações').check(); await expect(page.locator('html')).toHaveCSS('font-size', '24px'); await expect(page.locator('.cap-a11y')).toHaveAttribute('data-motion', 'true'); await audit(page, 'contraste-ampliado');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true); await page.reload(); await expect(page.locator('html')).toHaveCSS('font-size', '24px'); await expect(page.locator('.cap-a11y')).toHaveAttribute('data-contrast', 'true');
  await page.getByRole('button', { name: 'Início', exact: true }).click(); await expect(page.locator('html')).toHaveCSS('font-size', '16px');
});
test('painéis do aluno têm foco, teclado, árvore acessível e verificações WCAG', async ({ page }) => {
  await fixture(page); await page.goto('/#capacita');
  for (const [button, name, close] of [['Mini provas', 'Avaliações educacionais', 'Fechar avaliações'], ['Meu desempenho e competências', 'Desempenho e competências', 'Fechar desempenho'], ['Biblioteca virtual', 'Biblioteca virtual', 'Fechar biblioteca'], ['Minhas dúvidas', 'Meu mural privado de dúvidas', 'Fechar mural'], ['Solicitar apoio educacional', 'Apoio educacional e inclusão', 'Fechar apoio educacional']]) {
    const trigger = page.getByRole('button', { name: button, exact: true }); await trigger.focus(); await page.keyboard.press('Enter'); const panel = page.getByRole('region', { name, exact: true }); await expect(panel).toBeFocused(); await expect(panel.getByRole('heading').first()).toBeVisible(); expect(await panel.ariaSnapshot()).toContain('heading'); await audit(page, name.replaceAll(' ', '-')); await panel.getByRole('button', { name: close, exact: true }).click(); await expect(trigger).toBeFocused();
  }
});
test('painéis e formulários do professor permanecem acessíveis', async ({ page }) => {
  await fixture(page, true, true); await page.goto('/#capacita'); await page.getByRole('button', { name: 'Gerenciar cursos', exact: true }).click(); await page.getByRole('button', { name: 'Criar curso', exact: true }).click(); await audit(page, 'professor-curso'); await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
  await page.getByRole('button', { name: 'Mini provas', exact: true }).click(); const assessments = page.getByRole('region', { name: 'Avaliações educacionais' }); await assessments.getByRole('button', { name: 'Gerenciar avaliações' }).click(); await assessments.getByRole('button', { name: 'Criar mini prova' }).click(); await audit(page, 'professor-avaliacao'); await assessments.getByRole('button', { name: 'Fechar avaliações' }).click();
  await page.getByRole('button', { name: 'Biblioteca virtual', exact: true }).click(); const library = page.getByRole('region', { name: 'Biblioteca virtual', exact: true }); await library.getByRole('button', { name: 'Gerenciar biblioteca' }).click(); await library.getByRole('button', { name: 'Novo material PDF' }).click(); await audit(page, 'professor-pdf'); await library.getByRole('button', { name: 'Fechar biblioteca' }).click();
  await page.getByRole('button', { name: 'Dúvidas recebidas', exact: true }).click(); await audit(page, 'professor-duvidas'); await page.getByRole('button', { name: 'Fechar mural' }).click(); await page.getByRole('button', { name: 'Solicitar apoio educacional' }).click(); await page.getByRole('button', { name: 'Administrar pedidos de apoio' }).click(); await audit(page, 'administracao-apoio');
});

test('redimensionamento de texto a 200% e reflow a 320px preservam o conteúdo', async ({ page }) => {
  await fixture(page, false); await page.setViewportSize({ width: 1280, height: 900 }); await page.goto('/#capacita'); await expect(page.getByRole('heading', { name: 'Trampolim Capacita', exact: true })).toBeVisible(); await page.evaluate(() => { document.documentElement.style.fontSize = '200%'; }); await expect(page.locator('html')).toHaveCSS('font-size', '32px'); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.evaluate(() => { document.documentElement.style.fontSize = '100%'; }); await page.setViewportSize({ width: 320, height: 800 }); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await audit(page, 'reflow-320');
});
test('videoaula aplica preferências reais de mídia e mantém legendas e transcrição acessíveis', async ({ page }) => {
  await fixture(page); await page.addInitScript(() => localStorage.setItem('trampolim_capacita_accessibility_v1', JSON.stringify({ fontPercent: 100, playbackSpeed: 1.5, captions: false, muted: true })));
  const lesson = { id: 101, module_id: 30, title: 'Aula acessível de teste', description: 'Descrição visual sintética', objectives: '', status: 'PUBLISHED', validation_status: 'READY', duration_seconds: 180, captions_vtt: 'WEBVTT\n', transcript: 'Transcrição sintética fornecida pelo professor.', progress: {} };
  const mediaCourse = { ...course, subjects: [{ ...course.subjects[0], modules: [{ ...course.subjects[0].modules[0], lessons: [lesson] }] }] };
  await page.route('http://localhost:8080/api/**', async route => { const request = route.request(); const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/courses/10')) return route.fulfill({ json: mediaCourse }); if (pathname.endsWith('/lessons/101')) return route.fulfill({ json: lesson }); if (pathname.endsWith('/playback')) return route.fulfill({ json: { sessionId: 'a11y-media', positionSeconds: 0, videoPath: '/capacita/learning/media/a11y-video', captionsPath: '/capacita/learning/media/a11y-captions' } });
    if (pathname.endsWith('/media/a11y-captions')) return route.fulfill({ contentType: 'text/vtt', body: 'WEBVTT\n\n00:00.000 --> 00:05.000\nVídeo sintético para teste.\n', headers: { 'access-control-allow-origin': '*' } });
    if (pathname.endsWith('/media/a11y-video')) { const bytes = readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), 'fixtures/lesson-180s.mp4')); const match = request.headers().range?.match(/bytes=(\d+)-(\d*)/); const start = match ? Number(match[1]) : 0; const end = match?.[2] ? Number(match[2]) : bytes.length - 1; return route.fulfill({ status: match ? 206 : 200, contentType: 'video/mp4', body: bytes.subarray(start, end + 1), headers: { 'access-control-allow-origin': '*', 'accept-ranges': 'bytes', ...(match ? { 'content-range': `bytes ${start}-${end}/${bytes.length}` } : {}) } }); } return route.fallback();
  });
  await page.goto('/#capacita'); await page.getByRole('button', { name: 'Conhecer curso', exact: true }).click(); await page.getByRole('button', { name: 'Módulo', exact: true }).click(); await page.getByRole('button', { name: 'Assistir aula', exact: true }).click(); const video = page.getByLabel('Vídeo da aula Aula acessível de teste', { exact: true }); await expect.poll(() => video.evaluate((element: HTMLVideoElement) => element.readyState)).toBeGreaterThanOrEqual(1); await expect(video).toHaveJSProperty('muted', true); await expect(video).toHaveJSProperty('playbackRate', 1.5);
  await page.getByText('Acessibilidade e preferências', { exact: true }).click(); await page.getByLabel('Ativar legendas quando disponíveis').check(); await expect.poll(() => video.evaluate((element: HTMLVideoElement) => element.textTracks[0]?.mode)).toBe('showing'); await page.getByRole('button', { name: 'Reproduzir aula', exact: true }).focus(); await page.keyboard.press('Enter'); await expect(video).toHaveJSProperty('paused', false); await page.getByRole('button', { name: 'Pausar aula', exact: true }).click(); await expect(video).toHaveJSProperty('paused', true); await page.getByText('Ler transcrição', { exact: true }).click(); await expect(page.getByText(lesson.transcript)).toBeVisible(); await audit(page, 'videoaula');
});
