export type QuestionLimits = { maxBytes: number; audioMaxSeconds: number; videoMaxSeconds: number };
export const defaultLimits: QuestionLimits = { maxBytes: 25 * 1024 * 1024, audioMaxSeconds: 120, videoMaxSeconds: 120 };
