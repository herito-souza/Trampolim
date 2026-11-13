export function normalizeSearchText(value: string) {
  return value.normalize('NFD').replace(/\p{M}+/gu, '').toLocaleLowerCase('pt-BR').trim();
}

export function matchesSearch(query: string, ...values: (string | null | undefined)[]) {
  const needle = normalizeSearchText(query);
  return Boolean(needle) && normalizeSearchText(values.filter(Boolean).join(' ')).includes(needle);
}
