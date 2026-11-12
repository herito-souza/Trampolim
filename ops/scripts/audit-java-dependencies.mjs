import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {dirname} from 'node:path';
const [treePath,outputPath]=process.argv.slice(2);
if(!treePath||!outputPath)throw new Error('Usage: node audit-java-dependencies.mjs dependency-tree.json report.json');
const root=JSON.parse(await readFile(treePath,'utf8')),items=new Map();
function visit(node){if(node!==root){const name=node.groupId+':'+node.artifactId,key=name+'@'+node.version;const previous=items.get(key);if(!previous||previous.scope==='test')items.set(key,{name,version:node.version,scope:node.scope})}for(const child of node.children||[])visit(child)}visit(root);
const dependencies=[...items.values()];if(!dependencies.length||dependencies.length>1000)throw new Error('Unexpected dependency count; no scan performed');
const response=await fetch('https://api.osv.dev/v1/querybatch',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({queries:dependencies.map(d=>({package:{ecosystem:'Maven',name:d.name},version:d.version}))}),signal:AbortSignal.timeout(30000)});
if(!response.ok)throw new Error('OSV query HTTP '+response.status);const body=await response.json();
const complete=body.results?.length===dependencies.length&&!body.results.some(r=>r.next_page_token||r.error);
const findings=(body.results||[]).flatMap((result,index)=>(result.vulns||[]).map(v=>({...dependencies[index],id:v.id})));
const advisories=[];for(const id of [...new Set(findings.map(f=>f.id))]){const response=await fetch('https://api.osv.dev/v1/vulns/'+encodeURIComponent(id),{signal:AbortSignal.timeout(20000)});if(!response.ok)throw new Error('OSV advisory HTTP '+response.status);advisories.push(await response.json())}
await mkdir(dirname(outputPath),{recursive:true});await writeFile(outputPath,JSON.stringify({timestamp:new Date().toISOString(),source:'https://api.osv.dev/v1/querybatch',complete,dependencies,findings,advisories},null,2));
console.log(JSON.stringify({dependencies:dependencies.length,advisories:advisories.length,findings:findings.length,complete}));
if(!complete||findings.length)process.exitCode=1;
