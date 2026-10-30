import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import path from 'node:path';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

// Synthetic two-page PDF generated only inside tests; never installed as learning content.
function pdfFixture() {
  const content = 'BT /F1 16 Tf 50 700 Td (Material sintetico para teste) Tj ET';
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>', '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 6 0 R >>', '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 6 0 R >>', '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>', `<< /Length ${content.length} >>\nstream\n${content}\nendstream`];
  let pdf = '%PDF-1.4\n'; const offsets = [0];
  objects.forEach((object, index) => { offsets.push(Buffer.byteLength(pdf)); pdf += `${index + 1} 0 obj\n${object}\nendobj\n`; });
  const xref = Buffer.byteLength(pdf); pdf += `xref\n0 7\n0000000000 65535 f \n${offsets.slice(1).map(offset => `${String(offset).padStart(10, '0')} 00000 n \n`).join('')}trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`; return Buffer.from(pdf);
}
const lesson = { id: 101, module_id: 30, title: 'Aula integrada', description: '', objectives: '', status: 'PUBLISHED', duration_seconds: 180, validation_status: 'READY' };
const course = { id: 10, title: 'Curso sintético', description: 'Teste', category: 'Matemática', teacher_id: 1, teacher_name: 'Professor de teste', status: 'PUBLISHED', subjects: [{ id: 20, title: 'Disciplina', description: '', modules: [{ id: 30, title: 'Módulo', description: '', lessons: [lesson, { ...lesson, id: 102, title: 'Segunda aula' }] }] }] };
function material() { return { id: 7, course_id: 10, subject_id: 20, module_id: 30, teacher_id: 1, title: 'Guia de frações', description: 'Material complementar de teste', topic: 'Frações', category: 'Apostila', alternative_text: 'Alternativa didática fornecida pelo professor.', access_scope: 'STUDENTS', allow_download: false, status: 'PUBLISHED', revision: 2, current_version_id: 70 as number | null, teacher_name: 'Professor de teste', course_title: course.title, subject_title: 'Disciplina', module_title: 'Módulo', published_at: '2026-10-29T12:00:00Z', version_number: 1, page_count: 2, has_text: true, tagged: false, lesson_ids: [101, 102], versions: [{ id: 70, version_number: 1, created_at: '2026-10-29T12:00:00Z', page_count: 2 }] }; }

for (const device of [{ name: 'computador', width: 1440, height: 1000 }, { name: 'celular', width: 390, height: 844 }]) {
  test(`professor publica PDF e associa o mesmo material a duas aulas no ${device.name}`, async ({ page }) => {
    await page.setViewportSize(device); await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'library-teacher'));
    const item = { ...material(), status: 'DRAFT', revision: 0, current_version_id: null }; let created = false; let uploaded = false; let published = false;
    await page.route('http://localhost:8080/api/**', async route => {
      const request = route.request(); const url = new URL(request.url()); const pathname = url.pathname;
      if (pathname.startsWith('/api/capacita/learning/')) {
        expect(request.headers().authorization).toBe('Bearer library-teacher');
        if (pathname.endsWith('/me')) return route.fulfill({ json: { id: 1, teacher: true, admin: false } });
        if (pathname.endsWith('/courses')) return route.fulfill({ json: [course] });
        if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course });
        if (pathname.endsWith('/library/config')) return route.fulfill({ json: { maxBytes: 20971520, maxPages: 300 } });
        if (pathname.endsWith('/library/materials')) { if (request.method() === 'POST') { const data = request.postDataJSON(); expect(data.status).toBe('DRAFT'); expect(data.lessonIds).toEqual([101, 102]); created = true; return route.fulfill({ json: { id: 7 } }); } return route.fulfill({ json: created ? [item] : [] }); }
        if (pathname.endsWith('/materials/7/versions')) { expect(request.headers()['content-type']).toContain('multipart/form-data'); expect(url.searchParams.get('revision')).toBe(String(item.revision)); uploaded = true; item.current_version_id = 70; item.revision++; return route.fulfill({ json: { id: 70, duplicate: false } }); }
        if (pathname.endsWith('/materials/7')) { if (request.method() === 'PUT') { const data = request.postDataJSON(); expect(data.revision).toBe(item.revision); if (data.status === 'PUBLISHED') { expect(uploaded).toBe(true); published = true; } Object.assign(item, { ...data, revision: item.revision + 1 }); return route.fulfill({ status: 204 }); } return route.fulfill({ json: item }); }
      } return route.fulfill({ json: [] });
    });
    await page.goto('/#capacita'); await page.getByRole('button', { name: 'Biblioteca virtual', exact: true }).click(); const library = page.getByRole('region', { name: 'Biblioteca virtual', exact: true });
    await library.getByRole('button', { name: 'Gerenciar biblioteca' }).click(); await library.getByRole('button', { name: 'Novo material PDF' }).click(); const form = page.getByRole('form', { name: 'Publicação de material PDF' });
    await form.getByLabel('Título do material').fill('Guia de frações'); await form.getByLabel('Assunto do material').fill('Frações'); await form.getByLabel('Disciplina do material').selectOption('20'); await form.getByLabel('Módulo do material').selectOption('30'); await form.getByLabel('Aula integrada', { exact: true }).check(); await form.getByLabel('Segunda aula', { exact: true }).check();
    await form.getByLabel('Arquivo PDF').setInputFiles({ name: 'guia.pdf', mimeType: 'application/pdf', buffer: pdfFixture() }); await form.getByLabel('Situação do material').selectOption('PUBLISHED'); await form.getByRole('button', { name: 'Salvar material' }).click(); await expect.poll(() => published).toBe(true); await expect(library.getByText('Publicado', { exact: false }).first()).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  });
}

