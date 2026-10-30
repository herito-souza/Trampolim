import { expect, test } from '@playwright/test';

test('exclusão empresarial trata bloqueio e preserva sessão educacional', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('trampolim_empresas_token', 'company-delete-session');
    localStorage.setItem('trampolim_capacita_token', 'student-kept-session');
  });
  let attempts = 0;
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/empresas/account' && request.method() === 'DELETE') {
      attempts++;
      expect(request.headers().authorization).toBe('Bearer company-delete-session');
      return request.postDataJSON().password === 'correct-password'
        ? route.fulfill({ status: 204 })
        : route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ detail: 'Transfira a responsabilidade antes de excluir seu acesso.' }) });
    }
    if (path === '/api/company/dashboard') return route.fulfill({ status: 403, contentType: 'application/json', body: '{}' });
    return route.fulfill({ status: request.method() === 'POST' ? 204 : 200, contentType: 'application/json', body: request.method() === 'POST' ? '' : '[]' });
  });
  await page.goto('/#empresas');
  await page.getByRole('button', { name: 'Excluir meu acesso empresarial' }).click();
  const confirm = page.getByRole('button', { name: 'Confirmar exclusão' });
  await expect(confirm).toBeDisabled();
  await page.getByLabel('Senha atual').fill('blocked-password');
  await expect(confirm).toBeDisabled();
  await page.getByLabel('Confirmo a exclusão do meu acesso pessoal em Empresas.').check();
  await confirm.click();
  await expect(page.getByRole('alert')).toHaveText('Transfira a responsabilidade antes de excluir seu acesso.');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('company-delete-session');
  await page.getByLabel('Senha atual').fill('correct-password');
  await confirm.click();
  await expect(page.getByRole('button', { name: 'Excluir meu acesso empresarial' })).toHaveCount(0);
  expect(attempts).toBe(2);
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBe('student-kept-session');
});

async function mockLearningApi(page: import('@playwright/test').Page) {
  await page.route('http://localhost:8080/api/**', async route => {
    const path = new URL(route.request().url()).pathname.replace('/api', '');
    if (path === '/capacita/tracks') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 1, code: 'HIGH_SCHOOL', title: 'Ensino médio', description: 'Apoio escolar.', categories: [{ id: 4, name: 'Matemática', description: 'Raciocínio lógico.', sortOrder: 1, contentsCount: 0 }] }, { id: 3, code: 'CAREER_START', title: 'Conclusão do ensino médio e início profissional', description: 'Transição para o trabalho.', categories: [] }]) });
    if (path === '/capacita/contents') return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    if (path === '/public/companies/internships') return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    return route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
}

