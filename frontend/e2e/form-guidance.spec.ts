import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
for(const width of [320,375,768,1024,1440])test('cadastro acessível e validação sem perder preenchimento '+width,async({page})=>{
 await page.setViewportSize({width,height:900});await page.goto('/');await page.getByRole('button',{name:'Explorar oportunidades'}).click();await page.getByRole('button',{name:'Entrar',exact:true}).first().click();await page.getByRole('button',{name:'Criar conta',exact:true}).click();
 await page.getByLabel('Nome completo',{exact:true}).fill('Pessoa de teste');await page.getByLabel('E-mail',{exact:true}).fill('teste@example.com');await page.getByLabel('Senha',{exact:true}).fill('Senha-teste123');await page.getByRole('button',{name:'Mostrar caracteres',exact:true}).first().click();await expect(page.getByLabel('Senha',{exact:true})).toHaveAttribute('type','text');await page.getByRole('button',{name:'Ocultar caracteres',exact:true}).click();
 await page.getByRole('button',{name:'Criar meu perfil',exact:true}).click();await expect(page.getByLabel('Confirme sua senha',{exact:true})).toBeFocused();await page.keyboard.press('Tab');await expect(page.getByRole('button',{name:'Mostrar caracteres',exact:true}).nth(1)).toBeFocused();await page.keyboard.press('Shift+Tab');await expect(page.getByLabel('Confirme sua senha',{exact:true})).toBeFocused();await expect(page.getByLabel('Nome completo',{exact:true})).toHaveValue('Pessoa de teste');await expect(page.getByRole('alert')).toContainText('Confira os campos indicados');await expect(page.getByRole('progressbar')).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.screenshot({path:'../ops/forms-cadastro-'+width+'.png',fullPage:true});
 const result=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze();expect(result.violations.filter(v=>v.impact==='serious'||v.impact==='critical')).toEqual([]);
 expect(await page.evaluate(()=>JSON.stringify({...localStorage,...sessionStorage}).includes('Senha-teste123'))).toBe(false);
});
