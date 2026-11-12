import type { Course, Progress } from '../learning/types';
export function nextLearningLesson(course:Course,history:Progress[]) {
 if(course.status!=='PUBLISHED'||course.moderation_blocked)return undefined;
 const lessons=[...(course.subjects||[])].sort((a,b)=>a.sort_order-b.sort_order).flatMap(s=>[...(s.modules||[])].sort((a,b)=>a.sort_order-b.sort_order).flatMap(m=>[...(m.lessons||[])].sort((a,b)=>a.sort_order-b.sort_order))).filter(l=>l.status==='PUBLISHED'&&l.validation_status==='READY');
 const rows=history.filter(p=>p.course_id===course.id);
 const unfinished=rows.find(p=>!p.completed_at&&lessons.some(l=>l.id===p.lesson_id&&!l.progress?.completed_at));
 if(unfinished)return lessons.find(l=>l.id===unfinished.lesson_id);
 const completed=new Set([...rows.filter(p=>p.completed_at).map(p=>p.lesson_id),...lessons.filter(l=>l.progress?.completed_at).map(l=>l.id)]);
 return lessons.find(l=>!completed.has(l.id));
}