test('página institucional conecta oportunidades, empresas e Capacita', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockLearningApi(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Um lugar para aprender e avançar/ })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Entrar' })).toHaveCount(0);
  await expect(page.getByText(/Você não precisa ter tudo planejado/)).toHaveCount(0);

  const opportunities = page.locator('article').filter({ hasText: 'Trampolim Oportunidades' });
  await opportunities.hover();
  await expect(opportunities.getByText('Diagnóstico vocacional e recomendações')).toBeVisible();
  await opportunities.getByRole('button', { name: 'Explorar oportunidades' }).click();
  await expect(page.locator('#diagnostico')).toBeVisible();

  await page.goto('/');
  await page.getByRole('button', { name: 'Acessar área empresarial' }).click();
  await expect(page.getByRole('heading', { name: /Conecte sua empresa a novos talentos/ })).toBeVisible();
  await page.getByRole('button', { name: 'Criar conta empresarial' }).click();
  await expect(page.getByText('Dados da empresa')).toBeVisible();

  await page.goto('/');
  await page.getByRole('button', { name: 'Conhecer o Capacita' }).click();
  await expect(page.getByRole('heading', { name: 'Trampolim Capacita' })).toBeVisible();
  await page.getByRole('button', { name: 'Conclusão do ensino médio e início profissional' }).click();
  await page.getByRole('navigation', { name: 'Módulos do Capacita', exact: true }).getByRole('button', { name: 'Preparação profissional', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Vagas de estágio' })).toBeVisible();
  await expect(page.getByText('Nenhuma vaga de estágio disponível agora')).toBeVisible();
  await page.getByRole('button', { name: 'Fazer diagnóstico para o primeiro trabalho' }).click();
  await expect(page.locator('#diagnostico')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
});

test('Empresas e Capacita usam logins e sessões separados, e cada logout preserva a outra trilha', async ({ page }) => {
  const authRequests: string[] = [];
  const logoutRequests: { path: string; authorization: string | null }[] = [];
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace('/api', '');
    if (request.method() === 'POST' && ['/auth/login', '/empresas/auth/login', '/capacita/auth/login'].includes(path)) {
      authRequests.push(path);
      const trail = path.startsWith('/empresas') ? 'empresas' : path.startsWith('/capacita') ? 'capacita' : 'oportunidades';
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ token: `${trail}-session`, user: { name: 'Conta de teste', email: `${trail}@example.com` }, emailVerified: true, accountType: trail === 'empresas' ? 'COMPANY' : trail === 'capacita' ? 'STUDENT' : 'PERSON', accountStatus: 'ACTIVE' }) });
    }
    if (request.method() === 'POST' && ['/auth/logout', '/empresas/session/logout', '/capacita/session/logout'].includes(path)) {
      logoutRequests.push({ path, authorization: request.headers().authorization || null });
      return route.fulfill({ status: 204 });
    }
    if (path === '/profile') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ profile: { name: 'Conta de teste', email: 'oportunidades@example.com', phone: null, location: null, summary: null, resumeFileName: null, photoFileName: null }, experiences: [], educations: [] }) });
    if (path === '/profile/diagnostic') return route.fulfill({ status: 204 });
    if (path === '/empresas/association/organizations') return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    if (path === '/company/dashboard') return route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ detail: 'A conta não possui uma empresa vinculada.' }) });
    if (path === '/capacita/tracks') return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    if (path === '/capacita/contents' || path === '/public/companies/internships' || path === '/capacita/progress') return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    return route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });

  await page.goto('/#empresas');
  await page.getByRole('button', { name: 'Entrar na área empresarial' }).click();
  await page.getByLabel('E-mail corporativo ou do responsável').fill('empresas@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.getByRole('heading', { name: 'Acesso empresarial indisponível' })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('empresas-session');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBeNull();

  await page.goto('/#capacita');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByLabel('E-mail').fill('capacita@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.getByRole('button', { name: 'Sair do Capacita' })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBe('capacita-session');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('empresas-session');

  await page.goto('/#oportunidades');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByLabel('E-mail').fill('oportunidades@example.com');
  await page.getByLabel('Senha').fill('senha-segura');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.getByRole('navigation', { name: 'Navegação da conta' })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_token'))).toBe('oportunidades-session');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('empresas-session');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBe('capacita-session');
  await page.getByRole('button', { name: 'Sair' }).click();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('empresas-session');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBe('capacita-session');

  await page.goto('/#capacita');
  await page.getByRole('button', { name: 'Sair do Capacita' }).click();
  await expect(page.getByRole('button', { name: 'Entrar', exact: true })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('empresas-session');

  await page.goto('/#empresas');
  await page.getByRole('button', { name: 'Sair' }).click();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBeNull();
  expect(authRequests).toEqual(['/empresas/auth/login', '/capacita/auth/login', '/auth/login']);
  expect(logoutRequests).toEqual([
    { path: '/auth/logout', authorization: 'Bearer oportunidades-session' },
    { path: '/capacita/session/logout', authorization: 'Bearer capacita-session' },
    { path: '/empresas/session/logout', authorization: 'Bearer empresas-session' },
  ]);
});

test('indisponibilidade do perfil preserva as sessões e permite tentar novamente', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('trampolim_token', 'opportunities-session');
    localStorage.setItem('trampolim_empresas_token', 'company-session');
    localStorage.setItem('trampolim_capacita_token', 'student-session');
  });
  let unavailable = true;
  await page.route('http://localhost:8080/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/profile') return route.fulfill({ status: unavailable ? 503 : 200, contentType: 'application/json', body: JSON.stringify(unavailable ? { error: 'Temporariamente indisponível' } : { profile: { name: 'Candidate', email: 'candidate@example.test' }, experiences: [], educations: [] }) });
    return route.fulfill({ status: 200, contentType: 'application/json', body: path.includes('organizations') ? '[]' : '{}' });
  });
  await page.goto('/#oportunidades');
  await expect(page.getByRole('heading', { name: 'Não foi possível carregar seu perfil' })).toBeVisible();
  expect(await page.evaluate(() => ['trampolim_token', 'trampolim_empresas_token', 'trampolim_capacita_token'].map(key => localStorage.getItem(key)))).toEqual(['opportunities-session', 'company-session', 'student-session']);
  unavailable = false;
  await page.evaluate(() => localStorage.setItem('trampolim_profile_step', 'welcome'));
  await page.getByRole('button', { name: 'Tentar novamente' }).click();
  await expect(page.getByRole('heading', { name: 'Boas-vindas, Candidate!' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Não foi possível carregar seu perfil' })).toHaveCount(0);
});

test('exportação do Capacita usa apenas a sessão estudantil e baixa JSON', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('trampolim_empresas_token', 'company-private-session');
    localStorage.setItem('trampolim_capacita_token', 'student-private-session');
  });
  let authorization: string | undefined;
  const exported = { trail: 'capacita', account: { email: 'student@example.test' }, progress: [{ content_id: 3 }] };
  await page.route('http://localhost:8080/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/capacita/account/export') {
      authorization = route.request().headers().authorization;
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(exported) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });
  await page.goto('/#capacita');
  await page.getByRole('button', { name: 'Minha conta', exact: true }).click();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Baixar meus dados' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('trampolim-capacita-meus-dados.json');
  const stream = await download.createReadStream();
  const chunks: Buffer[] = [];
  for await (const chunk of stream!) chunks.push(Buffer.from(chunk));
  expect(JSON.parse(Buffer.concat(chunks).toString('utf8'))).toEqual(exported);
  expect(authorization).toBe('Bearer student-private-session');
});

