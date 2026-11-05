import test from 'node:test';
import assert from 'node:assert/strict';
import { publicSearch } from '../src/features/geography/publicSearch.ts';
test('cache público é limitado, expira e respeita cancelamento sem credenciais',async()=>{
 const originalFetch=globalThis.fetch, originalNow=Date.now;let calls=0,clock=100000;
 try{
 Date.now=()=>clock;
 globalThis.fetch=async(_url,options)=>{assert.equal(options.headers,undefined);calls++;return {ok:true,json:async()=>({items:[calls]})}};
 const signal=new AbortController().signal;
 const first=await publicSearch('https://example.test/search?a',signal);first.items.length=0;
 assert.deepEqual(await publicSearch('https://example.test/search?a',signal),{items:[1]});assert.equal(calls,1);
 clock+=30001;await publicSearch('https://example.test/search?a',signal);assert.equal(calls,2);
 for(let i=0;i<41;i++)await publicSearch('https://example.test/search?'+i,signal);
 const before=calls;await publicSearch('https://example.test/search?a',signal);assert.equal(calls,before+1);
 const abort=new AbortController();abort.abort();await assert.rejects(()=>publicSearch('https://example.test/search?a',abort.signal),{name:'AbortError'});
 }finally{globalThis.fetch=originalFetch;Date.now=originalNow;}
});

test('falha de rede tem mensagem clara, não é guardada no cache e permite recuperação',async()=>{
 const originalFetch=globalThis.fetch;const signal=new AbortController().signal;let calls=0;
 try{
  globalThis.fetch=async()=>{calls++;if(calls===1)throw new TypeError('Failed to fetch');return {ok:true,json:async()=>({items:['fixture']})}};
  const url='https://example.test/network-recovery-qa';
  await assert.rejects(()=>publicSearch(url,signal),/Verifique sua conexão e tente novamente/);
  assert.deepEqual(await publicSearch(url,signal),{items:['fixture']});assert.equal(calls,2);
  const abort=new AbortController();
  globalThis.fetch=async()=>{abort.abort();throw new TypeError('Failed to fetch')};
  await assert.rejects(()=>publicSearch('https://example.test/abort-qa',abort.signal),{name:'AbortError'});
 }finally{globalThis.fetch=originalFetch;}
});
