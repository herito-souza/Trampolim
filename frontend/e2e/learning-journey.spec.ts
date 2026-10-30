import { expect, test } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { readFileSync, mkdirSync } from 'node:fs';
const fixture = path.join(path.dirname(fileURLToPath(import.meta.url)), 'fixtures', 'lesson-180s.mp4');

for (const device of [{ name: 'computador', width: 1440, height: 900 }, { name: 'celular', width: 390, height: 844 }]) {
  test(`professor envia videoaula e publica após validação no ${device.name}`, async ({ page }) => {
    await page.setViewportSize(device);
    await page.addInitScript(() => { localStorage.setItem('trampolim_capacita_token', 'teacher-capacita'); localStorage.setItem('trampolim_empresas_token', 'company-kept'); });
    let uploaded = false;
    let published = false;
    let created = false;
    const lesson = { id: 101, module_id: 30, title: 'Aula de teste', description: '', objectives: '', sort_order: 0, status: 'DRAFT', duration_seconds: 180, validation_status: 'READY' };
    const course = { id: 10, title: 'Curso de teste', description: 'Curso sintético de teste', category: 'Matemática', teacher_name: 'Professor de teste', status: 'DRAFT', subjects: [{ id: 20, title: 'Disciplina de teste', description: '', sort_order: 0, modules: [{ id: 30, title: 'Módulo de teste', description: '', sort_order: 0, lessons: created ? [lesson] : [] }] }] };
    await page.route('http://localhost:8080/api/**', async route => {
      const request = route.request(); const url = new URL(request.url()); const pathname = url.pathname;
      if (pathname.startsWith('/api/capacita/learning/')) {
        expect(request.headers().authorization).toBe('Bearer teacher-capacita');
        if (pathname.endsWith('/me')) return route.fulfill({ json: { id: 1, name: 'Professor', email: 'teacher@example.test', teacher: true, admin: false } });
        if (pathname.endsWith('/courses')) return route.fulfill({ json: [course] });
        if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course });
        if (pathname.endsWith('/modules/30/lessons')) { created = true; expect(request.postDataJSON().status).toBe('DRAFT'); return route.fulfill({ json: { id: 101 } }); }
        if (pathname.endsWith('/lessons/101/video')) { expect(request.headers()['content-type']).toContain('multipart/form-data'); uploaded = true; return route.fulfill({ json: { validationStatus: 'READY', durationSeconds: 180 } }); }
        if (pathname.endsWith('/lessons/101') && request.method() === 'PUT') { if (request.postDataJSON().status === 'PUBLISHED') { expect(uploaded).toBe(true); published = true; } return route.fulfill({ status: 204 }); }
        return route.fulfill({ json: [] });
      }
      return route.fulfill({ json: [] });
    });
    await page.goto('/#capacita');
    await page.getByRole('button', { name: 'Gerenciar cursos', exact: true }).click();
    await page.getByRole('button', { name: 'Organizar curso', exact: true }).click();
    await page.getByRole('button', { name: 'Módulo de teste', exact: true }).click();
    await page.getByRole('button', { name: 'Criar videoaula', exact: true }).click();
    await page.getByLabel('Título da aula').fill('Aula de teste');
    await page.getByLabel('Descrição da aula').fill('Descrição curta');
    await page.getByLabel('Objetivos de aprendizagem').fill('Aprender com o vídeo');
    await page.getByLabel('Vídeo MP4, MOV ou WebM').setInputFiles(fixture);
    await expect(page.getByLabel('Prévia do arquivo selecionado')).toBeVisible();
    await page.getByRole('combobox', { name: 'Situação', exact: true }).selectOption('PUBLISHED');
    if (process.env.LEARNING_SCREENSHOTS_DIR) { mkdirSync(process.env.LEARNING_SCREENSHOTS_DIR, { recursive: true }); await page.getByRole('form', { name: 'Publicação de videoaula' }).screenshot({ path: path.join(process.env.LEARNING_SCREENSHOTS_DIR, `professor-${device.name}.png`) }); }
    await page.getByRole('button', { name: 'Salvar aula', exact: true }).click();
    await expect.poll(() => published).toBe(true);
    expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('company-kept');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  });
}