test('exclusão do Capacita exige confirmação e preserva sessão empresarial', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('trampolim_empresas_token', 'company-session');
    localStorage.setItem('trampolim_capacita_token', 'student-session');
  });
  let attempts = 0;
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/api/capacita/account' && request.method() === 'DELETE') {
      attempts++;
      expect(request.headers().authorization).toBe('Bearer student-session');
      const password = request.postDataJSON().password;
      return password === 'correct-password'
        ? route.fulfill({ status: 204 })
        : route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ detail: 'Não foi possível confirmar as credenciais.' }) });
    }
    return route.fulfill({ status: request.method() === 'POST' ? 204 : 200, contentType: 'application/json', body: request.method() === 'POST' ? '' : '[]' });
  });
  await page.goto('/#capacita');
  await page.getByRole('button', { name: 'Minha conta', exact: true }).click();
  await page.getByRole('button', { name: 'Excluir conta do Capacita' }).click();
  const confirm = page.getByRole('button', { name: 'Confirmar exclusão' });
  await expect(confirm).toBeDisabled();
  await page.getByLabel('Senha atual').fill('wrong-password');
  await expect(confirm).toBeDisabled();
  await page.getByLabel('Entendo que a exclusão do Capacita é permanente.').check();
  await confirm.click();
  await expect(page.getByRole('alert')).toHaveText('Não foi possível confirmar as credenciais.');
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBe('student-session');
  await page.getByLabel('Senha atual').fill('correct-password');
  await confirm.click();
  await expect(page.getByRole('button', { name: 'Excluir conta do Capacita' })).toHaveCount(0);
  expect(attempts).toBe(2);
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBe('company-session');
});

test('exportação empresarial usa sessão corporativa mesmo com Capacita conectado', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('trampolim_empresas_token', 'company-export-session');
    localStorage.setItem('trampolim_capacita_token', 'student-export-session');
  });
  const exports: string[] = [];
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path.endsWith('/account/export')) {
      exports.push(path);
      expect(request.headers().authorization).toBe('Bearer company-export-session');
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ trail: 'empresas', account: { email: 'company@example.test' }, companyMemberships: [] }) });
    }
    if (path === '/api/company/dashboard') return route.fulfill({ status: 403, contentType: 'application/json', body: '{}' });
    return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });
  await page.goto('/#empresas');
  const downloaded = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Baixar meus dados' }).click();
  const download = await downloaded;
  expect(download.suggestedFilename()).toBe('trampolim-empresas-meus-dados.json');
  expect(exports).toEqual(['/api/empresas/account/export']);
  expect(await page.evaluate(() => localStorage.getItem('trampolim_capacita_token'))).toBe('student-export-session');
});

