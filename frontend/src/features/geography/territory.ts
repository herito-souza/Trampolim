export type Municipality = {
    code: string;
    name: string;
    state: string;
};
export type TerritorySettings = {
    states?: string[];
    defaultRegion: string;
    referenceMunicipality: Municipality;
    regions: {
        id: string;
        name: string;
        description: string;
        municipalityCodes: string[];
    }[];
    source: string;
};
export type TerritoryChoice = {
    scope: string;
    label: string;
    includeRemote: boolean;
};
export const fold = (value: string | null | undefined) => (value || '').normalize('NFD').replace(/\p{M}+/gu, '').toLocaleLowerCase('pt-BR').trim();
export function territorialParams(choice: TerritoryChoice) { const params = new URLSearchParams(); if (choice.scope.startsWith('region:'))
    params.set('region', choice.scope.slice(7)); if (choice.scope.startsWith('state:')) params.set('state', choice.scope.slice(6)); if (choice.scope.startsWith('city:'))
    params.set('municipalityCode', choice.scope.slice(5)); params.set('includeRemote', String(choice.includeRemote)); return params; }
export type SearchRefinements = { accessibleOnly?: string; radiusKm?: string; latitude?: string; longitude?: string; contractType?: string; experienceLevel?: string; educationLevel?: string; maxExperienceMonths?: string; minimumSalary?: string; maximumSalary?: string; publishedDays?: string; workSchedule?: string; };
export type TerritorialFilters = SearchRefinements & {
    provider?: string;
    kind?: string;
    query?: string;
    city?: string;
    area?: string;
    modality?: string;
};
export function territorialSearch(choice: TerritoryChoice, filters: TerritorialFilters = {}) { const params = territorialParams(choice); for (const [field, value] of Object.entries({ accessibleOnly: filters.accessibleOnly, radiusKm: filters.radiusKm, latitude: filters.latitude, longitude: filters.longitude, provider: filters.provider, kind: filters.kind, q: filters.query, city: filters.city, area: filters.area, modality: filters.modality, contractType: filters.contractType, experienceLevel: filters.experienceLevel, educationLevel: filters.educationLevel, maxExperienceMonths: filters.maxExperienceMonths, minimumSalary: filters.minimumSalary, maximumSalary: filters.maximumSalary, publishedDays: filters.publishedDays, workSchedule: filters.workSchedule }))
    if (value?.trim())
        params.set(field, value.trim()); return params.toString(); }