test('aluno navega curso, módulo e videoaula com retomada e transcrição', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'student-capacita'));
  const lesson = { id: 101, module_id: 30, title: 'Aula de teste', description: 'Descrição da aula', objectives: 'Aprender', transcript: 'Transcrição da aula de teste', captions_vtt: 'WEBVTT\n', sort_order: 0, status: 'PUBLISHED', duration_seconds: 180, validation_status: 'READY', progress: { position_seconds: 40, watched_seconds: 40 } };
  const course = { id: 10, title: 'Curso de teste', description: 'Teste', category: 'Matemática', teacher_name: 'Professor', status: 'PUBLISHED', subjects: [{ id: 20, title: 'Disciplina', description: '', modules: [{ id: 30, title: 'Módulo de teste', description: '', lessons: [lesson] }] }] };
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request(); const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/media/test-video')) {
      const video = readFileSync(fixture); const range = request.headers().range;
      const match = range?.match(/bytes=(\d+)-(\d*)/); const start = match ? Number(match[1]) : 0; const end = match?.[2] ? Number(match[2]) : video.length - 1;
      return route.fulfill({ status: match ? 206 : 200, body: video.subarray(start, end + 1), contentType: 'video/mp4', headers: { 'access-control-allow-origin': '*', 'accept-ranges': 'bytes', ...(match ? { 'content-range': `bytes ${start}-${end}/${video.length}` } : {}) } });
    }
    if (pathname.endsWith('/media/test-captions')) return route.fulfill({ body: 'WEBVTT\n\n00:00.000 --> 00:05.000\nLegenda de teste\n', contentType: 'text/vtt', headers: { 'access-control-allow-origin': '*' } });
    if (pathname.startsWith('/api/capacita/learning/')) {
      expect(request.headers().authorization).toBe('Bearer student-capacita');
      if (pathname.endsWith('/me')) return route.fulfill({ json: { id: 2, name: 'Aluno', teacher: false, admin: false } });
      if (pathname.endsWith('/courses')) return route.fulfill({ json: [course] });
      if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course });
      if (pathname.endsWith('/lessons/101')) return route.fulfill({ json: lesson });
      if (pathname.endsWith('/playback')) return route.fulfill({ json: { sessionId: 'test-session', positionSeconds: 40, videoPath: '/capacita/learning/media/test-video', captionsPath: '/capacita/learning/media/test-captions' } });
    }
    return route.fulfill({ json: [] });
  });
  await page.goto('/#capacita');
  await expect(page.getByRole('button', { name: 'Gerenciar cursos', exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Conhecer curso', exact: true }).click();
  await page.getByRole('button', { name: 'Módulo de teste', exact: true }).click();
  await page.getByRole('button', { name: 'Assistir aula', exact: true }).click();
  await expect(page.locator('video')).toBeVisible();
  await expect.poll(() => page.locator('video').evaluate((element: HTMLVideoElement) => element.currentTime)).toBeCloseTo(40, 0);
  await page.locator('video').evaluate((element: HTMLVideoElement) => element.play());
  await expect.poll(() => page.locator('video').evaluate((element: HTMLVideoElement) => element.currentTime)).toBeGreaterThan(40);
  await page.locator('video').evaluate((element: HTMLVideoElement) => { element.pause(); element.volume = 0.5; });
  expect(await page.locator('video').evaluate((element: HTMLVideoElement) => element.paused && element.volume === 0.5)).toBe(true);
  await page.getByRole('combobox', { name: 'Velocidade', exact: true }).selectOption('1.5');
  await expect.poll(() => page.locator('video').evaluate((element: HTMLVideoElement) => element.playbackRate)).toBe(1.5);
  await page.getByText('Ler transcrição', { exact: true }).click();
  await expect(page.getByText('Transcrição da aula de teste', { exact: true })).toBeVisible();
  await expect(page.getByText('Aula concluída', { exact: true })).toHaveCount(0);
  if (process.env.LEARNING_SCREENSHOTS_DIR) { mkdirSync(process.env.LEARNING_SCREENSHOTS_DIR, { recursive: true }); await page.getByRole('region', { name: 'Videoaula', exact: true }).screenshot({ path: path.join(process.env.LEARNING_SCREENSHOTS_DIR, 'player-aluno.png') }); }
});
