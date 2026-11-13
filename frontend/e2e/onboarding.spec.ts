import {expect,test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('novo candidato percorre, adia e retoma o tutorial acessível', async ({page})=>{
 await page.setViewportSize({width:375,height:860});
 await page.addInitScript(()=>{
  localStorage.setItem('trampolim_token','onboarding-candidate-fixture');
  localStorage.setItem('trampolim_profile_step','applications');
  localStorage.setItem('trampolim_onboarding:oportunidades:verified@example.test',JSON.stringify({status:'pending',step:0}));
 });
 await page.route('http://localhost:8080/api/**',route=>{
  const path=new URL(route.request().url()).pathname;
  if(path==='/api/profile')return route.fulfill({json:{profile:{name:'Pessoa Nova',email:'verified@example.test',location:null,phone:null,summary:null},experiences:[],educations:[]}});
  if(path==='/api/profile/diagnostic')return route.fulfill({json:null});
  if(path==='/api/profile/applications')return route.fulfill({json:[]});
  if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Sem acesso'}});
  return route.fulfill({json:[]});
 });
 await page.goto('/#oportunidades/applications');
 const guide=page.getByTestId('onboarding-guide');
 await expect(guide).toBeVisible();
 await expect(guide.getByRole('heading',{name:'Complete seu perfil profissional'})).toBeVisible();
 await guide.getByRole('button',{name:'Próximo'}).click();
 await expect(guide.getByText('Passo 2 de 3')).toBeVisible();
 const axe=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa']).analyze();
 expect(axe.violations).toEqual([]);
 await guide.getByRole('button',{name:'Agora não'}).click();
 await expect(guide).toHaveCount(0);
 await page.getByRole('button',{name:/Menu da conta/}).click();
 await page.getByRole('button',{name:'Ver tutorial inicial'}).click();
 await expect(guide).toBeVisible();
 await expect(guide.getByText('Passo 1 de 3')).toBeVisible();
 await guide.getByRole('button',{name:'Pular tutorial'}).click();
 await expect(guide).toHaveCount(0);
 await page.getByRole('button',{name:/Menu da conta/}).click();
 await page.getByRole('button',{name:'Ver tutorial inicial'}).click();
 await guide.getByRole('button',{name:'Próximo'}).click();
 await guide.getByRole('button',{name:'Próximo'}).click();
 await guide.getByRole('button',{name:'Concluir'}).click();
 await expect(guide).toHaveCount(0);
 await expect.poll(()=>page.evaluate(()=>JSON.parse(localStorage.getItem('trampolim_onboarding:oportunidades:verified@example.test')||'{}').status)).toBe('completed');
});
