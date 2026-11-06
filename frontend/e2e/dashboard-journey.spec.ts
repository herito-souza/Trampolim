import {activate} from './navigation-helpers';
import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
const geography={defaultRegion:'irece-ba',referenceMunicipality:{code:'2914604',name:'Irecê',state:'BA'},states:['BA'],regions:[{id:'irece-ba',name:'Irecê e região',municipalityCodes:['2914604']}]};
async function audit(page:import('@playwright/test').Page){expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);expect((await new AxeBuilder({page}).include('main').withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa']).analyze()).violations).toEqual([])}
for(const width of [320,375,768,1024,1440]){
 test(`candidato tem próximo passo, histórico e busca correta em ${width}px`,async({page})=>{
 await page.setViewportSize({width,height:950});await page.addInitScript(()=>{localStorage.setItem('trampolim_token','candidate-dashboard');localStorage.setItem('trampolim_profile_step','opportunities');localStorage.setItem('trampolim_capacita_token','student-preserved')});
 const calls:URLSearchParams[]=[];
 await page.route('http://localhost:8080/api/**',async route=>{const request=route.request(),url=new URL(request.url()),p=url.pathname;
 if(p.endsWith('/geography/settings'))return route.fulfill({json:geography});
 if(p.endsWith('/geography/municipalities')){expect(request.headers().authorization).toBeUndefined();expect(url.searchParams.get('q')).toBe('Irecê');return route.fulfill({json:[geography.referenceMunicipality]})}
 if(p==='/api/profile'){expect(request.headers().authorization).toBe('Bearer candidate-dashboard');return route.fulfill({json:{profile:{name:'Pessoa de teste',email:'fixture@example.test',location:'Irecê/BA',phone:null,summary:null},experiences:[],educations:[]}})}
 if(p==='/api/profile/diagnostic')return route.fulfill({json:null});
 if(p==='/api/profile/applications')return route.fulfill({json:[{id:1,title:'Vaga acompanhada sintética',provider:'Fixture',location:'Irecê',status:'APPLIED',createdAt:'2026-11-04T12:00:00Z'}]});
 if(p==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Fixture'}});
 if(p.endsWith('/regional')||p.endsWith('/regional-opportunities')){calls.push(url.searchParams);return route.fulfill({json:{items:[],nextCursor:null}})}
 return route.fulfill({json:[]});});
 await page.goto('/#oportunidades');await expect(page.getByRole('heading',{name:'Seu próximo passo',exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Completar meu perfil',exact:true})).toBeVisible();await expect(page.getByRole('heading',{name:'Vaga acompanhada sintética',exact:true})).toBeVisible();await audit(page);await page.screenshot({path:`test-results/dashboard-candidate-${width}.png`,fullPage:true});
 await page.getByRole('button',{name:'Buscar vagas em Irecê',exact:true}).focus();await page.keyboard.press('Enter');await expect.poll(()=>calls.some(p=>p.get('municipalityCode')==='2914604')).toBe(true);
 await activate(page,'Oportunidades','Navegação da conta');await page.getByRole('button',{name:'Encontrar vagas remotas',exact:true}).click();await expect.poll(()=>calls.at(-1)?.get('modality')).toBe('Remoto');expect(calls.at(-1)?.has('municipalityCode')).toBe(false);expect(calls.at(-1)?.has('region')).toBe(false);
 expect(await page.evaluate(()=>localStorage.getItem('trampolim_capacita_token'))).toBe('student-preserved');
 });
 test(`empresa filtra anúncios e candidaturas com dados autorizados em ${width}px`,async({page})=>{
 await page.setViewportSize({width,height:950});await page.addInitScript(()=>{localStorage.setItem('trampolim_empresas_token','company-dashboard');localStorage.setItem('trampolim_capacita_token','student-preserved')});
 const listings=[{id:1,kind:'job',title:'Auxiliar sintético',area:'Administração',requirements:'Excel',location:'Irecê/BA',modality:'Presencial',publicationStatus:'DRAFT',paymentStatus:'NOT_REQUIRED',views:2,applicationClicks:1,applications:1},{id:2,kind:'job',title:'Estágio sintético',area:'Comércio',requirements:'Vendas',location:'Lapão/BA',modality:'Presencial',publicationStatus:'AWAITING_PAYMENT',paymentStatus:'PENDING',isInternship:true,views:0,applicationClicks:0,applications:1}];
 await page.route('http://localhost:8080/api/**',async route=>{const request=route.request(),p=new URL(request.url()).pathname;
 if(p.startsWith('/api/company/'))expect(request.headers().authorization).toBe('Bearer company-dashboard');
 if(p==='/api/company/dashboard')return route.fulfill({json:{tradeName:'Empresa sintética',legalName:'Fixture',cnpj:'11222333000181',phone:'000',industry:'Fixture',city:'Irecê',state:'BA',role:'OWNER',publicSlug:'fixture'}});
 if(p==='/api/company/listings')return route.fulfill({json:listings});
 if(p==='/api/company/applications')return route.fulfill({json:[{id:11,listingId:1,listingTitle:'Auxiliar sintético',name:'Pessoa A',email:'a@example.test',message:'Tenho prática de Excel',status:'RECEIVED',kind:'job'},{id:12,listingId:2,listingTitle:'Estágio sintético',name:'Pessoa B',email:'b@example.test',message:'Tenho interesse em vendas',status:'IN_REVIEW',kind:'job'}]});
 return route.fulfill({json:[]});});
 await page.goto('/#empresas');await expect(page.getByRole('heading',{name:'Ações rápidas da empresa',exact:true})).toBeVisible();await audit(page);
 await page.getByRole('button',{name:'Gerenciar anúncios',exact:true}).click();await page.getByLabel('Município/local da divulgação',{exact:true}).fill('irece');await expect(page.getByRole('heading',{name:'Auxiliar sintético',exact:true})).toBeVisible();await expect(page.getByRole('heading',{name:'Estágio sintético',exact:true})).toHaveCount(0);await page.getByRole('button',{name:'Limpar filtros de anúncios',exact:true}).click();await expect(page.getByRole('heading',{name:'Estágio sintético',exact:true})).toBeVisible();
 await activate(page,'Candidaturas','Navegação empresarial');await page.getByLabel('Município/local da vaga',{exact:true}).fill('Irecê');await page.getByLabel('Competência mencionada na candidatura',{exact:true}).fill('excel');await expect(page.getByRole('heading',{name:'Pessoa A',exact:true})).toBeVisible();await expect(page.getByRole('heading',{name:'Pessoa B',exact:true})).toHaveCount(0);await audit(page);await page.screenshot({path:`test-results/dashboard-company-${width}.png`,fullPage:true});
 await page.getByRole('button',{name:'Limpar filtros de candidaturas',exact:true}).click();await expect(page.getByRole('heading',{name:'Pessoa B',exact:true})).toBeVisible();
 await activate(page,'Visão geral','Navegação empresarial');await page.getByRole('button',{name:'Divulgar estágio · R$ 50,00',exact:true}).click();await expect(page.getByRole('heading',{name:'Detalhes do estágio',exact:true})).toBeVisible();await expect(page.getByLabel('Localidade',{exact:true})).toHaveValue('');expect(await page.evaluate(()=>localStorage.getItem('trampolim_capacita_token'))).toBe('student-preserved');
 });
}
for(const width of [320,375,768,1024,1440])for(const teacher of [false,true])test(`Capacita organiza aprendizado e ferramentas com professor=${teacher} em ${width}px`,async({page})=>{
 await page.setViewportSize({width,height:950});await page.addInitScript(()=>{localStorage.setItem('trampolim_capacita_token','learning-dashboard');localStorage.setItem('trampolim_empresas_token','company-preserved')});let performanceCalls=0,historyCalls=0;
 await page.route('http://localhost:8080/api/**',async route=>{const request=route.request(),url=new URL(request.url()),p=url.pathname;
 if(p.startsWith('/api/capacita/learning/'))expect(request.headers().authorization).toBe('Bearer learning-dashboard');
 if(p.endsWith('/learning/me'))return route.fulfill({json:{id:1,name:'Fixture',teacher,admin:false}});
 if(p.endsWith('/learning/courses'))return route.fulfill({json:[{id:1,title:'Curso sintético',description:'Fixture',category:'Tecnologia',teacher_name:'Professor sintético',status:'PUBLISHED'}]});
 if(p.endsWith('/learning/progress')){historyCalls++;return route.fulfill({json:[{course_id:1,lesson_id:1,course_title:'Curso sintético',title:'Aula em andamento',completed_at:null}]})}
 if(p.endsWith('/performance')){performanceCalls++;return route.fulfill({json:{courses:[],coursesStarted:1,coursesCompleted:0,lessonsCompleted:0,modulesCompleted:0,assessmentsSubmitted:0,assessmentsPassed:0,competenciesEarned:0}})}
 if(p.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});
 if(p.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
 return route.fulfill({json:[]});});
 await page.goto('/#capacita');await expect(page.getByRole('heading',{name:'Organize seu aprendizado',exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Retomar meu estudo',exact:true})).toBeVisible();expect(performanceCalls).toBe(0);const initialHistory=historyCalls;await page.getByRole('button',{name:'Tecnologia',exact:true}).click();await expect(page.getByRole('searchbox',{name:'Buscar cursos',exact:true})).toHaveValue('Tecnologia');expect(historyCalls).toBe(initialHistory);await audit(page);
 if(teacher){await activate(page,'Gerenciar cursos');await expect(page.getByRole('heading',{name:'Ensinar e acompanhar',exact:true})).toBeVisible();await page.getByRole('button',{name:'Criar um curso',exact:true}).click();await expect(page.getByRole('button',{name:'Salvar curso',exact:true})).toBeVisible();}
 else{await expect(page.getByRole('button',{name:'Gerenciar cursos',exact:true})).toHaveCount(0);await page.getByRole('button',{name:'Acompanhar progresso e competências',exact:true}).click();await expect.poll(()=>performanceCalls).toBeGreaterThan(0);}
 expect(await page.evaluate(()=>localStorage.getItem('trampolim_empresas_token'))).toBe('company-preserved');
});

for(const analyst of [false,true])test(`painel empresarial respeita ausência de dados e papel analista=${analyst}`,async({page})=>{
 await page.addInitScript(()=>localStorage.setItem('trampolim_empresas_token','restricted-dashboard'));let privateCalls=0;
 await page.route('http://localhost:8080/api/**',async route=>{const p=new URL(route.request().url()).pathname;
 if(p==='/api/company/dashboard')return route.fulfill({json:{tradeName:'Empresa sintética',legalName:'Fixture',cnpj:'11222333000181',phone:'000',industry:'Fixture',city:'Irecê',state:'BA',role:analyst?'ANALYST':'OWNER'}});
 if(p==='/api/company/listings')return route.fulfill(analyst?{json:[]}:{status:503,json:{detail:'Fixture indisponível'}});
 if(p==='/api/company/applications'){privateCalls++;return route.fulfill({json:[]})}
 return route.fulfill({json:[]});});
 await page.goto('/#empresas');await expect(page.getByRole('heading',{name:'Ações rápidas da empresa',exact:true})).toBeVisible();
 if(analyst){await expect(page.getByRole('button',{name:'Publicar uma vaga',exact:true})).toHaveCount(0);await expect(page.getByRole('button',{name:'Analisar candidaturas',exact:true})).toHaveCount(0);await expect(page.getByRole('navigation',{name:'Navegação empresarial',exact:true}).getByRole('button',{name:'Candidaturas',exact:true})).toHaveCount(0);expect(privateCalls).toBe(0)}
 else{const status=page.getByRole('region',{name:'Situação dos anúncios',exact:true});await expect(status).toContainText('—');await expect(page.getByRole('alert')).toContainText('Fixture indisponível');await page.getByRole('button',{name:'Gerenciar anúncios',exact:true}).click();await expect(page.getByRole('heading',{name:'Não foi possível confirmar os anúncios',exact:true})).toBeVisible();}
});
