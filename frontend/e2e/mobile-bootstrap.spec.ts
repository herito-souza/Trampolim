import {test,expect} from '@playwright/test';
import {createDiagnosticResult} from '../src/features/diagnostic/recommendations';
const profile={profile:{name:'Pessoa sintética',email:'fixture@example.test',location:'Irecê/BA',phone:null,summary:null},experiences:[],educations:[]};
const diagnostic=createDiagnosticResult(Array(10).fill(0));
for(const authorized of [true,false])test('consultas paralelas preservam validação da sessão: '+authorized,async({page})=>{
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','candidate-fixture');localStorage.setItem('trampolim_profile_step','opportunities');localStorage.setItem('trampolim_empresas_token','company-preserved');localStorage.setItem('trampolim_capacita_token','student-preserved')});
 let release:()=>void=()=>{};const gate=new Promise<void>(resolve=>{release=resolve});let diagnosticSeen=false,screenSeen=false;
 page.on('request',request=>{if(request.url().includes('/features/account/OpportunitiesPage.tsx'))screenSeen=true});
 await page.route('http://localhost:8080/api/**',async route=>{
  const path=new URL(route.request().url()).pathname;
  if(path==='/api/profile'){await gate;return route.fulfill(authorized?{json:profile}:{status:401,json:{detail:'Fixture'}})}
  if(path==='/api/profile/diagnostic'){expect(route.request().headers().authorization).toBe('Bearer candidate-fixture');diagnosticSeen=true;return route.fulfill({json:diagnostic})}
  if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Fixture'}});
  return route.fulfill({json:[]});
 });
 try{
  await page.goto('/#oportunidades/opportunities');await expect.poll(()=>diagnosticSeen).toBe(true);await expect.poll(()=>screenSeen).toBe(true);
  expect(await page.evaluate(()=>sessionStorage.getItem('trampolim_diagnostic_result'))).toBeNull();
  await expect(page.getByRole('status').filter({hasText:'Carregando sua sessão'})).toBeVisible();release();
  if(authorized){await expect(page.getByRole('heading',{name:'Seu próximo passo',exact:true})).toBeVisible();expect(await page.evaluate(()=>JSON.parse(sessionStorage.getItem('trampolim_diagnostic_result')||'null'))).toEqual(diagnostic)}
  else{await expect.poll(()=>page.evaluate(()=>localStorage.getItem('trampolim_token'))).toBeNull();expect(await page.evaluate(()=>sessionStorage.getItem('trampolim_diagnostic_result'))).toBeNull();await expect(page.getByRole('heading',{name:'Seu próximo passo',exact:true})).toHaveCount(0)}
  expect(await page.evaluate(()=>localStorage.getItem('trampolim_empresas_token'))).toBe('company-preserved');expect(await page.evaluate(()=>localStorage.getItem('trampolim_capacita_token'))).toBe('student-preserved');
 }finally{release()}
});
for(const failed of [true,false])test('diagnóstico indisponível ou sincronização pendente: '+failed,async({page})=>{
 await page.addInitScript(value=>{localStorage.setItem('trampolim_token','candidate-fixture');localStorage.setItem('trampolim_profile_step','opportunities');sessionStorage.setItem('trampolim_diagnostic_result',JSON.stringify(value))},diagnostic);
 let writes=0;
 await page.route('http://localhost:8080/api/**',async route=>{
  const request=route.request(),path=new URL(request.url()).pathname;
  if(path==='/api/profile')return route.fulfill({json:profile});
  if(path==='/api/profile/diagnostic'){
   if(request.method()==='PUT'){writes++;expect(request.postDataJSON()).toEqual(diagnostic);return route.fulfill({json:diagnostic})}
   return route.fulfill(failed?{status:503,json:{detail:'Fixture'}}:{status:204});
  }
  if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Fixture'}});
  return route.fulfill({json:[]});
 });
 await page.goto('/#oportunidades/opportunities');await expect(page.getByRole('heading',{name:'Seu próximo passo',exact:true})).toBeVisible();expect(writes).toBe(failed?0:1);expect(await page.evaluate(()=>localStorage.getItem('trampolim_token'))).toBe('candidate-fixture');
});
