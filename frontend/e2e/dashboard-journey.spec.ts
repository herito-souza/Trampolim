import {createDiagnosticResult} from '../src/features/diagnostic/recommendations';
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
 let dashboardCalls=0;page.on('response',response=>{if(response.status()===200&&new URL(response.url()).pathname==='/api/company/dashboard')dashboardCalls++});const listings=[{id:1,kind:'job',title:'Auxiliar sintético',area:'Administração',requirements:'Excel',location:'Irecê/BA',modality:'Presencial',publicationStatus:'DRAFT',paymentStatus:'NOT_REQUIRED',views:2,applicationClicks:1,applications:1},{id:2,kind:'job',title:'Estágio sintético',area:'Comércio',requirements:'Vendas',location:'Lapão/BA',modality:'Presencial',publicationStatus:'AWAITING_PAYMENT',paymentStatus:'PENDING',isInternship:true,views:0,applicationClicks:0,applications:1}];
 await page.route('http://localhost:8080/api/**',async route=>{const request=route.request(),p=new URL(request.url()).pathname;
 if(p.startsWith('/api/company/'))expect(request.headers().authorization).toBe('Bearer company-dashboard');
 if(p==='/api/company/dashboard'){return route.fulfill({json:{tradeName:'Empresa sintética',legalName:'Fixture',cnpj:'11222333000181',phone:'000',industry:'Fixture',city:'Irecê',state:'BA',role:'OWNER',publicSlug:'fixture'}})}
 if(p==='/api/company/listings')return route.fulfill({json:listings});
 if(p==='/api/company/applications')return route.fulfill({json:[{id:11,listingId:1,listingTitle:'Auxiliar sintético',name:'Pessoa A',email:'a@example.test',message:'Tenho prática de Excel',status:'RECEIVED',kind:'job'},{id:12,listingId:2,listingTitle:'Estágio sintético',name:'Pessoa B',email:'b@example.test',message:'Tenho interesse em vendas',status:'IN_REVIEW',kind:'job'}]});
 return route.fulfill({json:[]});});
 await page.goto('/#empresas');await expect(page.getByRole('heading',{name:'Ações rápidas da empresa',exact:true})).toBeVisible();expect(dashboardCalls).toBe(1);await audit(page);await page.screenshot({path:'test-results/dashboard-company-overview-'+width+'.png',fullPage:true});
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
 if(p.endsWith('/learning/courses/1'))return route.fulfill({json:{id:1,title:'Curso sintético',status:'PUBLISHED',subjects:[{id:1,sort_order:0,modules:[{id:1,sort_order:0,lessons:[{id:1,module_id:1,title:'Aula em andamento',status:'PUBLISHED',validation_status:'READY',sort_order:0}]}]}]}});
 if(p.endsWith('/learning/progress')){historyCalls++;return route.fulfill({json:[{course_id:1,lesson_id:1,course_title:'Curso sintético',title:'Aula em andamento',completed_at:null}]})}
 if(p.endsWith('/performance')){performanceCalls++;return route.fulfill({json:{courses:[],coursesStarted:1,coursesCompleted:0,lessonsCompleted:0,modulesCompleted:0,assessmentsSubmitted:0,assessmentsPassed:0,competenciesEarned:0}})}
 if(p.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});
 if(p.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
 return route.fulfill({json:[]});});
 await page.goto('/#capacita');await expect(page.getByRole('heading',{name:'Organize seu aprendizado',exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Retomar meu estudo',exact:true})).toBeVisible();await expect.poll(()=>performanceCalls).toBe(1);const initialHistory=historyCalls;await page.getByRole('button',{name:'Tecnologia',exact:true}).click();await expect(page.getByRole('searchbox',{name:'Buscar cursos',exact:true})).toHaveValue('Tecnologia');expect(historyCalls).toBe(initialHistory);expect(performanceCalls).toBe(1);await expect(page.getByRole('heading',{name:'Aula em andamento',exact:true})).toBeVisible();await audit(page);await page.screenshot({path:'test-results/dashboard-learning-'+width+'-'+teacher+'.png',fullPage:true});
 if(teacher){await activate(page,'Gerenciar cursos');await expect(page.getByRole('heading',{name:'Ensinar e acompanhar',exact:true})).toBeVisible();await page.getByRole('button',{name:'Criar um curso',exact:true}).click();await expect(page.getByRole('button',{name:'Salvar curso',exact:true})).toBeVisible();}
 else{await expect(page.getByRole('button',{name:'Gerenciar cursos',exact:true})).toHaveCount(0);await page.getByRole('button',{name:'Acompanhar progresso e competências',exact:true}).click();await expect(page.getByRole('heading',{name:'Histórico de avaliações',exact:true})).toBeVisible();expect(performanceCalls).toBe(1);}
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

test('próximo passo não repete cadastro e diagnóstico concluídos nem inventa candidaturas',async({page})=>{
 const diagnostic=createDiagnosticResult(Array(10).fill(0));let profileCalls=0;
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','complete-candidate');localStorage.setItem('trampolim_profile_step','opportunities')});
 await page.route('http://localhost:8080/api/**',route=>{const p=new URL(route.request().url()).pathname;
 if(p.endsWith('/geography/settings'))return route.fulfill({json:geography});
 if(p.endsWith('/regional'))return route.fulfill({json:{items:[],nextCursor:null}});
 if(p==='/api/profile'){profileCalls++;return route.fulfill({json:{profile:{name:'Pessoa de teste',email:'complete@example.test',phone:'000',location:'Irecê/BA',summary:'Resumo de teste'},experiences:[],educations:[]}})}
 if(p==='/api/profile/diagnostic')return route.fulfill({json:diagnostic});
 if(p==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Sem acesso'}});
 return route.fulfill({json:[]});});
 await page.goto('/#oportunidades');const actions=page.getByRole('region',{name:'Seu próximo passo',exact:true});
 await expect(actions.getByRole('button',{name:'Explorar vagas compatíveis',exact:true})).toBeVisible();
 await expect(actions.getByRole('button',{name:'Completar meu perfil',exact:true})).toHaveCount(0);
 await expect(actions.getByRole('button',{name:'Descobrir meu caminho profissional',exact:true})).toHaveCount(0);
 await expect(actions.getByRole('button',{name:'Acompanhar candidaturas',exact:true})).toHaveCount(0);
 await expect(page.getByText('Você ainda não registrou candidaturas nesta conta.',{exact:true})).toBeVisible();
 // Reentering the dashboard reuses the authenticated profile; StrictMode may cancel a bootstrap request.
 const initialProfileCalls=profileCalls;await actions.getByRole('button',{name:'Consultar cursos disponíveis',exact:true}).click();await expect(page.getByRole('heading',{name:'Catálogo de cursos',exact:true})).toBeVisible();await activate(page,'Oportunidades','Navegação da conta');await expect(page.getByRole('heading',{name:'Seu próximo passo',exact:true})).toBeVisible();expect(profileCalls).toBe(initialProfileCalls);
});
test('Capacita mostra tentativas e notificações reais, retoma aula sem buscar curso novamente',async({page})=>{
 let details=0,notices=0,media=0;
 page.on('response',response=>{if(response.status()===200&&new URL(response.url()).pathname==='/api/capacita/learning/notifications')notices++});
 const lesson={id:9,module_id:5,title:'Próxima aula sintética',sort_order:2,status:'PUBLISHED',validation_status:'READY',duration_seconds:180};
 const course={id:1,title:'Curso sintético',status:'PUBLISHED',subjects:[{id:4,sort_order:0,modules:[{id:5,title:'Módulo',sort_order:0,lessons:[{...lesson,id:8,title:'Aula concluída',sort_order:1,progress:{completed_at:'2026-11-01'}},lesson]}]}]};
 await page.addInitScript(()=>localStorage.setItem('trampolim_capacita_token','overview-student'));
 await page.route('http://localhost:8080/api/**',route=>{const p=new URL(route.request().url()).pathname;
 if(p.endsWith('/learning/me'))return route.fulfill({json:{id:2,name:'Aluno sintético',teacher:false,admin:false}});
 if(p.endsWith('/learning/courses'))return route.fulfill({json:[{...course,subjects:undefined}]});
 if(p.endsWith('/learning/courses/1')){details++;return route.fulfill({json:course})}
 if(p.endsWith('/learning/progress'))return route.fulfill({json:[{course_id:1,lesson_id:8,title:'Aula concluída',completed_at:'2026-11-01'}]});
 if(p.endsWith('/performance'))return route.fulfill({json:{courses:[],coursesStarted:1,coursesCompleted:0,lessonsCompleted:1,modulesCompleted:0,assessmentsSubmitted:0,assessmentsPassed:0,competenciesEarned:2}});
 if(p.endsWith('/assessment-attempts'))return route.fulfill({json:[{id:'attempt-fixture',title:'Prova sintética em andamento',status:'IN_PROGRESS'}]});
 if(p.endsWith('/notifications')){return route.fulfill({json:{unreadCount:1,items:[{id:1,question_id:7,kind:'REPLY',read_at:null,created_at:'2026-11-06T12:00:00Z'}]}})}
 if(p.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
 if(p.endsWith('/lessons/9'))return route.fulfill({json:lesson});
 if(p.endsWith('/lessons/9/playback'))return route.fulfill({json:{sessionId:'fixture',positionSeconds:0}});
 if(p.includes('/media')){media++;return route.fulfill({status:404})}
 return route.fulfill({json:[]});});
 await page.goto('/#capacita');await expect(page.getByRole('heading',{name:'Próxima aula sintética',exact:true})).toBeVisible();await expect(page.getByText('Prova sintética em andamento',{exact:true})).toBeVisible();await expect(page.getByRole('region',{name:'Notificações recentes',exact:true})).toContainText('Resposta recebida');
 expect(details).toBe(1);expect(notices).toBe(1);await page.getByRole('button',{name:'Continuar nesta aula',exact:true}).click();await expect(page.getByRole('heading',{name:'Próxima aula sintética',exact:true})).toBeVisible();expect(details).toBe(1);expect(media).toBe(0);
});
test('falha do resumo educacional não vira progresso zero e permite nova tentativa',async({page})=>{
 let recovered=false;await page.addInitScript(()=>localStorage.setItem('trampolim_capacita_token','offline-summary'));
 await page.route('http://localhost:8080/api/**',route=>{const p=new URL(route.request().url()).pathname;
 if(p.endsWith('/learning/me'))return route.fulfill({json:{id:2,name:'Aluno sintético',teacher:false,admin:false}});
 if(p.endsWith('/performance'))return recovered?route.fulfill({json:{courses:[],coursesStarted:2,coursesCompleted:0,lessonsCompleted:3,modulesCompleted:0,assessmentsSubmitted:0,assessmentsPassed:0,competenciesEarned:1}}):route.fulfill({status:503,json:{detail:'Falha de teste'}});
 if(p.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});
 if(p.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
 return route.fulfill({json:[]});});
 await page.goto('/#capacita');const summary=page.getByRole('region',{name:'Resumo do aprendizado',exact:true});await expect(summary).toContainText('Progresso indisponível.');await expect(summary.getByText('Competências adquiridas:',{exact:true})).toHaveCount(0);
 recovered=true;await summary.getByRole('button',{name:'Atualizar resumo do aprendizado',exact:true}).click();await expect(summary.getByText('Competências adquiridas:',{exact:true})).toBeVisible();await expect(summary.getByRole('definition').last()).toHaveText('1');
});
