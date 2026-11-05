import { useEffect, useState } from 'react';
/** A pending query is never used to fetch or label results for a different selection. */
export function useRegionalQuery(query: string) { const [settled, setSettled] = useState<string | null>(null); useEffect(() => { const timer = setTimeout(() => setSettled(query), 300); return () => clearTimeout(timer); }, [query]); return settled === query ? settled : null; }
