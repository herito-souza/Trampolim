import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {activate} from './navigation-helpers';
import type {Page} from '@playwright/test';
import {mkdir} from 'node:fs/promises';
const evidence='../ops/navigation-2026-11-05';
async function fixture(page:Page,role:'candidate'|'admin'|'owner'|'analyst'|'student'|'teacher'){
 await page.addInitScript(({role})=>{if(['candidate','admin'].includes(role)){localStorage.setItem('trampolim_token','synthetic-candidate');localStorage.setItem('trampolim_profile_step','opportunities')}else if(['owner','analyst'].includes(role))localStorage.setItem('trampolim_empresas_token','synthetic-company');else localStorage.setItem('trampolim_capacita_token','synthetic-student')}, {role});
 const calls:string[]=[];
 await page.route('http://localhost:8080/api/**',async route=>{const request=route.request(),p=new URL(request.url()).pathname;calls.push(p);
 if(p==='/api/profile')return route.fulfill({json:{profile:{name:'Pessoa sintética QA',email:'qa@example.test',location:'Irecê/BA'},experiences:[],educations:[]}});
 if(p==='/api/profile/diagnostic')return route.fulfill({json:null});
 if(p==='/api/admin/saas/overview')return route.fulfill(role==='admin'?{json:{}}:{status:403,json:{detail:'Acesso restrito'}});
 if(p==='/api/company/dashboard')return route.fulfill({json:{tradeName:'Empresa sintética QA',legalName:'Fixture',cnpj:'11222333000181',phone:'000',city:'Irecê',state:'BA',industry:'Fixture',role:role==='analyst'?'ANALYST':'OWNER'}});
 if(p.endsWith('/learning/me'))return route.fulfill({json:{id:1,name:'Estudante sintético QA',teacher:role==='teacher',admin:false}});
 if(p.endsWith('/learning/courses'))return route.fulfill({json:[]});
 if(p.endsWith('/age-declaration'))return route.fulfill({json:{ageBand:'AGE_18_PLUS'}});
 if(p.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});
 if(p.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
 if(p.endsWith('/performance'))return route.fulfill({json:{courses:[],coursesStarted:0,coursesCompleted:0,lessonsCompleted:0,modulesCompleted:0,assessmentsSubmitted:0,assessmentsPassed:0,competenciesEarned:0}});
 if(p.endsWith('/geography/settings'))return route.fulfill({json:{defaultRegion:'irece-ba',referenceMunicipality:{code:'2914604',name:'Irecê',state:'BA'},states:['BA'],regions:[{id:'irece-ba',name:'Irecê e região',municipalityCodes:['2914604']}]}});
 if(p.endsWith('/regional')||p.endsWith('/regional-opportunities'))return route.fulfill({json:{items:[],nextCursor:null}});
 return route.fulfill({json:[]});});return calls;
}
for(const width of [320,375,768,1024,1440])for(const role of ['candidate','admin','owner','analyst','student','teacher'] as const)test(`navegação SaaS ${role} em ${width}px`,async({page})=>{
 await page.setViewportSize({width,height:900});const calls=await fixture(page,role);const company=['owner','analyst'].includes(role),learning=['student','teacher'].includes(role),area=company?'empresas':learning?'capacita':'oportunidades';
 await page.goto('/#'+area);await expect(page.locator('.workspace-topbar')).toBeVisible();const toggle=page.getByRole('button',{name:'Alternar menu lateral',exact:true});
 await expect(toggle).toHaveAttribute('aria-expanded',width>=1200?'true':'false');
 if(learning)await expect(page.getByRole('heading',{name:'Organize seu aprendizado',exact:true})).toBeVisible();
 if(company){await expect(page.getByRole('heading',{name:'Ações rápidas da empresa',exact:true})).toBeVisible();const bounds=await page.locator('.workspace-content').boundingBox();const hero=await page.locator('.workspace-content main h1').first().boundingBox();expect(hero!.x).toBeGreaterThanOrEqual(bounds!.x-1);}
 if(width===1440&&['owner','candidate','student'].includes(role)){await mkdir(evidence,{recursive:true});await page.screenshot({path:`${evidence}/${role}-1440.png`});}
 if(width===375&&role==='student'){await mkdir(evidence,{recursive:true});await page.screenshot({path:`${evidence}/student-375.png`});}
 if(width<768){await toggle.click();await expect(page.getByRole('dialog',{name:'Menu '+(company?'Empresas':learning?'Capacita':'Oportunidades')})).toBeVisible();await expect(page.getByRole('button',{name:'Fechar menu',exact:true})).toBeFocused();expect(await page.evaluate(()=>document.body.style.overflow)).toBe('hidden');if(width===375&&role==='student')await page.screenshot({path:`${evidence}/drawer-375.png`});}
 const hidden={includeHidden:true,exact:true};
 if(learning){await expect(page.getByRole('button',{name:'Gerenciar cursos',...hidden})).toHaveCount(role==='teacher'?1:0);}
 else if(company){await expect(page.getByRole('button',{name:'Equipe',...hidden})).toHaveCount(role==='owner'?1:0);await expect(page.getByRole('button',{name:'Candidaturas',...hidden})).toHaveCount(role==='owner'?1:0);}
 else{await expect(page.getByRole('button',{name:'Gestão SaaS',...hidden})).toHaveCount(role==='admin'?1:0);}
 if(width<768){await page.keyboard.press('Escape');await expect(toggle).toBeFocused();expect(await page.evaluate(()=>document.body.style.overflow)).not.toBe('hidden');}
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 const identityCalls=calls.filter(p=>p.endsWith('/learning/me')).length;
 const destination=company?'Vagas e cursos':learning?'Minha conta':'Vagas';await activate(page,destination,company?'Navegação empresarial':learning?'Módulos do Capacita':'Navegação da conta');
 await expect(page).toHaveURL(company?/#empresa-divulgacoes/:learning?/#capacita\/account/:/#oportunidades\/catalog-jobs/);
 expect(calls.filter(p=>p.endsWith('/learning/me')).length).toBe(identityCalls);
 if(width<768)await expect(page.locator('dialog.workspace-drawer')).not.toBeVisible();
 await page.reload();await expect(page.locator('.workspace-topbar')).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 if(width===1440){await toggle.click();await expect(toggle).toHaveAttribute('aria-expanded','false');await page.reload();await expect(toggle).toHaveAttribute('aria-expanded','false');}
 if(width===375){const scan=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa']).analyze();expect(scan.violations.map(v=>({id:v.id,nodes:v.nodes.map(n=>n.target)}))).toEqual([]);}

});

test('menu móvel fecha fora, restaura foco e permite voltar no navegador',async({page})=>{
 await page.setViewportSize({width:375,height:844});await fixture(page,'candidate');await page.goto('/#oportunidades');const toggle=page.getByRole('button',{name:'Alternar menu lateral'});await expect(toggle).toBeVisible();await toggle.click();await page.mouse.click(370,400);await expect(page.locator('dialog.workspace-drawer')).not.toBeVisible();await expect(toggle).toBeFocused();
 await activate(page,'Vagas','Navegação da conta');await activate(page,'Cursos','Navegação da conta');await page.goBack();await expect(page).toHaveURL(/#oportunidades\/catalog-jobs/);await expect(page.getByRole('heading',{name:/Vagas e áreas para explorar/})).toBeVisible();
 await page.goBack();await expect(page).toHaveURL(/#oportunidades\/opportunities/);await expect(page.getByRole('heading',{name:'Seu próximo passo',exact:true})).toBeVisible();
 await page.mouse.wheel(0,600);await expect.poll(()=>page.locator('.workspace-topbar').evaluate(el=>Math.round(el.getBoundingClientRect().top))).toBe(0);
 await activate(page,'Sair');expect(await page.evaluate(()=>localStorage.getItem('trampolim_token'))).toBeNull();await expect(page.locator('.workspace-topbar')).toHaveCount(0);
});

test('URL administrativa mantém restrição de conta comum',async({page})=>{await fixture(page,'candidate');await page.goto('/#oportunidades/admin-saas');await expect(page.getByRole('heading',{name:'Acesso restrito',exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Gestão SaaS',includeHidden:true,exact:true})).toHaveCount(0)});

for(const width of [768,1024])test(`tablet mantém botão acessível e não reduz conteúdo ao expandir em ${width}px`,async({page})=>{
 await page.setViewportSize({width,height:900});await fixture(page,'candidate');await page.goto('/#oportunidades');const toggle=page.getByRole('button',{name:'Alternar menu lateral'});await expect(toggle).toBeVisible();const before=await page.locator('.workspace-content').boundingBox();
 await toggle.click();await expect(toggle).toHaveAttribute('aria-expanded','true');await expect(page.locator('.workspace-content')).toHaveCSS('width',`${before!.width}px`);await toggle.click();await expect(toggle).toHaveAttribute('aria-expanded','false');
 await toggle.click();await activate(page,'Cursos','Navegação da conta');await expect(toggle).toHaveAttribute('aria-expanded','false');
});

test('preferências de acessibilidade permanecem utilizáveis no menu da conta em celular',async({page})=>{
 await page.setViewportSize({width:320,height:844});await fixture(page,'student');await page.goto('/#capacita');await expect(page.locator('.workspace-topbar')).toBeVisible();await page.locator('summary[aria-label^="Menu da conta"]').click();await page.getByText('Acessibilidade e preferências',{exact:true}).click();await page.getByLabel('Tamanho da fonte').selectOption('150');await page.getByLabel('Aumentar espaçamento do texto').check();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.keyboard.press('Escape');await expect(page.locator('summary[aria-label^="Menu da conta"]')).toBeFocused();
});

test('URL de aprendizagem e histórico preservam o módulo e rejeitam modo professor em conta de aluno',async({page})=>{
 await page.setViewportSize({width:375,height:844});await fixture(page,'student');await page.goto('/#capacita/learning/assessments');await expect(page.locator('.workspace-title')).toHaveText('Mini provas');await activate(page,'Meu desempenho e competências','Funcionalidades de aprendizagem');await page.goBack();await expect(page.locator('.workspace-title')).toHaveText('Mini provas');await page.reload();await expect(page.locator('.workspace-title')).toHaveText('Mini provas');
 await page.goto('/#capacita/learning/inbox?mode=teacher');await expect(page.getByRole('heading',{name:'Organize seu aprendizado',exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Gerenciar cursos',includeHidden:true,exact:true})).toHaveCount(0);await expect(page.getByRole('button',{name:'Dúvidas recebidas',includeHidden:true,exact:true})).toHaveCount(0);
});

for(const trail of ['oportunidades','empresas','capacita'] as const)test('sessão recusada em '+trail+' não libera privilégios nem encerra outras trilhas',async({page})=>{
 await page.setViewportSize({width:375,height:844});
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','expired-opportunities');localStorage.setItem('trampolim_empresas_token','expired-company');localStorage.setItem('trampolim_capacita_token','expired-student')});
 await page.route('http://localhost:8080/api/**',route=>{const path=new URL(route.request().url()).pathname;if(trail!=='oportunidades'&&path==='/api/profile')return route.fulfill({json:{profile:{name:'Conta sintética mantida',email:'kept@example.test'},experiences:[],educations:[]}});if(trail!=='oportunidades'&&path==='/api/profile/diagnostic')return route.fulfill({json:null});return route.fulfill({status:401,json:{detail:'Sessão de teste expirada'}})});
 await page.goto('/#'+trail);
 if(trail==='oportunidades'){
  await expect.poll(()=>page.evaluate(()=>localStorage.getItem('trampolim_token'))).toBeNull();await expect(page.locator('.workspace-topbar')).toHaveCount(0);
 }else if(trail==='empresas')await expect(page.getByRole('heading',{name:'Acesso empresarial indisponível',exact:true})).toBeVisible();
 else {await expect(page.getByText('Sessão de teste expirada',{exact:true}).first()).toBeVisible();await expect(page.getByRole('button',{name:'Gerenciar cursos',exact:true,includeHidden:true})).toHaveCount(0);}
 await expect(page.getByRole('button',{name:'Gestão SaaS',exact:true,includeHidden:true})).toHaveCount(0);
 if(trail!=='oportunidades')expect(await page.evaluate(()=>localStorage.getItem('trampolim_token'))).toBe('expired-opportunities');
 if(trail!=='empresas')expect(await page.evaluate(()=>localStorage.getItem('trampolim_empresas_token'))).toBe('expired-company');
 if(trail!=='capacita')expect(await page.evaluate(()=>localStorage.getItem('trampolim_capacita_token'))).toBe('expired-student');
});