test('aluno lê PDF real em tela estreita, pesquisa, navega páginas, usa zoom e alternativa textual', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 }); await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'library-student'));
  const item = material(); let searched = false; let textOpened = false;
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request(); const url = new URL(request.url()); const pathname = url.pathname;
    if (pathname.startsWith('/api/capacita/learning/')) {
      expect(request.headers().authorization).toBe('Bearer library-student');
      if (pathname.endsWith('/me')) return route.fulfill({ json: { id: 2, teacher: false, admin: false } });
      if (pathname.endsWith('/courses')) return route.fulfill({ json: [course] }); if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course });
      if (pathname.endsWith('/materials')) { if (url.searchParams.get('search') === 'frações') { searched = true; expect(url.searchParams.get('course')).toBe('10'); expect(url.searchParams.get('module')).toBe('30'); } return route.fulfill({ json: [item] }); }
      if (pathname.endsWith('/materials/7')) return route.fulfill({ json: item });
      if (pathname.endsWith('/materials/7/file')) return route.fulfill({ body: pdfFixture(), contentType: 'application/pdf', headers: { 'access-control-allow-origin': '*' } });
      if (pathname.endsWith('/materials/7/text')) { textOpened = true; return route.fulfill({ json: { alternativeText: item.alternative_text, extractedText: 'Texto extraído para teste.', extractionVerified: false } }); }
    } return route.fulfill({ json: [] });
  });
  await page.goto('/#capacita'); await page.getByRole('button', { name: 'Biblioteca virtual', exact: true }).click(); const library = page.getByRole('region', { name: 'Biblioteca virtual', exact: true }); await expect(library.getByRole('button', { name: 'Gerenciar biblioteca' })).toHaveCount(0);
  await library.getByLabel('Curso da biblioteca').selectOption('10'); await library.getByLabel('Módulo da biblioteca').selectOption('30'); await library.getByLabel('Pesquisar título ou assunto').fill('frações'); await expect.poll(() => searched).toBe(true); await library.getByRole('button', { name: 'Ler PDF' }).click();
  const reader = page.getByRole('region', { name: 'Leitura de Guia de frações' }); await expect(reader).toBeVisible(); await expect(reader.getByText('Carregando PDF…', { exact: true })).toBeHidden({ timeout: 15000 }); await expect(reader.getByText('de 2', { exact: true })).toBeVisible(); await expect(reader.locator('.textLayer')).toContainText('Material sintetico para teste'); await expect(reader.getByRole('button', { name: 'Baixar PDF' })).toHaveCount(0);
  await reader.getByRole('button', { name: 'Próxima página' }).click(); await expect(reader.getByLabel('Página do PDF')).toHaveValue('2'); await reader.getByLabel('Zoom do PDF', { exact: true }).selectOption('1.5'); await reader.getByRole('button', { name: 'Consultar alternativa textual' }).click(); await expect(reader.getByText(item.alternative_text)).toBeVisible(); expect(textOpened).toBe(true);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  if (process.env.LEARNING_SCREENSHOTS_DIR) { mkdirSync(process.env.LEARNING_SCREENSHOTS_DIR, { recursive: true }); await reader.screenshot({ path: path.join(process.env.LEARNING_SCREENSHOTS_DIR, 'biblioteca-aluno-celular.png') }); }
});

