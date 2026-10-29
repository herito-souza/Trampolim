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

async function openOpportunities(page: import('@playwright/test').Page) {
  await page.goto('/');
  await page.getByRole('button', { name: 'Explorar oportunidades' }).click();
}

test('login abre o diagnóstico e a navegação percorre catálogo e perfil em tela estreita', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockApi(page);
  await openOpportunities(page);
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha', { exact: true }).fill('senha-segura');
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
  await expect(page.getByRole('heading', { name: 'Meu perfil' })).toBeVisible();
  await expectNoSevereAccessibilityIssues(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
});

test('falha de login mostra mensagem acessível sem criar sessão', async ({ page }) => {
  await mockApi(page, 401);
  await openOpportunities(page);
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha').fill('senha-errada');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.getByRole('alert')).toContainText('E-mail ou senha incorretos.');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_token'))).toBeNull();
});

test('cadastro bloqueia senhas divergentes antes de enviar credenciais', async ({ page }) => {
  await mockApi(page);
  const registrationRequests: string[] = [];
  page.on('request', request => {
    if (request.method() === 'POST' && request.url().endsWith('/auth/cadastro')) registrationRequests.push(request.url());
  });
  await openOpportunities(page);
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await page.getByLabel('Nome completo').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha', { exact: true }).fill('senha-segura');
  await page.getByLabel('Confirme sua senha').fill('senha-diferente');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Criar meu perfil' }).click();
  await expect(page.getByRole('alert')).toHaveText('A confirmação da senha não confere.');
  expect(registrationRequests).toHaveLength(0);
});

test('diagnóstico percorre as dez perguntas, permite confirmar e exibe recomendações', async ({ page }) => {
  await openOpportunities(page);
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
  await openOpportunities(page);
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
  await openOpportunities(page);
  await page.getByRole('button', { name: 'Entrar' }).first().click();
  await page.getByLabel('E-mail').fill('pessoa@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  const navigation = page.getByRole('navigation', { name: 'Navegação da conta' });
  await navigation.getByRole('button', { name: 'Oportunidades' }).click();
  await expect(page.getByRole('heading', { name: /Oportunidades para você/ })).toBeVisible();
  await expectNoSevereAccessibilityIssues(page);
});

test('catálogo consulta vagas empresariais sem compartilhar token do candidato', async ({ page }) => {
  await page.addInitScript(() => { localStorage.setItem('trampolim_token', 'candidate-session'); localStorage.setItem('trampolim_profile_step', 'welcome'); });
  await mockApi(page);
  let publicAuthorization: string | undefined;
  await page.route('http://localhost:8080/api/public/companies/opportunities', async route => {
    publicAuthorization = route.request().headers().authorization;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 9, companySlug: 'fixture-company', companyName: 'Empresa de teste', title: 'Vaga empresarial integrada', area: 'Tecnologia', description: 'Anúncio de teste', requirements: 'Requisitos de teste', location: 'Recife', modality: 'Remoto', internship: true }]) });
  });
  await page.goto('/#oportunidades');
  await page.getByRole('navigation', { name: 'Navegação da conta' }).getByRole('button', { name: 'Vagas', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Vaga empresarial integrada' })).toBeVisible();
  expect(publicAuthorization).toBeUndefined();
  await expect(page.getByRole('link', { name: 'Ver vaga na empresa' })).toHaveAttribute('href', '/?empresa=fixture-company');
  await page.getByLabel('Buscar nas vagas empresariais').fill('inexistente');
  await expect(page.getByText('Nenhuma vaga empresarial encontrada.')).toBeVisible();
});

test('administração altera somente a trilha selecionada com justificativa', async ({ page }) => {
  await page.addInitScript(() => { localStorage.setItem('trampolim_token', 'platform-admin-session'); localStorage.setItem('trampolim_profile_step', 'welcome'); });
  await mockApi(page);
  const changes: { path: string; status: string; reason: string }[] = [];
  await page.route('http://localhost:8080/api/admin/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (request.method() === 'PUT' && path.endsWith('/status')) {
      changes.push({ path, ...request.postDataJSON() });
      expect(request.headers().authorization).toBe('Bearer platform-admin-session');
      return route.fulfill({ status: 204 });
    }
    if (path === '/api/admin/trail-accounts/capacita/22/audit') {
      expect(request.headers().authorization).toBe('Bearer platform-admin-session');
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 1, userId: 22, actor: 'admin@example.test', previousStatus: 'ACTIVE', newStatus: 'SUSPENDED', reason: 'Histórico estudantil de teste', createdAt: '2026-10-28T12:00:00Z' }]) });
    }
    if (path.startsWith('/api/admin/trail-accounts/')) {
      const student = path.endsWith('/capacita');
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: student ? 22 : 11, email: student ? 'student@example.test' : 'company@example.test', name: student ? 'Student' : 'Company', status: 'ACTIVE', emailVerified: true, createdAt: '2026-10-28' }]) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: path.endsWith('/overview') ? JSON.stringify({ users: 0, verifiedUsers: 0, activeUsers30d: 0, companies: 0, associations: 0, associationLinks: 0, publishedJobs: 0, publishedInternships: 0, publishedLearningContents: 0, applications: 0, pendingPublications: 0, paidInternshipPayments: 0, internshipRevenueCents: 0, revenueByMonth: [] }) : path.endsWith('/preview') ? '{}' : '[]' });
  });
  await page.goto('/#oportunidades');
  await page.getByRole('button', { name: 'Gestão SaaS', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Contas por trilha' })).toBeVisible();
  await expect(page.getByText(/company@example.test/)).toBeVisible();
  await page.getByLabel('Trilha administrada').selectOption('capacita');
  await expect(page.getByText(/student@example.test/)).toBeVisible();
  await page.getByRole('button', { name: 'Histórico', exact: true }).click();
  await expect(page.getByRole('region', { name: 'Histórico da conta' })).toContainText('Histórico estudantil de teste');
  await page.getByRole('button', { name: 'Fechar histórico' }).click();
  await page.getByRole('button', { name: 'Suspender', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Confirmar alteração' })).toBeDisabled();
  await page.getByLabel('Justificativa', { exact: true }).fill('Revisão de segurança de teste');
  await page.getByRole('button', { name: 'Confirmar alteração' }).click();
  await expect.poll(() => changes.length).toBe(1);
  expect(changes[0]).toEqual({ path: '/api/admin/trail-accounts/capacita/22/status', status: 'SUSPENDED', reason: 'Revisão de segurança de teste' });
});
