export const libraryRoot = '/capacita/learning/library';
export type LibraryScope = { course?: number; module?: number; lesson?: number };
export type Material = {
  id: number; course_id: number; subject_id: number | null; module_id: number | null; teacher_id: number;
  title: string; description: string; topic: string; category: string; alternative_text: string;
  access_scope: 'STUDENTS' | 'TEACHERS'; allow_download: boolean; status: 'DRAFT' | 'PUBLISHED' | 'HIDDEN';
  revision: number; current_version_id: number | null; teacher_name: string; course_title: string; subject_title?: string; module_title?: string;
  published_at: string | null; version_number: number | null; page_count: number | null; has_text: boolean; tagged: boolean;
  lesson_ids?: number[]; versions?: { id: number; version_number: number; created_at: string; page_count: number }[];
};
