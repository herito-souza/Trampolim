import test from 'node:test';
import assert from 'node:assert/strict';
import {isActiveListing} from '../src/features/dashboard/dashboardModels.ts';
import {nextLearningLesson} from '../src/features/dashboard/learningModels.ts';
const now=Date.parse('2026-11-06T12:00:00Z');
const listing={publicationStatus:'PUBLISHED',isInternship:false,paymentStatus:'NOT_REQUIRED',expiresAt:null,applicationDeadline:null};
test('somente anúncios publicados, disponíveis e estágios pagos ficam ativos',()=>{
 assert.equal(isActiveListing(listing,now),true);
 for(const publicationStatus of ['DRAFT','AWAITING_PAYMENT','PAUSED','CLOSED'])assert.equal(isActiveListing({...listing,publicationStatus},now),false);
 assert.equal(isActiveListing({...listing,isInternship:true,paymentStatus:'PENDING'},now),false);
 assert.equal(isActiveListing({...listing,isInternship:true,paymentStatus:'PAID'},now),true);
 assert.equal(isActiveListing({...listing,expiresAt:'2026-11-05'},now),false);
 assert.equal(isActiveListing({...listing,applicationDeadline:'2026-11-05'},now),false);
 assert.equal(isActiveListing({...listing,expiresAt:'2026-11-06'},now),true);
 assert.equal(isActiveListing({...listing,expiresAt:'invalid'},now),false);
});
const lesson=(id,sort_order,extra={})=>({id,sort_order,status:'PUBLISHED',validation_status:'READY',...extra});
const course={id:1,status:'PUBLISHED',subjects:[{sort_order:0,modules:[{sort_order:0,lessons:[lesson(2,2),lesson(1,1),lesson(3,3,{status:'DRAFT'}),lesson(4,4,{validation_status:'PENDING'})]}]}]};
test('próxima aula respeita ordem real, publicação, validação e progresso',()=>{
 assert.equal(nextLearningLesson(course,[]).id,1);
 assert.equal(nextLearningLesson(course,[{course_id:1,lesson_id:1,completed_at:'date'}]).id,2);
 assert.equal(nextLearningLesson(course,[{course_id:1,lesson_id:2,completed_at:null}]).id,2);
 assert.equal(nextLearningLesson(course,[{course_id:2,lesson_id:1,completed_at:'date'}]).id,1);
 assert.equal(nextLearningLesson(course,[{course_id:1,lesson_id:1,completed_at:'date'},{course_id:1,lesson_id:2,completed_at:'date'}]),undefined);
 assert.equal(nextLearningLesson({...course,moderation_blocked:true},[]),undefined);
 assert.equal(nextLearningLesson({...course,status:'HIDDEN'},[]),undefined);
});
