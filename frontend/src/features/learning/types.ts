export type Course = { id: number; title: string; description: string; category: string; teacher_name: string; image_url: string | null; status: string; moderation_blocked?: boolean; lessons_count?: number; subjects?: Subject[] };
export type Subject = { id: number; title: string; description: string; sort_order: number; modules: Module[] };
export type Module = { id: number; title: string; description: string; sort_order: number; lessons: Lesson[] };
export type Lesson = { id: number; module_id: number; title: string; description: string; objectives: string; transcript?: string; captions_vtt?: string; sort_order: number; status: string; duration_seconds: number | null; validation_status: string; progress?: Progress };
export type Progress = { lesson_id?: number; course_id?: number; title?: string; course_title?: string; position_seconds?: number; watched_seconds?: number; completed_at?: string | null };
export const inputClass = 'mt-1 w-full rounded-xl border border-white/20 bg-[#0b1421] px-3 py-2 text-ink';
export const buttonClass = 'min-h-11 rounded-full border border-white/20 px-4 py-2 text-sm font-bold hover:border-brand-blue disabled:opacity-50';
