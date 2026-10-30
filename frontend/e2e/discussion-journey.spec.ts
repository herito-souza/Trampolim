import { expect, test } from '@playwright/test';
import type { Page, Route } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
test.use({ launchOptions: { args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream'] }, permissions: ['microphone', 'camera'] });
const fixtures = path.join(path.dirname(fileURLToPath(import.meta.url)), 'fixtures');
const course = { id: 10, title: 'Curso de teste', description: 'Teste', category: 'Matemática', teacher_name: 'Professor', status: 'PUBLISHED', subjects: [{ id: 20, title: 'Disciplina', description: '', modules: [{ id: 30, title: 'Módulo de teste', description: '', lessons: [] }] }] };
function question(id = 101, student = 2) { return { id, course_id: 10, module_id: null, lesson_id: null, title: `Dúvida ${id}`, status: 'WAITING', student_id: student, teacher_id: 1, student_name: `Aluno ${student}`, course_title: course.title, module_title: null, lesson_title: null, created_at: '2026-10-29T10:00:00Z' }; }
async function session(page: Page, teacher = false) { await page.addInitScript(({ teacher }) => { localStorage.setItem('trampolim_capacita_token', teacher ? 'teacher-discussion' : 'student-discussion'); localStorage.setItem('trampolim_empresas_token', 'company-independent'); }, { teacher }); }
async function common(route: Route, teacher = false) {
  const url = new URL(route.request().url()); const pathname = url.pathname;
  if (pathname.endsWith('/learning/me')) return route.fulfill({ json: { id: teacher ? 1 : 2, teacher, admin: false, name: teacher ? 'Professor' : 'Aluno' } });
  if (pathname.endsWith('/learning/courses')) return route.fulfill({ json: [course] });
  if (pathname.endsWith('/learning/courses/10')) return route.fulfill({ json: course });
  if (pathname.endsWith('/discussion/config')) return route.fulfill({ json: { maxBytes: 26214400, audioMaxSeconds: 120, videoMaxSeconds: 120 } });
  if (pathname.endsWith('/notifications/stream')) return route.fulfill({ contentType: 'text/event-stream', body: 'event: ready\ndata: {"connected":true}\n\n' });
  if (pathname.endsWith('/notifications')) return route.fulfill({ json: { items: [], unreadCount: 0 } });
  return route.fulfill({ json: [] });
}
test('professor revisa destinatários e repete lote com a mesma chave após falha', async ({ page }) => {
  await session(page, true); let requests = 0; let key = ''; let sent = false; const rows = [question(101, 2), question(102, 3)];
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request(); const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/discussion/questions')) return route.fulfill({ json: rows.map(item => ({ ...item, status: sent ? 'ANSWERED' : 'WAITING' })) });
    if (pathname.endsWith('/discussion/batches') && request.method() === 'GET') return route.fulfill({ json: sent ? [{ id: 'batch-result', questionIds: [101, 102], createdAt: '2026-10-29T10:01:00Z' }] : [] });
    if (pathname.endsWith('/discussion/batches')) {
      requests++; const payload = request.postDataJSON(); expect(payload.confirmed).toBe(true); expect(payload.questionIds.sort()).toEqual([101, 102]); expect(request.headers().authorization).toBe('Bearer teacher-discussion');
      if (requests === 1) { key = payload.clientKey; return route.fulfill({ status: 503, json: { detail: 'Falha de conexão após salvar. Tente novamente.' } }); }
      expect(payload.clientKey).toBe(key); sent = true; return route.fulfill({ json: { id: 'batch-result', questionIds: [101, 102], createdAt: '2026-10-29T10:01:00Z' } });
    }
    return common(route, true);
  });
  await page.goto('/#capacita'); await page.getByRole('button', { name: 'Dúvidas recebidas', exact: true }).click();
  await page.getByLabel('Selecionar dúvida 101').check(); await page.getByLabel('Selecionar dúvida 102').check();
  await page.getByLabel('Mensagem ou descrição textual').fill('Uma resposta geral revisada pelo professor.'); expect(requests).toBe(0);
  await page.getByRole('button', { name: 'Revisar resposta e destinatários' }).click();
  await expect(page.getByRole('region', { name: 'Revisão do envio em lote' })).toContainText('Aluno 2'); await expect(page.getByRole('region', { name: 'Revisão do envio em lote' })).toContainText('Aluno 3');
  const submit = page.getByRole('button', { name: 'Enviar resposta em lote', exact: true }); await expect(submit).toBeDisabled();
  await page.getByLabel('Confirmo a resposta e todas as dúvidas destinatárias.').check(); await submit.click();
  await expect(page.getByRole('alert')).toContainText('Falha de conexão após salvar'); expect(requests).toBe(1);
  await submit.click(); await expect(page.getByText(/Resposta enviada a 2 dúvidas/)).toBeVisible(); expect(requests).toBe(2);
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('company-independent');
});

test('aluno envia texto, recupera notificação na reconexão e abre sua resposta', async ({ page }) => {
  await session(page); let created = false; let notified = false; let read = false; let followUp = false; const row = question();
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request(); const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/notifications/stream')) { let event = ''; if (created && !notified) { notified = true; event = 'event: notifications\nid: 9\ndata: {"items":[{"id":9,"question_id":101,"kind":"REPLY"}]}\n\n'; } return route.fulfill({ contentType: 'text/event-stream', body: 'event: ready\ndata: {"connected":true}\n\n' + event }); }
    if (pathname.endsWith('/notifications')) return route.fulfill({ json: { items: notified ? [{ id: 9, question_id: 101, kind: 'REPLY', read_at: read ? '2026-10-29T10:02:00Z' : null, created_at: '2026-10-29T10:01:00Z' }] : [], unreadCount: notified && !read ? 1 : 0 } });
    if (pathname.endsWith('/notifications/9/read')) { read = true; return route.fulfill({ status: 204 }); }
    if (pathname.endsWith('/discussion/questions') && request.method() === 'POST') { const payload = request.postDataJSON(); expect(payload.courseId).toBe(10); expect(payload.moduleId).toBe(30); expect(payload.body).toBe('Minha dúvida por texto.'); created = true; return route.fulfill({ json: { id: 101 } }); }
    if (pathname.endsWith('/discussion/questions')) return route.fulfill({ json: created ? [{ ...row, status: notified ? 'ANSWERED' : 'WAITING' }] : [] });
    if (pathname.endsWith('/discussion/questions/101')) return route.fulfill({ json: { ...row, status: followUp ? 'WAITING' : notified ? 'ANSWERED' : 'WAITING' } });
    if (pathname.endsWith('/questions/101/messages') && request.method() === 'POST') { followUp = true; return route.fulfill({ json: { id: 3 } }); }
    if (pathname.endsWith('/questions/101/messages')) return route.fulfill({ json: [{ id: 1, question_id: 101, author_name: 'Aluno', author_role: 'STUDENT', body: 'Minha dúvida por texto.', created_at: row.created_at }, ...(notified ? [{ id: 2, question_id: 101, author_name: 'Professor', author_role: 'TEACHER', body: 'Resposta privada do professor.', created_at: row.created_at }] : []), ...(followUp ? [{ id: 3, question_id: 101, author_name: 'Aluno', author_role: 'STUDENT', body: 'Obrigada, tenho um acompanhamento.', created_at: row.created_at }] : [])] });
    return common(route);
  });
  await page.goto('/#capacita'); await page.getByRole('button', { name: 'Conhecer curso' }).click(); await page.getByRole('button', { name: 'Módulo de teste', exact: true }).click(); await page.getByRole('button', { name: 'Dúvidas deste módulo' }).click();
  await page.getByLabel('Título da dúvida').fill('Dúvida privada'); await page.getByLabel('Mensagem ou descrição textual').fill('Minha dúvida por texto.'); await page.getByRole('button', { name: 'Enviar dúvida', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Notificações (1)', exact: true })).toBeVisible(); await page.getByRole('button', { name: 'Notificações (1)', exact: true }).click(); await page.getByRole('button', { name: /Resposta recebida/ }).click();
  await expect(page.getByText('Resposta privada do professor.', { exact: true })).toBeVisible(); await expect(page.getByRole('button', { name: 'Notificações (0)', exact: true })).toBeVisible();
  await page.getByLabel('Mensagem ou descrição textual').fill('Obrigada, tenho um acompanhamento.'); await page.getByRole('button', { name: 'Enviar acompanhamento', exact: true }).click(); await expect(page.getByText('Obrigada, tenho um acompanhamento.', { exact: true })).toBeVisible();
});

for (const kind of ['AUDIO', 'VIDEO'] as const) {
  test(`aluno envia ${kind} com descrição pelo celular`, async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 }); await session(page); let uploaded = false; let created = false; const row = question(); const asset = '11111111-1111-4111-8111-111111111111';
    await page.route('http://localhost:8080/api/**', async route => {
      const request = route.request(); const pathname = new URL(request.url()).pathname;
      if (pathname.endsWith('/discussion/media')) { expect(new URL(request.url()).searchParams.get('kind')).toBe(kind); expect(request.headers().authorization).toBe('Bearer student-discussion'); expect(request.headers()['content-type']).toContain('multipart/form-data'); uploaded = true; return route.fulfill({ json: { id: asset, kind, durationSeconds: 3 } }); }
      if (pathname.endsWith('/discussion/questions') && request.method() === 'POST') { expect(uploaded).toBe(true); expect(request.postDataJSON().mediaId).toBe(asset); expect(request.postDataJSON().body).toBe('Descrição textual da mídia.'); created = true; return route.fulfill({ json: { id: 101 } }); }
      if (pathname.endsWith('/discussion/questions')) return route.fulfill({ json: created ? [row] : [] });
      if (pathname.endsWith('/discussion/questions/101')) return route.fulfill({ json: row });
      if (pathname.endsWith('/questions/101/messages')) return route.fulfill({ json: [{ id: 1, question_id: 101, author_role: 'STUDENT', author_name: 'Aluno', body: 'Descrição textual da mídia.', media_id: asset, media_kind: kind, created_at: row.created_at }] });
      return common(route);
    });
    await page.goto('/#capacita'); await page.getByRole('button', { name: 'Conhecer curso' }).click(); await page.getByRole('button', { name: 'Dúvidas deste curso' }).click();
    await page.getByLabel('Título da dúvida').fill('Minha gravação'); await page.getByLabel('Mensagem ou descrição textual').fill('Descrição textual da mídia.'); await page.getByRole('combobox', { name: 'Tipo de gravação', exact: true }).selectOption(kind);
    await page.getByLabel('Ou enviar arquivo').setInputFiles(path.join(fixtures, kind === 'AUDIO' ? 'question-audio.wav' : 'question-video.mp4'));
    await expect(page.getByLabel(kind === 'AUDIO' ? 'Prévia do áudio da mensagem' : 'Prévia do vídeo da mensagem')).toBeVisible();
    await page.getByRole('button', { name: 'Enviar dúvida', exact: true }).click(); await expect(page.getByRole('button', { name: kind === 'AUDIO' ? 'Ouvir áudio' : 'Assistir vídeo da mensagem', exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  });
}

test.describe('captura pelo navegador', () => {
  test('grava áudio e vídeo com alternativa de arquivo preservada', async ({ page }) => {
    await session(page); await page.route('http://localhost:8080/api/**', route => new URL(route.request().url()).pathname.endsWith('/discussion/questions') ? route.fulfill({ json: [] }) : common(route));
    await page.goto('/#capacita'); await page.getByRole('button', { name: 'Conhecer curso' }).click(); await page.getByRole('button', { name: 'Dúvidas deste curso' }).click();
    for (const kind of ['AUDIO', 'VIDEO']) {
      await page.getByRole('combobox', { name: 'Tipo de gravação', exact: true }).selectOption(kind); await page.getByRole('button', { name: kind === 'AUDIO' ? 'Gravar áudio' : 'Gravar vídeo', exact: true }).click();
      const stop = page.getByRole('button', { name: /Parar gravação/ }); await expect(stop).toContainText('2s'); await stop.click();
      await expect(page.getByLabel(kind === 'AUDIO' ? 'Prévia do áudio da mensagem' : 'Prévia do vídeo da mensagem')).toBeVisible(); await page.getByRole('button', { name: 'Remover anexo' }).click();
    }
    await expect(page.getByLabel('Ou enviar arquivo')).toBeEnabled();
  });
});
