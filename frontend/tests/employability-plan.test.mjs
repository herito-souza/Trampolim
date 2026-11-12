import test from 'node:test';
import assert from 'node:assert/strict';
import {journey,initialObjectives,stepDestination} from '../src/features/planning/planModel.ts';
const step=(key,kind='SKILL_DEVELOPMENT')=>({key,kind,title:key,criteria:{},resource:{},criterionHash:key});
test('progresso usa somente etapas atuais e conclusão declarada, sem premiar etapas dispensadas',()=>{
 const plan={exists:true,revision:2,snapshot:{steps:[step('a'),step('b'),step('c')]},progress:[{step_key:'a',state:'DECLARED_DONE'},{step_key:'b',state:'DISMISSED'},{step_key:'old',state:'DECLARED_DONE'}]};const result=journey(plan);assert.equal(result.done,1);assert.equal(result.total,3);assert.equal(result.percent,33);assert.equal(result.next.key,'c');
});
test('prioriza etapa em andamento e não recomenda ação de snapshot desatualizado',()=>{
 const plan={exists:true,revision:2,snapshot:{steps:[step('a'),step('b')]},progress:[{step_key:'b',state:'IN_PROGRESS'}]};assert.equal(journey(plan).next.key,'b');assert.equal(journey({...plan,localInputsChanged:true}).next,undefined);assert.equal(journey({exists:false,revision:0}).percent,0);
});
test('objetivos são cópia revisável, sem converter preferência em competência',()=>{
 const seed={occupation:'Analista',area:'Tecnologia',city:'Irecê',modality:'Remoto',skills:[{skillCode:'sql',level:2,requireEvidence:false}],jobs:[]};const copy=initialObjectives({objectives:seed});copy.skills[0].level=4;assert.equal(seed.skills[0].level,2);assert.deepEqual(initialObjectives({suggestedObjectives:{city:'Lapão'}}),{occupation:'',area:'',city:'Lapão',modality:'',skills:[],jobs:[]});
});
test('etapas reutilizam destinos existentes sem confundir as sessões',()=>{assert.equal(stepDestination(step('a','TRAINING')),'capacita');assert.equal(stepDestination(step('b','DIAGNOSTIC')),'diagnostic');assert.equal(stepDestination(step('c','EVIDENCE_REVIEW')),'radar');assert.equal(stepDestination(step('d','PROFILE_REVIEW')),'profile');assert.equal(stepDestination(step('e','SEARCH_OPPORTUNITIES')),'jobs');});
