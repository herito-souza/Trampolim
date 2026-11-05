import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
for(const width of [390,1440]) test(`busca combinada, expansão e cursos em ${width}px`,async({page})=>{
 await page.setViewportSize({width,height:950});
 const calls:URLSearchParams[]=[];
 await page.addInitScript(()=>localStorage.setItem('trampolim_capacita_token','preserved-student'));
 await page.route('http://localhost:8080/api/**',async route=>{
  const url=new URL(route.request().url());
  if(url.pathname.endsWith('/geography/settings'))return route.fulfill({json:{defaultRegion:'irece-ba',referenceMunicipality:{code:'2914604',name:'Irecê',state:'BA'},states:['BA','SP'],regions:[{id:'irece-ba',name:'Irecê e região',municipalityCodes:['2914604']} ]}});
  if(url.pathname.endsWith('/regional-opportunities')){expect(route.request().headers().authorization).toBeUndefined();calls.push(url.searchParams);const course=url.searchParams.get('kind')==='course';return route.fulfill({json:{items:[{id:url.searchParams.has('before')?9:10,title:course?'Curso sintético para teste':'Vaga sintética para teste',companyName:'Fixture',companySlug:'fixture',area:'Educação',location:'Irecê/BA',modality:'Presencial',description:'Somente teste automatizado',requirements:'Fixture',internship:false}],nextCursor:url.searchParams.has('before')?null:10}})}
  return route.fulfill({json:[]});
 });
 await page.goto('/#inicio');
 await page.getByRole('combobox',{name:'Município',exact:true}).selectOption('state:BA');
 await page.getByLabel('Cargo ou área profissional').fill('auxiliar');
 await page.getByText('Refinar por setor, contratação e requisitos',{exact:true}).click();
 await page.getByRole('combobox',{name:'Setor ou área informada',exact:true}).selectOption('education');
 await page.getByRole('combobox',{name:'Contratação',exact:true}).selectOption('Aprendiz');
 await page.getByRole('combobox',{name:'Escolaridade concluída',exact:true}).selectOption('2');
 await page.getByRole('combobox',{name:'Experiência disponível',exact:true}).selectOption('0');
 await page.getByLabel('Remuneração mínima (R$)',{exact:true}).fill('1000');
 await page.getByLabel('Remuneração máxima (R$)',{exact:true}).fill('2500');
 await page.getByRole('combobox',{name:'Data de publicação',exact:true}).selectOption('7');
 await page.getByRole('button',{name:'Pesquisar oportunidades',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Vaga sintética para teste',exact:true})).toBeVisible();
 for(const [key,value] of Object.entries({state:'BA',area:'education',contractType:'Aprendiz',educationLevel:'2',maxExperienceMonths:'0',minimumSalary:'1000',maximumSalary:'2500',publishedDays:'7'})) expect(calls.at(-1)?.get(key)).toBe(value);
 await page.getByRole('button',{name:'Próxima página de vagas empresariais',exact:true}).click();
 await expect.poll(()=>calls.at(-1)?.get('before')).toBe('10');
 await page.getByRole('button',{name:'Ampliar busca para todas as localidades',exact:true}).click();
 await expect.poll(()=>calls.at(-1)?.has('state')).toBe(false);expect(calls.at(-1)?.has('before')).toBe(false);
 await page.reload();
 await expect(page.getByLabel('Cargo ou área profissional')).toHaveValue('auxiliar');
 await expect(page.getByRole('combobox',{name:'Município',exact:true})).toHaveValue('all');
 await page.getByRole('button',{name:'Limpar filtros',exact:true}).click();
 await expect(page.getByLabel('Cargo ou área profissional')).toHaveValue('');
 await page.getByRole('combobox',{name:'O que você procura?',exact:true}).selectOption('course');
 await page.getByRole('button',{name:'Pesquisar oportunidades',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Curso sintético para teste',exact:true})).toBeVisible();
 expect(calls.at(-1)?.get('kind')).toBe('course');expect(calls.at(-1)?.has('educationLevel')).toBe(false);
 expect(await page.evaluate(()=>localStorage.getItem('trampolim_capacita_token'))).toBe('preserved-student');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze()).violations).toEqual([]);
 await page.screenshot({path:`test-results/search-${width}.png`,fullPage:true});
});

test('raio usa referência arredondada, requer autorização e não persiste coordenadas',async({page,context})=>{
 await context.grantPermissions(['geolocation']);await context.setGeolocation({latitude:-11.3033,longitude:-41.8564});
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','synthetic-candidate');localStorage.setItem('trampolim_profile_step','catalog-jobs')});
 let radius:URLSearchParams|null=null;
 await page.route('http://localhost:8080/api/**',async route=>{
 const url=new URL(route.request().url()),p=url.pathname;
 if(p.endsWith('/geography/settings'))return route.fulfill({json:{defaultRegion:'irece-ba',referenceMunicipality:{code:'2914604',name:'Irecê',state:'BA'},states:['BA'],regions:[{id:'irece-ba',name:'Irecê e região',municipalityCodes:['2914604']}]}});
 if(p==='/api/profile')return route.fulfill({json:{profile:{name:'Fixture',email:'fixture@example.test'},experiences:[],educations:[]}});
 if(p==='/api/profile/diagnostic')return route.fulfill({json:null});
 if(p==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Fixture denied'}});
 if(p.endsWith('/regional')){if(url.searchParams.has('radiusKm'))radius=url.searchParams;return route.fulfill({json:{items:[],nextCursor:null}})}
 if(p.endsWith('/regional-opportunities'))return route.fulfill({json:{items:[],nextCursor:null}});
 return route.fulfill({json:[]});
 });
 await page.goto('/#oportunidades');
 await page.getByRole('button',{name:/Filtros adicionais/}).click();
 await page.getByRole('combobox',{name:'Distância máxima',exact:true}).selectOption('5');
 expect(radius).toBeNull();
 await page.getByRole('button',{name:'Autorizar referência aproximada',exact:true}).click();
 await expect.poll(()=>radius?.get('radiusKm')).toBe('5');expect(radius?.get('latitude')).toBe('-11.3');expect(radius?.get('longitude')).toBe('-41.86');
 await expect(page.getByText('Os anúncios empresariais não possuem coordenadas ou acessibilidade estruturada para estes filtros.',{exact:false})).toBeVisible();
 expect(await page.evaluate(()=>JSON.stringify({...sessionStorage,...localStorage}))).not.toContain('-41.86');
 await page.getByRole('button',{name:'Limpar todos',exact:true}).click();await expect(page.getByRole('combobox',{name:'Distância máxima',exact:true})).toHaveValue('');
});
