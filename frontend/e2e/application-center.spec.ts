import {expect,test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

for(const width of [320,375,768,1440]){
 test(`Central de Candidaturas respeita consentimento, timeline e largura em ${width}px`,async({page})=>{
  await page.setViewportSize({width,height:920});
  await page.addInitScript(()=>{localStorage.setItem('trampolim_token','candidate-center-fixture');localStorage.setItem('trampolim_profile_step','applications')});
  const queries:Record<string,unknown>[]=[];
  await page.route('http://localhost:8080/api/**',async route=>{
   const request=route.request(),path=new URL(request.url()).pathname;
   if(path==='/api/profile')return route.fulfill({json:{profile:{name:'Candidata de teste',email:'verified@example.test',location:'Irecê/BA',phone:null,summary:null},experiences:[],educations:[]}});
   if(path==='/api/profile/applications'&&request.method()==='GET')return route.fulfill({json:[{id:41,itemId:8,title:'Assistente de teste',provider:'Empresa local de teste',location:'Irecê/BA',url:'https://example.test',status:'APPLIED',createdAt:'2026-11-04T12:00:00Z',updatedAt:'2026-11-04T12:00:00Z',notes:'Nota exclusivamente pessoal',followUpDate:null}]});
   if(path==='/api/profile/applications/center'&&request.method()==='POST'){
    const body=request.postDataJSON() as Record<string,unknown>;queries.push(body);
    return route.fulfill({json:{items:[{id:17,listingId:9,listingTitle:'Auxiliar administrativo',companyName:'Comércio Regional',publicSlug:'comercio-regional',location:'Irecê/BA',status:'INTERVIEW',createdAt:'2026-11-02T10:00:00Z',statusUpdatedAt:'2026-11-08T11:30:00Z',timeline:[{id:1,eventType:'RECEIVED',previousStatus:null,status:'RECEIVED',createdAt:'2026-11-02T10:00:00Z'},{id:2,eventType:'STAGE_CHANGED',previousStatus:'RECEIVED',status:'IN_REVIEW',createdAt:'2026-11-04T09:00:00Z'},{id:3,eventType:'STAGE_CHANGED',previousStatus:'IN_REVIEW',status:'INTERVIEW',createdAt:'2026-11-08T11:30:00Z'}]}],page:0,size:10,totalElements:1,hasNext:false}});
   }
   if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Sem acesso'}});
   return route.fulfill({json:[]});
  });
  await page.goto('/#oportunidades/applications');
  await expect(page.getByRole('heading',{name:'Central de Candidaturas',exact:true})).toBeVisible();
  expect(queries).toHaveLength(0);
  const consent=page.getByRole('checkbox',{name:/Autorizo uma consulta pontual/});await consent.check();
  await page.getByRole('button',{name:'Buscar candidaturas enviadas',exact:true}).click();
  await expect(page.getByText('Etapa atualizada pela empresa · etapa atual', {exact:true})).toBeVisible();
  await expect(page.getByText('Candidatura recebida',{exact:true})).toBeVisible();
  await expect(page.getByText('Observação interna confidencial',{exact:true})).toHaveCount(0);
  await expect(page.getByText('Nota exclusivamente pessoal',{exact:true})).toHaveCount(0);
  await page.getByLabel('Empresa',{exact:true}).fill('regional');
  await page.getByLabel('Vaga',{exact:true}).fill('administrativo');
  await page.getByLabel('De',{exact:true}).fill('2026-11-01');
  await page.getByLabel('Até',{exact:true}).fill('2026-11-30');
  await page.getByRole('combobox',{name:'Situação'}).selectOption('INTERVIEW');
  await page.getByRole('button',{name:'Aplicar filtros e atualizar',exact:true}).click();
  await expect.poll(()=>queries.length).toBe(2);
  expect(queries.at(-1)).toMatchObject({status:'INTERVIEW',company:'regional',job:'administrativo',from:'2026-11-01',to:'2026-11-30',page:0,size:10,confirmedCompanyLookup:true});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  const axe=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa']).analyze();expect(axe.violations).toEqual([]);
 });
}

test('resposta sem autorização mostra erro e preserva o registro pessoal',async({page})=>{
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','candidate-center-error');localStorage.setItem('trampolim_profile_step','applications')});
 await page.route('http://localhost:8080/api/**',route=>{const path=new URL(route.request().url()).pathname;if(path==='/api/profile')return route.fulfill({json:{profile:{name:'Pessoa',email:'verified@example.test'},experiences:[],educations:[]}});if(path==='/api/profile/applications')return route.fulfill({json:[]});if(path==='/api/profile/applications/center')return route.fulfill({status:403,json:{detail:'Consulta não autorizada.'}});if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Sem acesso'}});return route.fulfill({json:[]})});
 await page.goto('/#oportunidades/applications');await page.getByRole('checkbox',{name:/Autorizo uma consulta pontual/}).check();await page.getByRole('button',{name:'Buscar candidaturas enviadas',exact:true}).click();await expect(page.getByRole('alert')).toContainText('Consulta não autorizada.');await expect(page.getByRole('heading',{name:'Meu acompanhamento pessoal',exact:true})).toBeVisible();
});
