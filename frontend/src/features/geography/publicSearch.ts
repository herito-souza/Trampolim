/** Only public, unauthenticated responses. Bounded memory, short TTL, no account data. */
const cache = new Map<string, { at: number; value: unknown }>();
export async function publicSearch<T>(url: string, signal: AbortSignal): Promise<T> {
 signal.throwIfAborted();
 const hit = cache.get(url);
 if (hit && Date.now() - hit.at < 30000) return structuredClone(hit.value) as T;
 let response: Response;
 try { response = await fetch(url, { signal }); }
 catch {
  signal.throwIfAborted();
  throw new Error('Não foi possível carregar a pesquisa. Verifique sua conexão e tente novamente.');
 }
 if (!response.ok) throw new Error('Não foi possível carregar a pesquisa. Tente novamente.');
 const value: unknown = await response.json();
 signal.throwIfAborted();
 if(!Array.isArray(value)&&(!value||typeof value!=='object'||!('items' in value)||!Array.isArray(value.items)))throw new Error('Resposta inesperada do serviço de pesquisa.');
 cache.delete(url); cache.set(url, { at: Date.now(), value });
 while (cache.size > 40) cache.delete(cache.keys().next().value!);
 return structuredClone(value) as T;
}
