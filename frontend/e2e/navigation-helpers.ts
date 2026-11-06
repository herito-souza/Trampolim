import {expect} from '@playwright/test';
import type {Page} from '@playwright/test';
export async function prepareNavigation(page:Page,name:string,label?:string){
 const scope=label?page.getByRole('navigation',{name:label,exact:true,includeHidden:true}):page;
 const hidden=scope.getByRole('button',{name,exact:true,includeHidden:true});
 await expect(hidden).toBeAttached();
 if(await hidden.isVisible())return;
 const owner=await hidden.evaluate(element=>element.closest('dialog.workspace-drawer')?'sidebar':element.closest('.workspace-popover')?'profile':'other');
 if(owner==='sidebar')await page.getByRole('button',{name:'Alternar menu lateral',exact:true}).click();
 else if(owner==='profile')await page.locator('summary[aria-label^="Menu da conta"]').click();
 await expect(hidden).toBeVisible();
}
export async function activate(page:Page,name:string,label?:string){await prepareNavigation(page,name,label);const scope=label?page.getByRole('navigation',{name:label,exact:true}):page;await scope.getByRole('button',{name,exact:true}).click();}

export async function openPreferences(page:Page){const toggle=page.getByText('Acessibilidade e preferências',{exact:true});await expect(toggle).toBeAttached();if(!await toggle.isVisible())await page.locator('summary[aria-label^="Menu da conta"]').click();await toggle.click();}
