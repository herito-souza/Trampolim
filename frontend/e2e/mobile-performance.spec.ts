import {test,expect} from '@playwright/test';
import {activate} from './navigation-helpers';
test('Empresas carrega associação uma vez e apresenta anúncios em lotes sem limitar filtros',async({page})=>{
 await page.setViewportSize({width:375,height:900});await page.addInitScript(()=>localStorage.setItem('trampolim_empresas_token','synthetic-mobile'));
 const calls:string[]=[],modules:string[]=[];page.on('request',r=>modules.push(r.url()));
 await page.route('http://localhost:8080/api/**',async route=>{const path=new URL(route.request().url()).pathname;calls.push(path);if(path.endsWith('/company/dashboard'))return route.fulfill({json:{tradeName:'Empresa sintética isolada',role:'OWNER',city:'Irecê',state:'BA',industry:'Teste'}});if(path.endsWith('/company/listings'))return route.fulfill({json:Array.from({length:45},(_,n)=>({id:n+1,title:'Vaga sintética '+(n+1),kind:'job',area:'Teste',description:'Fixture',requirements:'Teste',location:'Irecê',modality:'Presencial',publicationStatus:'DRAFT',paymentStatus:'NOT_PROCESSED',createdAt:'2026-11-01',views:0,applicationClicks:0,applications:0}))});return route.fulfill({json:[]});});
 await page.goto('/#empresas');await expect(page.getByRole('heading',{name:'Visão geral da empresa'})).toBeVisible();
 await expect.poll(()=>calls.filter(p=>p.endsWith('/company/listings')).length).toBeGreaterThan(0);
 // Vite development runs StrictMode setup/cleanup twice; each consumer must issue no more than that pair.
 expect(calls.filter(p=>p.endsWith('/association/organizations')).length).toBeLessThanOrEqual(2);expect(calls.some(p=>p.endsWith('/commercial-interests'))).toBe(false);expect(modules.some(p=>p.includes('/AssociationDashboardPage.tsx'))).toBe(false);
 await page.getByRole('button',{name:'Gerenciar anúncios',exact:true}).click();await expect(page.locator('#divulgacoes article')).toHaveCount(20);
 const more=page.getByRole('button',{name:'Mostrar mais anúncios',exact:true});await more.focus();await page.keyboard.press('Enter');await expect(page.locator('#divulgacoes article')).toHaveCount(40);
 await page.getByLabel('Cargo, área ou requisito do anúncio').fill('Vaga sintética 45');await expect(page.locator('#divulgacoes article')).toHaveCount(1);await expect(page.locator('#divulgacoes article')).toContainText('Vaga sintética 45');expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
test('entrada do aluno não baixa editor docente, reprodutor, mapas ou PDFs',async({page})=>{
 await page.addInitScript(()=>localStorage.setItem('trampolim_capacita_token','synthetic-student'));const requests:string[]=[];page.on('request',r=>requests.push(r.url()));
 await page.route('http://localhost:8080/api/**',async route=>{const path=new URL(route.request().url()).pathname;if(path.endsWith('/learning/me'))return route.fulfill({json:{id:1,name:'Aluno sintético',teacher:false,admin:false}});if(path.endsWith('/inclusion/preferences'))return route.fulfill({json:{saved:false}});if(path.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});if(path.endsWith('/performance'))return route.fulfill({json:{coursesStarted:0,coursesCompleted:0,lessonsCompleted:0,modulesCompleted:0,assessmentsSubmitted:0,assessmentsPassed:0,competenciesEarned:0,courses:[]}});if(path.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});return route.fulfill({json:[]});});
 await page.goto('/#capacita');await expect(page.getByRole('searchbox',{name:'Buscar cursos'})).toBeVisible();await expect(page.getByRole('heading',{name:'Continuar aprendendo',exact:true})).toBeVisible();expect(requests.filter(p=>/CourseManagement|LessonPlayer|pdf.worker|maplibre|RegionalMap|\.mp4/.test(p))).toEqual([]);await activate(page,'Biblioteca virtual');await expect(page.getByRole('heading',{name:/Biblioteca virtual/})).toBeVisible();
});

test('ferramenta lenta informa espera, acompanha conexão e conclui o carregamento',async({page,context})=>{
 await page.addInitScript(()=>localStorage.setItem('trampolim_empresas_token','synthetic-slow'));
 await page.route('http://localhost:8080/api/**',async route=>{const path=new URL(route.request().url()).pathname;if(path.endsWith('/company/dashboard'))return route.fulfill({json:{tradeName:'Empresa sintética isolada',role:'OWNER',city:'Irecê',state:'BA'}});if(path.includes('/company/challenges'))return route.fulfill({json:{items:[],nextCursor:null}});return route.fulfill({json:[]});});
 let release:()=>void=()=>{};const gate=new Promise<void>(resolve=>{release=resolve});
 await page.route('**/src/features/career/CompanyCareerPanel.tsx*',async route=>{await gate;await route.continue()});
 try{
  await page.goto('/#empresas');await expect(page.getByRole('heading',{name:'Visão geral da empresa'})).toBeVisible();await activate(page,'Desafios e impacto');
  await expect(page.getByText('O carregamento está demorando. Verifique sua conexão; os dados já salvos permanecem disponíveis.',{exact:true})).toBeVisible({timeout:7000});
  await context.setOffline(true);await expect(page.getByText('Sem conexão. O carregamento poderá continuar quando a internet voltar.',{exact:true})).toBeVisible();
  await context.setOffline(false);release();await expect(page.getByRole('heading',{name:'Desafios e impacto',exact:true})).toBeVisible();
 }finally{release();await context.setOffline(false)}
});