test('cadastro Capacita exige faixa etária e envia declaração somente à trilha educacional', async ({ page }) => {
  let registrations = 0;
  await page.route('http://localhost:8080/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/capacita/auth/register') {
      registrations++;
      expect(route.request().postDataJSON().ageBand).toBe('AGE_14_17');
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ token: '', user: { name: 'Student test', email: 'student@example.test' }, emailVerified: false, accountType: 'STUDENT', accountStatus: 'ACTIVE' }) });
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });
  await page.goto('/#capacita');
  await page.getByRole('button', { name: 'Criar conta', exact: true }).click();
  const notice = page.locator('summary', { hasText: 'Aviso de uso e privacidade — Capacita' });
  await notice.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByText('Contas independentes.', { exact: true })).toBeVisible();
  await expect(page.getByText(/Esta conta pertence somente ao Trampolim Capacita/)).toBeVisible();
  await expect(page.getByLabel('Li o aviso de uso e privacidade e concordo com as condições apresentadas para esta área.')).not.toBeChecked();
  await page.getByLabel('Nome completo').fill('Student test');
  await page.getByLabel('E-mail', { exact: true }).fill('student@example.test');
  await page.getByLabel('Senha', { exact: true }).fill('student-password');
  await page.getByLabel('Confirme sua senha').fill('student-password');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Criar meu perfil' }).click();
  await expect(page.getByRole('alert')).toContainText('Selecione sua faixa etária');
  expect(registrations).toBe(0);
  await page.getByRole('combobox', { name: /Faixa etária/ }).selectOption('AGE_14_17');
  await page.getByRole('button', { name: 'Criar meu perfil' }).click();
  await expect.poll(() => registrations).toBe(1);
  expect(await page.evaluate(() => localStorage.getItem('trampolim_token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('trampolim_empresas_token'))).toBeNull();
});

test('conta antiga declara faixa etária somente no Capacita', async ({ page }) => {
  await page.addInitScript(() => { localStorage.setItem('trampolim_capacita_token','student-session'); localStorage.setItem('trampolim_empresas_token','company-session'); });
  let saved: unknown;
  await page.route('http://localhost:8080/api/**', async route => {
    if (new URL(route.request().url()).pathname === '/api/capacita/account/age-declaration') {
      expect(route.request().headers().authorization).toBe('Bearer student-session');
      if (route.request().method() === 'PUT') { saved=route.request().postDataJSON(); return route.fulfill({ status:204 }); }
      return route.fulfill({status:200,contentType:'application/json',body:'{"ageBand":null}'});
    }
    return route.fulfill({status:200,contentType:'application/json',body:'[]'});
  });
  await page.goto('/#capacita');
  await page.getByRole('button', { name: 'Minha conta', exact: true }).click();
  await expect(page.getByRole('button',{name:'Salvar faixa etária'})).toBeDisabled();
  await page.getByLabel('Faixa etária declarada').selectOption('AGE_14_17');
  await page.getByRole('button',{name:'Salvar faixa etária'}).click();
  await expect(page.getByText('Faixa etária declarada salva.')).toBeVisible();
  expect(saved).toEqual({ageBand:'AGE_14_17'});
  expect(await page.evaluate(()=>localStorage.getItem('trampolim_empresas_token'))).toBe('company-session');
});

test('associação mantém painel quando indicador educacional fica indisponível', async ({ page }) => {
  await page.addInitScript(()=>localStorage.setItem('trampolim_empresas_token','association-session'));
  await page.route('http://localhost:8080/api/**', async route => {
    const path=new URL(route.request().url()).pathname;
    if(path==='/api/company/dashboard') return route.fulfill({status:403,contentType:'application/json',body:'{}'});
    if(path==='/api/empresas/association/organizations') return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify([{id:7,name:'Associação de teste',type:'ASSOCIATION',status:'ACTIVE',members:1,linkedCompanies:0,createdAt:'2026-10-28'}])});
    if(path.endsWith('/dashboard')) return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({organizationId:7,name:'Associação de teste',companies:0,activeCompanies:0,openJobs:0,openInternships:0,applications:0,publicLearningContents:null})});
    return route.fulfill({status:200,contentType:'application/json',body:'[]'});
  });
  await page.goto('/#empresas');
  await expect(page.getByRole('heading',{name:'Acompanhamento institucional'})).toBeVisible();
  const metric=page.locator('article').filter({hasText:'Conteúdos de aprendizagem'});
  await expect(metric).toContainText('Indisponível');
  await expect(page.getByRole('heading',{name:'Acompanhamento institucional'})).toBeVisible();
});
