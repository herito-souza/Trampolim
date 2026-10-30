import { test, expect } from '@playwright/test';

test('dados públicos do Capacita só são buscados ao abrir o módulo correspondente', async ({ page }) => {
  const calls: string[]=[];
  await page.route('http://localhost:8080/api/**', async route => {
    const url=new URL(route.request().url());calls.push(url.pathname);
    if(url.pathname.endsWith('/inclusion/preferences'))return route.fulfill({json:{saved:false}});
    if(url.pathname.endsWith('/capacita/tracks'))return route.fulfill({json:[{id:1,code:'HIGH_SCHOOL',categories:[]}]});
    return route.fulfill({json:[]});
  });
  await page.goto('/#capacita');await expect(page.getByRole('heading',{name:'Trampolim Capacita',exact:true})).toBeVisible();
  expect(calls.filter(path=>path.endsWith('/capacita/contents')||path.endsWith('/internships'))).toEqual([]);
  await page.getByRole('button',{name:'Conteúdos por etapa',exact:true}).click();
  await expect.poll(()=>calls.filter(path=>path.endsWith('/capacita/contents')).length).toBe(1);
  expect(calls.some(path=>path.endsWith('/internships'))).toBe(false);
  await page.getByRole('button',{name:'Preparação profissional',exact:true}).click();
  await expect.poll(()=>calls.filter(path=>path.endsWith('/internships')).length).toBe(1);
});

test('cursos usam páginas limitadas e busca no servidor sem recarregar identidade', async ({ page }) => {
  await page.addInitScript(()=>localStorage.setItem('trampolim_capacita_token','synthetic-pagination'));
  let identityCalls=0;const queries:URLSearchParams[]=[];
  await page.route('http://localhost:8080/api/**',async route=>{
    const url=new URL(route.request().url());
    if(url.pathname.endsWith('/learning/me')){identityCalls++;return route.fulfill({json:{id:1,teacher:false,admin:false}});}
    if(url.pathname.endsWith('/learning/courses')){
      queries.push(url.searchParams);const offset=Number(url.searchParams.get('offset'));const q=url.searchParams.get('q');
      return route.fulfill({json:Array.from({length:q?1:25},(_,index)=>({id:offset+index+1,title:q?`Resultado ${q}`:`Curso ${offset+index+1}`,teacher_name:'Sintético',category:'Teste',description:'Fixture'}))});
    }
    if(url.pathname.endsWith('/inclusion/preferences'))return route.fulfill({json:{saved:false}});
    if(url.pathname.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});
    if(url.pathname.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
    return route.fulfill({json:[]});
  });
  await page.goto('/#capacita');await expect(page.getByRole('heading',{name:'Curso 1',exact:true})).toBeVisible();
  await expect(page.getByRole('button',{name:'Conhecer curso',exact:true})).toHaveCount(24);
  const initialIdentityCalls=identityCalls;
  await page.getByRole('button',{name:'Próximos cursos',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Curso 25',exact:true})).toBeVisible();
  await page.getByRole('searchbox',{name:'Buscar cursos',exact:true}).fill('Matemática');
  await expect(page.getByRole('heading',{name:'Resultado Matemática',exact:true})).toBeVisible();
  expect(queries.every(query=>query.get('limit')==='25')).toBe(true);
  expect(queries.some(query=>query.get('offset')==='24')).toBe(true);
  expect(queries.at(-1)?.get('offset')).toBe('0');expect(identityCalls).toBe(initialIdentityCalls);
});

test('estágios usam cursor e não duplicam registros ao reabrir a seção', async ({ page }) => {
  const queries:string[]=[];
  let first=true; let releaseFirst=()=>{}; const firstResponse=new Promise<void>(resolve=>{releaseFirst=resolve;});
  await page.route('http://localhost:8080/api/**',async route=>{
    const url=new URL(route.request().url());
    if(url.pathname.endsWith('/internships')){
      queries.push(url.searchParams.get('before')||'first');
      if(first){first=false;await firstResponse;}
      const before=Number(url.searchParams.get('before')||1000);
      return route.fulfill({json:Array.from({length:25},(_,index)=>({id:before-index-1,slug:'synthetic',title:`Estágio ${before-index-1}`,companyName:'Empresa fictícia',area:'Tecnologia',location:'Teste',modality:'Remoto',description:'Fixture',requirements:'Teste'}))});
    }
    return route.fulfill({json:[]});
  });
  await page.goto('/#capacita');
  const modules=page.getByRole('navigation',{name:'Módulos do Capacita',exact:true});
  await modules.getByRole('button',{name:'Preparação profissional',exact:true}).click();
  try { await expect(page.getByText('Carregando estágios…',{exact:true})).toBeVisible(); await expect(page.getByRole('heading',{name:'Nenhuma vaga de estágio disponível agora'})).toHaveCount(0); } finally { releaseFirst(); }
  await expect(page.getByRole('heading',{name:'Estágio 999',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Carregar mais estágios',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Estágio 975',exact:true})).toBeVisible();
  await expect(page.getByRole('button',{name:'Ver vaga e candidatar-se',exact:true})).toHaveCount(48);
  await modules.getByRole('button',{name:'Visão geral',exact:true}).click();
  await modules.getByRole('button',{name:'Preparação profissional',exact:true}).click();
  await expect(page.getByRole('button',{name:'Ver vaga e candidatar-se',exact:true})).toHaveCount(24);
  await expect(page.getByRole('heading',{name:'Estágio 999',exact:true})).toBeVisible();
  expect(queries.at(-1)).toBe('first');
});

test('logout limpa progresso privado visível sem afetar a sessão empresarial', async ({ page }) => {
  await page.addInitScript(()=>{localStorage.setItem('trampolim_capacita_token','synthetic-owner');localStorage.setItem('trampolim_empresas_token','synthetic-other-trail');});
  await page.route('http://localhost:8080/api/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith('/session/logout'))return route.fulfill({status:204});
    if(path.endsWith('/learning/me'))return route.fulfill({json:{id:1,teacher:false,admin:false}});
    if(path.endsWith('/capacita/tracks'))return route.fulfill({json:[{id:1,code:'HIGH_SCHOOL',categories:[{id:1,name:'Categoria sintética',description:'Teste'}]}]});
    if(path.endsWith('/capacita/contents'))return route.fulfill({json:[{id:1,title:'Material de teste',summary:'Fixture',stageCode:'HIGH_SCHOOL',categoryId:1,categoryName:'Categoria sintética'}]});
    if(path.endsWith('/capacita/progress'))return route.fulfill({json:[1]});
    if(path.endsWith('/notifications'))return route.fulfill({json:{items:[],unreadCount:0}});
    if(path.endsWith('/stream'))return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'});
    return route.fulfill({json:[]});
  });
  await page.goto('/#capacita');await page.getByRole('button',{name:'Conteúdos por etapa',exact:true}).click();
  await expect(page.getByRole('button',{name:'Concluído ✓',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Sair do Capacita',exact:true}).click();
  await expect(page.getByRole('button',{name:'Entrar para acompanhar',exact:true})).toBeVisible();
  await expect(page.getByRole('button',{name:'Concluído ✓',exact:true})).toHaveCount(0);
  expect(await page.evaluate(()=>localStorage.getItem('trampolim_empresas_token'))).toBe('synthetic-other-trail');
});
