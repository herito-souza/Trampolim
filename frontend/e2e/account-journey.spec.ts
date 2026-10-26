import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const profile = {
  profile: { name: 'Pessoa de Teste', email: 'pessoa@example.com', phone: null, location: 'Recife, PE', summary: null, resumeFileName: null, photoFileName: null },
  experiences: [], educations: [],
};
const listedJob = {
  id: 81, kind: 'job', title: 'Atendente de teste', provider: 'Empresa exemplo', description: 'Atendimento ao público.', url: 'https://example.com/vaga', area: 'service',
  location: 'Recife, PE', latitude: null, longitude: null, modality: 'Presencial', experienceLevel: null, contractType: 'CLT', workSchedule: 'Integral', salary: null, accessibility: null,
  active: true, moderationStatus: 'APPROVED', expiresAt: null, createdAt: '2026-10-20T12:00:00Z', updatedAt: '2026-10-20T12:00:00Z', createdBy: null, updatedBy: null, reviewedBy: null, publishedAt: '2026-10-20T12:00:00Z',
};

async function mockApi(page: import('@playwright/test').Page, loginStatus = 200) {
  await page.route('http://localhost:8080/api/**', async route => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace('/api', '');
    if (path === '/auth/login') {
      if (loginStatus !== 200) return route.fulfill({ status: loginStatus, contentType: 'application/json', body: JSON.stringify({ detail: 'E-mail ou senha incorretos.' }) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ token: 'e2e-token', user: { name: 'Pessoa de Teste', email: 'pessoa@example.com' }, emailVerified: true }) });
    }
    if (path === '/profile/diagnostic' && route.request().method() === 'GET') return route.fulfill({ status: 204 });
    if (path === '/profile') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(profile) });
    if (path === '/profile/favorites') {
      if (route.request().method() === 'PUT') {
        const payload = JSON.parse(route.request().postData() || '{"ids":[]}') as { ids: string[] };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(payload.ids) });
      }
      return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    }
    if (path === '/profile/applications' && route.request().method() === 'POST') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: 1, itemId: listedJob.id, title: listedJob.title, provider: listedJob.provider, location: listedJob.location, url: listedJob.url, status: 'SAVED', createdAt: listedJob.createdAt, updatedAt: listedJob.updatedAt, notes: null, followUpDate: null }) });
    if (path === '/profile/applications' || path === '/opportunities/course') return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    if (path === '/opportunities/job') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([listedJob]) });
    if (path.endsWith('/reports') || path === '/metrics/catalog') return route.fulfill({ status: 204 });
    if (path === '/profile/alerts') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ enabled: false, areas: [], frequency: 'weekly' }) });
    if (path === '/profile/public') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ shareId: null, enabled: false, showLocation: false, showSummary: false, showExperiences: false, showEducation: false }) });
    return route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
}

async function expectNoSevereAccessibilityIssues(page: import('@playwright/test').Page) {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  expect(result.violations.filter(issue => issue.impact === 'critical' || issue.impact === 'serious')).toEqual([]);
}

test('login abre o diagnóstico e a navegação percorre catálogo e perfil em tela estreita', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockApi(page);
  await page.goto('/');
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.getByRole('navigation', { name: 'Navegação da conta' })).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Navegação da conta' }).getByRole('button', { name: 'Diagnóstico' })).toHaveAttribute('aria-current', 'page');

  const navigation = page.getByRole('navigation', { name: 'Navegação da conta' });
  await navigation.getByRole('button', { name: 'Oportunidades' }).click();
  await expect(page.getByRole('heading', { name: /Oportunidades para você/ })).toBeVisible();
  await expectNoSevereAccessibilityIssues(page);
  await navigation.getByRole('button', { name: 'Vagas' }).click();
  await expect(page.getByRole('heading', { name: /Vagas e áreas para explorar/ })).toBeVisible();
  await expectNoSevereAccessibilityIssues(page);
  await page.getByRole('button', { name: 'Meu perfil' }).click();
  await expect(page.getByRole('heading', { name: 'Monte seu perfil' })).toBeVisible();
  await expectNoSevereAccessibilityIssues(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
});

test('falha de login mostra mensagem acessível sem criar sessão', async ({ page }) => {
  await mockApi(page, 401);
  await page.goto('/');
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha').fill('senha-errada');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.getByRole('alert')).toContainText('E-mail ou senha incorretos.');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_token'))).toBeNull();
});

test('diagnóstico percorre as dez perguntas, permite confirmar e exibe recomendações', async ({ page }) => {
  await page.goto('/');
  const quiz = page.locator('#diagnostico');
  await expect(quiz.getByRole('heading', { name: 'O que você mais gosta de fazer no dia a dia?' })).toBeVisible();
  for (let question = 0; question < 10; question += 1) {
    await quiz.getByRole('group').getByRole('button').first().click();
  }
  await expect(quiz.getByRole('heading', { name: 'Revise suas respostas' })).toBeVisible();
  await quiz.getByRole('button', { name: 'Confirmar diagnóstico' }).click();
  await expect(quiz.getByText('Diagnóstico concluído')).toBeVisible();
  await expect(quiz.getByRole('heading', { name: /Trilha: Atendimento e relacionamento/ })).toBeVisible();
});

test('pessoa salva uma vaga, acompanha a candidatura e sinaliza um anúncio', async ({ page }) => {
  await mockApi(page);
  await page.goto('/');
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  const navigation = page.getByRole('navigation', { name: 'Navegação da conta' });
  await navigation.getByRole('button', { name: 'Vagas' }).click();
  const jobCard = page.getByRole('article').filter({ has: page.getByRole('heading', { name: listedJob.title }) });
  const areaCard = page.getByRole('article').filter({ has: page.getByRole('heading', { name: 'Atendente ou recepcionista' }) });
  await expect(jobCard).toBeVisible();
  await areaCard.getByRole('button', { name: '♡ Salvar no perfil' }).click();
  await expect(areaCard.getByRole('button', { name: '♥ Salvo no perfil' })).toBeVisible();
  await jobCard.getByRole('button', { name: 'Acompanhar candidatura' }).click();
  await expect(jobCard.getByRole('button', { name: '✓ Acompanhando' })).toBeVisible();
  await jobCard.getByRole('button', { name: 'Sinalizar anúncio' }).click();
  await jobCard.getByLabel('Motivo').selectOption('EXPIRED');
  await jobCard.getByLabel('Detalhes (opcional)').fill('O prazo do anúncio parece ter terminado.');
  await jobCard.getByRole('button', { name: 'Enviar aviso' }).click();
  await expect(page.getByText('Obrigado. Seu aviso foi enviado para revisão.')).toBeVisible();
});

test('tela de oportunidades não tem violações graves WCAG A/AA', async ({ page }) => {
  await mockApi(page);
  await page.goto('/');
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  const navigation = page.getByRole('navigation', { name: 'Navegação da conta' });
  await navigation.getByRole('button', { name: 'Oportunidades' }).click();
  await expect(page.getByRole('heading', { name: /Oportunidades para você/ })).toBeVisible();
  await expectNoSevereAccessibilityIssues(page);
});