test('professor gera PDF no editor e recebe mensagem de falha de upload sem publicar', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'library-teacher')); const item = { ...material(), status: 'DRAFT', revision: 0, current_version_id: null }; let generated = false; let fail = true;
  await page.route('http://localhost:8080/api/**', async route => { const request = route.request(); const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/me')) return route.fulfill({ json: { id: 1, teacher: true } }); if (pathname.endsWith('/courses')) return route.fulfill({ json: [course] }); if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course });
    if (pathname.endsWith('/materials')) return route.fulfill({ json: [item] }); if (pathname.endsWith('/library/config')) return route.fulfill({ json: { maxBytes: 20971520 } });
    if (pathname.endsWith('/materials/7')) { if (request.method() === 'PUT') { item.revision++; return route.fulfill({ status: 204 }); } return route.fulfill({ json: item }); }
    if (pathname.endsWith('/materials/7/compose')) { expect(request.postDataJSON().content).toBe('Conteúdo didático de teste.'); if (fail) return route.fulfill({ status: 400, json: { detail: 'PDF não aceito para teste.' } }); generated = true; return route.fulfill({ json: { id: 70 } }); } return route.fulfill({ json: [] });
  });
  await page.goto('/#capacita'); await page.getByRole('button', { name: 'Biblioteca virtual', exact: true }).click(); const library = page.getByRole('region', { name: 'Biblioteca virtual', exact: true }); await library.getByRole('button', { name: 'Gerenciar biblioteca' }).click(); await library.getByRole('button', { name: 'Editar material' }).click(); const form = page.getByRole('form', { name: 'Publicação de material PDF' }); await form.getByLabel('Criar PDF com editor de texto').check(); await form.getByLabel('Conteúdo do módulo didático').fill('Conteúdo didático de teste.'); await form.getByRole('button', { name: 'Salvar material' }).click(); await expect(form.getByRole('alert')).toHaveText('PDF não aceito para teste.'); expect(generated).toBe(false); fail = false; await form.getByRole('button', { name: 'Salvar material' }).click(); await expect.poll(() => generated).toBe(true);
});

test('biblioteca e mural da aula preservam o reprodutor e seu contexto', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'library-student')); let scoped = false;
  await page.route('http://localhost:8080/api/**', async route => { const request = route.request(); const url = new URL(request.url()); const pathname = url.pathname;
    if (pathname.endsWith('/media/library-context')) { const bytes = readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), 'fixtures', 'lesson-180s.mp4')); const match = request.headers().range?.match(/bytes=(\d+)-(\d*)/); const start = match ? Number(match[1]) : 0; const end = match?.[2] ? Number(match[2]) : bytes.length - 1; return route.fulfill({ status: match ? 206 : 200, body: bytes.subarray(start, end + 1), contentType: 'video/mp4', headers: { 'access-control-allow-origin': '*', 'accept-ranges': 'bytes', ...(match ? { 'content-range': `bytes ${start}-${end}/${bytes.length}` } : {}) } }); }
    if (pathname.endsWith('/me')) return route.fulfill({ json: { id: 2, teacher: false, admin: false } }); if (pathname.endsWith('/courses')) return route.fulfill({ json: [course] }); if (pathname.endsWith('/courses/10')) return route.fulfill({ json: course }); if (pathname.endsWith('/lessons/101')) return route.fulfill({ json: { ...lesson, progress: { position_seconds: 44, watched_seconds: 44 } } }); if (pathname.endsWith('/playback')) return route.fulfill({ json: { sessionId: 'library-context', positionSeconds: 44, videoPath: '/capacita/learning/media/library-context' } }); if (pathname.endsWith('/library/materials')) { scoped = url.searchParams.get('lesson') === '101'; return route.fulfill({ json: [material()] }); } return route.fulfill({ json: [] });
  });
  await page.goto('/#capacita'); await page.getByRole('button', { name: 'Conhecer curso', exact: true }).click(); await page.getByRole('button', { name: 'Módulo', exact: true }).click(); await page.getByRole('button', { name: 'Assistir aula', exact: true }).first().click(); const video = page.locator('video'); await expect(video).toHaveCount(1); await video.evaluate(node => { node.dataset.studyContext = 'preserved'; });
  await page.getByRole('button', { name: 'Materiais desta aula', exact: true }).click(); await expect.poll(() => scoped).toBe(true); await expect(video).toHaveAttribute('data-study-context', 'preserved'); await page.getByRole('button', { name: 'Dúvidas desta aula', exact: true }).click(); await expect(video).toHaveAttribute('data-study-context', 'preserved'); await page.getByRole('button', { name: 'Fechar biblioteca', exact: true }).click(); await expect(video).toHaveAttribute('data-study-context', 'preserved');
});
