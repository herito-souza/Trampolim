import '../playwright-env.mjs';
import { chromium } from '@playwright/test';
import { createServer } from 'node:http';
import { readFile, writeFile, mkdir, readdir } from 'node:fs/promises';
import { resolve, extname } from 'node:path';
import { gzipSync } from 'node:zlib';
import { fileURLToPath } from 'node:url';
const root=resolve(fileURLToPath(new URL('../',import.meta.url))),dist=resolve(root,'dist');
const label=process.argv[2]||'sample';const output=resolve(root,'../ops/performance-'+label+'.json');
const probe=await readFile(resolve(root,'node_modules/web-vitals/dist/web-vitals.iife.js'),'utf8');
const mime={'.html':'text/html','.js':'text/javascript','.mjs':'text/javascript','.css':'text/css','.json':'application/json','.woff2':'font/woff2','.svg':'image/svg+xml','.png':'image/png'};
const cache=new Map();
const server=createServer(async(req,res)=>{try{const path=decodeURIComponent(new URL(req.url,'http://localhost').pathname);const target=resolve(dist,'.'+(path==='/'?'/index.html':path));if(!target.startsWith(dist+'/')&&!target.startsWith(dist+'\\')){res.writeHead(403).end();return;}let body=cache.get(target);if(!body){body=await readFile(target);cache.set(target,body);}const type=mime[extname(target)]||'application/octet-stream';res.setHeader('Content-Type',type);res.setHeader('Cache-Control',target.endsWith('index.html')?'no-cache':'public,max-age=31536000,immutable');if(/text|json|svg/.test(type)&&req.headers['accept-encoding']?.includes('gzip')){res.setHeader('Content-Encoding','gzip');body=gzipSync(body);}res.end(body);}catch{res.writeHead(404).end();}});
await new Promise(resolve=>server.listen(4180,'127.0.0.1',resolve));
const browser=await chromium.launch({headless:true});const results=[];
const profile={profile:{name:'Pessoa sintética de medição',email:'fixture@example.test',location:'Irecê/BA',phone:null,summary:null},experiences:[],educations:[]};
const config={defaultRegion:'irece-ba',referenceMunicipality:{code:'2914604',name:'Irecê',state:'BA'},regions:[{id:'irece-ba',name:'Irecê e região',municipalityCodes:['2914604']}]};
const scenarios=[... [320,375,768,1024,1440].map(width=>({area:'homepage',width})),...['candidate','company','capacita'].flatMap(area=>[375,1440].map(width=>({area,width})))];
try{
 for(const scenario of scenarios.filter(scenario=>!process.env.PERF_WIDTH||scenario.width===Number(process.env.PERF_WIDTH)))for(let run=1;run<=3;run++){
 const context=await browser.newContext({viewport:{width:scenario.width,height:900},deviceScaleFactor:1});const page=await context.newPage();const cdp=await context.newCDPSession(page);
 await cdp.send('Network.enable');await cdp.send('Network.setCacheDisabled',{cacheDisabled:true});await cdp.send('Network.emulateNetworkConditions',{offline:false,latency:150,downloadThroughput:1600000/8,uploadThroughput:750000/8,connectionType:'cellular4g'});await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
 await page.addInitScript({content:probe+`;window.__measuredVitals={};for(const name of ['LCP','INP','CLS'])webVitals['on'+name](metric=>{window.__measuredVitals[name]=metric.value},{reportAllChanges:true,durationThreshold:16});`});
 await page.addInitScript(({area})=>{if(area==='candidate'){localStorage.setItem('trampolim_token','fixture-candidate');localStorage.setItem('trampolim_profile_step','opportunities')}if(area==='company')localStorage.setItem('trampolim_empresas_token','fixture-company');if(area==='capacita')localStorage.setItem('trampolim_capacita_token','fixture-student')},scenario);
 let requests=0,bytes=0,heavy=[];page.on('request',r=>{requests++;if(/maplibre|RegionalMap|pdf\.worker|\.mp4/.test(r.url()))heavy.push(r.url().split('/').pop())});cdp.on('Network.loadingFinished',e=>bytes+=e.encodedDataLength);
 await page.route('http://localhost:8080/api/**',async route=>{await new Promise(r=>setTimeout(r,120));const url=new URL(route.request().url()),p=url.pathname;let json=[];let status=200;
 if(p.endsWith('/geography/settings'))json=config;else if(p==='/api/profile')json=profile;else if(p==='/api/profile/diagnostic')json=null;else if(p==='/api/admin/saas/overview'){status=403;json={detail:'Fixture'}}
 else if(p==='/api/company/dashboard')json={tradeName:'Empresa sintética de medição',legalName:'Fixture',cnpj:'11222333000181',role:'OWNER',city:'Irecê',state:'BA',industry:'Fixture',phone:'000'};
 else if(p.endsWith('/learning/me'))json={id:1,name:'Fixture',teacher:false,admin:false};else if(p.endsWith('/learning/courses'))json=[{id:1,title:'Curso sintético de medição',description:'Fixture de medição controlada',category:'Tecnologia',teacher_name:'Professor sintético',status:'PUBLISHED'}];else if(p.endsWith('/notifications'))json={items:[],unreadCount:0};else if(p.endsWith('/stream')){return route.fulfill({contentType:'text/event-stream',body:'event: ready\ndata: {}\n\n'})}
 else if(p.endsWith('/inclusion/preferences'))json={saved:false};
 return route.fulfill({status,json});});
 const hash=scenario.area==='homepage'?'inicio':scenario.area==='company'?'empresas':scenario.area==='capacita'?'capacita':'oportunidades';
 const start=Date.now();await page.goto('http://127.0.0.1:4180/#'+hash,{waitUntil:'domcontentloaded'});await page.locator('main').waitFor();await page.waitForTimeout(6500);
 if(scenario.area==='homepage'){for(let i=0;i<4;i++)await page.getByRole('button',{name:i%2?'Mostrar menos':'Saiba mais',exact:true}).first().click();await page.getByLabel('Cargo ou área profissional').fill('atendimento');}
 if(scenario.area==='candidate'){await page.getByRole('button',{name:'Consultar cursos disponíveis',exact:true}).click();await page.getByLabel('Buscar por nome ou palavra-chave').fill('informática');}
 if(scenario.area==='company'){await page.getByRole('button',{name:'Gerenciar anúncios',exact:true}).click();await page.getByLabel('Cargo, área ou requisito do anúncio').fill('auxiliar');}
 if(scenario.area==='capacita'){await page.getByRole('searchbox',{name:'Buscar cursos',exact:true}).fill('Tecnologia');const learning=page.getByRole('button',{name:'Meu aprendizado',exact:true,includeHidden:true});if(!await learning.isVisible())await page.getByRole('button',{name:'Alternar menu lateral',exact:true}).click();await learning.click();}
 await page.waitForTimeout(1200);const other=await context.newPage();await other.bringToFront();await page.waitForTimeout(150);
 const metrics=await page.evaluate(()=>({...window.__measuredVitals}));const sample={...scenario,run,LCP:metrics.LCP??null,INP:metrics.INP??null,CLS:metrics.CLS??null,requests,encodedBytes:bytes,heavyRequests:heavy.length,elapsedMs:Date.now()-start};results.push(sample);console.log(JSON.stringify(sample));await context.close();
 }
 const assets=[];for(const file of await readdir(resolve(dist,'assets'))){if(/\.(js|css)$/.test(file)){const body=await readFile(resolve(dist,'assets',file));assets.push({file,bytes:body.length,gzipBytes:gzipSync(body).length})}}
 await mkdir(resolve(root,'../ops'),{recursive:true});await writeFile(output,JSON.stringify({label,date:new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Sao_Paulo'}).format(new Date()),method:{tool:'web-vitals in Chromium/Playwright',build:'production dist served locally with gzip',runsPerScenario:3,latencyMs:150,downloadMbps:1.6,uploadMbps:.75,cpuSlowdown:4,cache:'disabled per context',api:'synthetic responses delayed 120ms; not backend measurements',note:'Laboratory interaction sample, not CrUX/production p75. Initial 6.5s observation followed by scripted interactions.'},results,assets},null,2));console.log('Saved '+output);
}finally{await browser.close();await new Promise(resolve=>server.close(resolve))}
