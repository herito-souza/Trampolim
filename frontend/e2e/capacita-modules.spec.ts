import {activate,prepareNavigation} from './navigation-helpers';
import { test, expect } from '@playwright/test';

for (const width of [1280, 390]) {
  test(`Capacita separa módulos e preserva o curso em ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.addInitScript(() => localStorage.setItem('trampolim_capacita_token', 'module-student'));
    const course = { id: 10, title: 'Curso de navegação', description: 'Curso sintético', category: 'Teste', teacher_name: 'Professor', status: 'PUBLISHED', subjects: [] };
    await page.route('http://localhost:8080/api/**', async route => {
      const path = new URL(route.request().url()).pathname;
      if (path.endsWith('/learning/me')) return route.fulfill({ json: { id: 2, teacher: false, admin: false } });
      if (path.endsWith('/learning/courses')) return route.fulfill({ json: [course] });
      if (path.endsWith('/courses/10')) return route.fulfill({ json: course });
      if (path.endsWith('/notifications')) return route.fulfill({ json: { items: [], unreadCount: 0 } });
      if (path.endsWith('/stream')) return route.fulfill({ contentType: 'text/event-stream', body: 'event: ready\ndata: {}\n\n' });
      if (path.endsWith('/inclusion/preferences')) return route.fulfill({ json: { saved: false } });
      if (path.endsWith('/performance')) return route.fulfill({ json: { courses: [], competencies: [] } });
      return route.fulfill({ json: [] });
    });
    await page.goto('/#capacita');
    const modules = page.getByRole('navigation', { name: 'Módulos do Capacita', exact: true });
    const tools = page.getByRole('navigation', { name: 'Funcionalidades de aprendizagem' });
    await prepareNavigation(page,'Aprendizagem','Módulos do Capacita');
    await expect(modules.getByRole('button', { name: 'Aprendizagem', exact: true })).toHaveAttribute('aria-current', 'page');
    await expect(tools.getByRole('button', { name: 'Administração', exact: true })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Gerenciar cursos', exact: true })).toHaveCount(0);
    if(width<768)await page.keyboard.press('Escape');
    await page.getByRole('button', { name: 'Conhecer curso' }).click();
    await expect(page.getByRole('heading', { name: course.title, exact: true })).toBeVisible();
    await activate(page,'Biblioteca virtual','Funcionalidades de aprendizagem');
    await expect(page.getByRole('region', { name: 'Biblioteca virtual', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: course.title, exact: true })).not.toBeVisible();
    await activate(page,'Mini provas','Funcionalidades de aprendizagem');
    await expect(page.getByRole('region', { name: 'Biblioteca virtual', exact: true })).toHaveCount(0);
    await expect(page.getByRole('region', { name: 'Avaliações educacionais' })).toBeVisible();
    await activate(page,'Cursos e videoaulas','Funcionalidades de aprendizagem');
    await expect(page.getByRole('heading', { name: course.title, exact: true })).toBeVisible();
    await activate(page,'Minha conta','Módulos do Capacita');
    await expect(page.getByRole('heading', { name: 'Minha conta e privacidade' })).toBeVisible();
    await expect(page.getByRole('button',{name:'Minha conta',exact:true,includeHidden:true})).toHaveAttribute('aria-current','page');
    await activate(page,'Preparação profissional','Módulos do Capacita');
    await expect(page.getByRole('heading', { name: 'Vagas de estágio', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Minha conta e privacidade' })).not.toBeVisible();
    await activate(page,'Conteúdos por etapa','Módulos do Capacita');
    await expect(page.getByRole('searchbox', { name: 'Buscar conteúdo' })).toBeVisible();
    const stages = page.locator('#conteudos');
    await stages.getByRole('button', { name: 'Ensino superior', exact: true }).click();
    await expect(stages.getByRole('button', { name: 'Ensino superior', exact: true })).toHaveAttribute('aria-pressed', 'true');
    await prepareNavigation(page,'Visão geral','Módulos do Capacita');
    const first = modules.getByRole('button', { name: 'Visão geral', exact: true });
    await first.focus(); await page.keyboard.press('ArrowRight');
    await expect(modules.getByRole('button', { name: 'Aprendizagem', exact: true })).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('heading', { name: course.title, exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    if (width === 390) await page.screenshot({ path: 'test-results/capacita-modulos-mobile.png', fullPage: true });
  });
}
