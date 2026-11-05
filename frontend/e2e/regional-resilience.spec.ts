import {test,expect} from '@playwright/test';
const settings={defaultRegion:'irece-ba',referenceMunicipality:{code:'2914604',name:'Irecê',state:'BA'},states:['BA','SP'],regions:[{id:'irece-ba',name:'Irecê e região',municipalityCodes:['2914604','2919157']}]};

for(const residence of ['Irecê/BA','Lapão/BA'])test(`morador de ${residence} pesquisa Irecê sem mudar sua residência ou compartilhar sessões`,async({page})=>{
 await page.setViewportSize({width:375,height:844});
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','synthetic-regional-candidate');localStorage.setItem('trampolim_profile_step','catalog-jobs');localStorage.setItem('trampolim_capacita_token','synthetic-student-preserved')});
 const searches:URLSearchParams[]=[];let writes=0;
 await page.route('http://localhost:8080/api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),p=url.pathname;
  if(req.method()!=='GET'&&p.startsWith('/api/profile'))writes++;
  if(p.endsWith('/geography/settings'))return route.fulfill({json:settings});
  if(p.endsWith('/geography/municipalities'))return route.fulfill({json:[settings.referenceMunicipality]});
  if(p==='/api/profile')return route.fulfill({json:{profile:{name:'Candidato sintético QA',email:'qa@example.test',location:residence},experiences:[],educations:[]}});
  if(p==='/api/profile/diagnostic')return route.fulfill({json:null});
  if(p==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Fixture denied'}});
  if(p.endsWith('/regional')||p.endsWith('/regional-opportunities')){searches.push(url.searchParams);return route.fulfill({json:{items:[],nextCursor:null}})}
  return route.fulfill({json:[]});
 });
 await page.goto('/#oportunidades');
 await page.getByRole('combobox',{name:'Onde você quer encontrar oportunidades?'}).selectOption('city');
 await page.getByLabel('Buscar município pelo nome').fill('Irecê');
 await page.getByRole('button',{name:'Irecê — BA',exact:true}).click();
 await expect.poll(()=>searches.some(q=>q.get('municipalityCode')==='2914604')).toBe(true);
 expect(writes).toBe(0);expect(await page.evaluate(()=>localStorage.getItem('trampolim_capacita_token'))).toBe('synthetic-student-preserved');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});

test('busca em rede móvel lenta recupera erro de API e interrupção sem perder filtros',async({page,context})=>{
 await page.setViewportSize({width:375,height:844});
 const cdp=await context.newCDPSession(page);await cdp.send('Network.enable');
 let failure:'api'|'network'|null='api';let attempts=0;
 await page.route('http://localhost:8080/api/**',async route=>{
  const url=new URL(route.request().url());
  if(url.pathname.endsWith('/geography/settings'))return route.fulfill({json:settings});
  if(url.pathname.endsWith('/regional-opportunities')){
   attempts++;await new Promise(resolve=>setTimeout(resolve,350));
   if(failure==='api')return route.fulfill({status:503,json:{detail:'Falha sintética de homologação'}});
   if(failure==='network')return route.abort('internetdisconnected');
   expect(url.searchParams.get('q')).toBe('auxiliar QA');expect(url.searchParams.get('municipalityCode')).toBe('2914604');
   return route.fulfill({json:{items:[{id:100,title:'Vaga sintética QA — não é oferta real',companyName:'Empresa sintética QA',companySlug:'qa',area:'Administração',location:'Irecê/BA',modality:'Presencial',description:'Fixture exclusiva do teste',requirements:'Fixture'}],nextCursor:null}});
  }
  return route.fulfill({json:[]});
 });
 await page.goto('/#inicio');await expect(page.getByRole('heading',{level:1})).toBeVisible();await cdp.send('Network.emulateNetworkConditions',{offline:false,latency:350,downloadThroughput:400000/8,uploadThroughput:150000/8,connectionType:'cellular3g'});await page.getByLabel('Cargo ou área profissional').fill('auxiliar QA');await page.getByRole('button',{name:'Pesquisar oportunidades',exact:true}).click();
 await expect(page.getByRole('alert')).toContainText('Não foi possível carregar a pesquisa');
 failure='network';await page.getByRole('button',{name:'Tentar novamente',exact:true}).click();await expect.poll(()=>attempts).toBe(2);await expect(page.getByRole('alert')).toContainText('Não foi possível carregar a pesquisa');
 failure=null;await page.getByRole('button',{name:'Tentar novamente',exact:true}).click();await expect(page.getByRole('heading',{name:'Vaga sintética QA — não é oferta real',exact:true})).toBeVisible();
 await expect(page.getByLabel('Cargo ou área profissional')).toHaveValue('auxiliar QA');await expect(page.getByRole('combobox',{name:'Município',exact:true})).toHaveValue('city');expect(attempts).toBe(3);
});
