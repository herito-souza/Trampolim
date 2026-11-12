import type {DiagnosticResult} from './recommendations';
export const diagnosticQuestionCount=10;
export function loadDiagnosticResult(): DiagnosticResult | null {
  try {
    const raw = sessionStorage.getItem('trampolim_diagnostic_result');
    if (!raw) return null;
    const result = JSON.parse(raw) as DiagnosticResult;
    return Array.isArray(result.answers) && result.answers.length === diagnosticQuestionCount && result.scores ? result : null;
  } catch { return null; }
}
