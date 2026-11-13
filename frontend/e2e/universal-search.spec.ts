import {expect,test} from '@playwright/test';

test('atalho, pesquisa de páginas, navegação por teclado e fechar com Escape',async({page})=>{
 await page.setViewportSize({width:375,height:850});
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','universal-search-fixture');localStorage.setItem('trampolim_profile_step','applications')});
 await page.route('http://localhost:8080/api/**',route=>{const path=new URL(route.request().url()).pathname;if(path==='/api/profile')return route.fulfill({json:{profile:{name:'Pessoa de teste',email:'search@example.test'},experiences:[],educations:[]}});if(path==='/api/profile/diagnostic')return route.fulfill({json:null});if(path==='/api/profile/applications')return route.fulfill({json:[]});if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Sem acesso'}});return route.fulfill({json:[]})});
 await page.goto('/#oportunidades/applications');
 await page.keyboard.press('Control+k');
 const dialog=page.getByRole('dialog',{name:'Pesquisa no Trampolim Oportunidades'}),input=dialog.getByRole('combobox');
 await expect(input).toBeFocused();await input.fill('candidaturas');
 const result=dialog.getByRole('option',{name:'Candidaturas'});await expect(result).toBeVisible();
 await input.press('Enter');await expect(dialog).toBeHidden();
 await page.getByRole('button',{name:'Pesquisar no Trampolim Oportunidades (Ctrl+K ou Cmd+K)'}).click();
 await input.fill('sem resultado conhecido');await expect(dialog.getByText('Nenhum resultado encontrado. Tente outro termo.')).toBeVisible();
 await page.keyboard.press('Escape');await expect(dialog).toBeHidden();
 await expect(page.getByRole('button',{name:'Pesquisar no Trampolim Oportunidades (Ctrl+K ou Cmd+K)'})).toBeFocused();
});

test('resultados de recursos usam o catálogo público e apresentam estado de busca',async({page})=>{
 await page.addInitScript(()=>{localStorage.setItem('trampolim_token','universal-search-resource-fixture');localStorage.setItem('trampolim_profile_step','applications')});
 await page.route('http://localhost:8080/api/**',route=>{const path=new URL(route.request().url()).pathname;if(path==='/api/profile')return route.fulfill({json:{profile:{name:'Pessoa',email:'search@example.test'},experiences:[],educations:[]}});if(path==='/api/profile/diagnostic')return route.fulfill({json:null});if(path==='/api/profile/applications')return route.fulfill({json:[]});if(path==='/api/admin/saas/overview')return route.fulfill({status:403,json:{detail:'Sem acesso'}});if(path==='/api/public/companies/regional-opportunities')return route.fulfill({json:{items:[{id:1,title:'Auxiliar administrativo',companyName:'Empresa pública de teste',area:'Administração',description:'Vaga de demonstração',municipalityLabel:'Irecê/BA'}],nextCursor:null}});if(path==='/api/opportunities/course/regional')return route.fulfill({json:{items:[],nextCursor:null}});return route.fulfill({json:[]})});
 await page.goto('/#oportunidades/applications');await page.getByRole('button',{name:'Pesquisar no Trampolim Oportunidades (Ctrl+K ou Cmd+K)'}).click();
 const dialog=page.getByRole('dialog',{name:'Pesquisa no Trampolim Oportunidades'});await dialog.getByRole('combobox').fill('auxiliar');
 await expect(dialog.getByRole('option',{name:/Auxiliar administrativo/})).toBeVisible();
 await expect(dialog.getByText('Buscando conteúdos…')).toHaveCount(0);
});
