export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export interface UserSummary {
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  user: UserSummary;
  emailVerified: boolean;
  accountType?: 'PERSON' | 'COMPANY';
}

export interface Profile {
  name: string;
  email: string;
  phone: string | null;
  location: string | null;
  summary: string | null;
  resumeFileName: string | null;
  photoFileName: string | null;
}

export interface CatalogItem {
  id: number; kind: 'course' | 'job'; title: string; provider: string; description: string; url: string; area: string;
  location: string | null; latitude: number | null; longitude: number | null; modality: string | null; experienceLevel: string | null; contractType: string | null; workSchedule: string | null; salary: string | null; accessibility: string | null; active: boolean;
  moderationStatus: 'PENDING' | 'APPROVED' | 'REJECTED'; expiresAt: string | null; createdAt: string; updatedAt: string; createdBy: string | null; updatedBy: string | null; reviewedBy: string | null;
  publishedAt: string | null; lastLinkCheckAt?: string | null; lastLinkReminderAt?: string | null; linkStatus?: 'UNKNOWN' | 'ACTIVE' | 'BROKEN' | null;
}
export interface AlertPreferences { enabled: boolean; areas: string[]; frequency: 'immediate' | 'daily' | 'weekly'; city: string; modality: string; contractType: string; workSchedule: string; keywords: string; useDistance: boolean; latitude: number | null; longitude: number | null; radiusKm: number | null }
export interface Application { id: number; itemId: number; title: string; provider: string; location: string | null; url: string; status: string; createdAt: string; updatedAt: string; notes: string | null; followUpDate: string | null }
export interface PublicProfile { name: string; location: string | null; summary: string | null; experiences: Experience[]; educations: Education[]; summaryShared: boolean; experiencesShared: boolean; educationShared: boolean }

export async function publicApi<T>(path: string): Promise<T> {
  const response = await fetch(`${API_URL}${path}`);
  if (!response.ok) throw new Error('Este perfil não está disponível.');
  return response.json() as Promise<T>;
}

export function recordCatalogMetric(eventType: 'open' | 'apply', kind: 'course' | 'job', itemKey: string | number, token: string): void {
  void fetch(`${API_URL}/metrics/catalog`, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ eventType, kind, itemKey: String(itemKey) }), keepalive: true }).catch(() => undefined);
}

export interface Experience {
  id: number;
  position: string;
  company: string;
  startDate: string | null;
  endDate: string | null;
  current: boolean;
  description: string | null;
}

export interface Education {
  id: number;
  institution: string;
  course: string;
  level: string | null;
  startDate: string | null;
  endDate: string | null;
  inProgress: boolean;
}

export interface ProfileResponse {
  profile: Profile;
  experiences: Experience[];
  educations: Education[];
}

export async function api<T>(path: string, token: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.detail || errorBody?.message || 'Não foi possível concluir a solicitação.');
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export async function authenticate(mode: 'login' | 'cadastro' | 'company-cadastro' | 'company-join', payload: Record<string, string>): Promise<AuthResponse> {
  const path = mode === 'company-cadastro' ? '/auth/company/cadastro' : mode === 'company-join' ? '/auth/company/join' : `/auth/${mode}`;
  const response = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw new Error(await readApiError(response, mode === 'login' ? 'login' : 'cadastro'));
  const body = await response.json().catch(() => null);
  if (!body || typeof body !== 'object') throw new Error('O servidor respondeu em um formato inesperado. Tente novamente.');
  return body as AuthResponse;
}

export async function acceptCompanyInvite(inviteToken: string, token: string): Promise<void> {
  await api<{ accountType: string }>('/company/invites/accept', token, { method: 'POST', body: JSON.stringify({ token: inviteToken }) });
}

async function readApiError(response: Response, flow: 'login' | 'cadastro'): Promise<string> {
  const text = await response.text().catch(() => '');
  let body: Record<string, unknown> | null = null;
  try {
    const parsed: unknown = JSON.parse(text);
    if (parsed && typeof parsed === 'object') body = parsed as Record<string, unknown>;
    else if (typeof parsed === 'string') return parsed;
  } catch {
    const plainText = text.trim();
    if (plainText && !/^\s*</.test(plainText) && plainText.length < 400) return plainText;
  }

  const message = [body?.detail, body?.message, body?.error_description, body?.description]
    .find((value): value is string => typeof value === 'string' && value.trim().length > 0);
  if (message) return message;
  if (body?.errors && typeof body.errors === 'object') {
    const firstError = Object.values(body.errors as Record<string, unknown>).flatMap(value => Array.isArray(value) ? value : [value]).find(value => typeof value === 'string' && value.trim());
    if (typeof firstError === 'string') return firstError;
  }
  if (typeof body?.title === 'string' && !['Unauthorized', 'Forbidden', 'Bad Request', 'Internal Server Error'].includes(body.title)) return body.title;

  if (response.status === 401) return 'E-mail ou senha incorretos. Confira os dois campos e tente novamente.';
  if (response.status === 403) return flow === 'login' ? 'Acesso não autorizado. Confirme seu e-mail ou verifique se esta conta pode entrar.' : 'O backend está bloqueando a rota de cadastro. Reinicie o backend atualizado e tente novamente.';
  if (response.status === 409) return 'Já existe uma conta com este e-mail ou CNPJ.';
  if (response.status === 429) return 'Muitas tentativas em pouco tempo. Aguarde alguns minutos e tente novamente.';
  if (response.status >= 500) return 'O servidor encontrou um problema ao processar a solicitação. Tente novamente em instantes.';
  if (response.status === 400) return 'Há um dado obrigatório ausente ou inválido. Confira os campos do formulário.';
  return `Não foi possível concluir: o servidor respondeu com o código ${response.status}.`;
}

export async function requestPasswordReset(email: string): Promise<string> {
  const response = await fetch(`${API_URL}/auth/password/reset-request`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.detail || 'Não foi possível solicitar a redefinição.');
  return body.message as string;
}

export async function resetPassword(token: string, password: string): Promise<string> {
  const response = await fetch(`${API_URL}/auth/password/reset`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token, password }) });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.detail || 'Não foi possível alterar a senha.');
  return body.message as string;
}

export async function confirmEmail(token: string): Promise<string> {
  const response = await fetch(`${API_URL}/auth/email/verify?token=${encodeURIComponent(token)}`);
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.detail || 'Não foi possível confirmar este e-mail.');
  return body.message as string;
}

export async function resendEmailConfirmation(email: string): Promise<string> {
  const response = await fetch(`${API_URL}/auth/email/resend`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.detail || 'Não foi possível reenviar o link.');
  return body.message as string;
}
