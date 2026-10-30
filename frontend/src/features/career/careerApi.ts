import { API_URL, ApiError } from '../account/api';
export async function careerPublic<T>(path: string): Promise<T> { const response = await fetch(API_URL + path); if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(body?.detail || 'Não foi possível consultar os dados públicos agora. Tente novamente.', response.status);
} return response.json() as Promise<T>; }
