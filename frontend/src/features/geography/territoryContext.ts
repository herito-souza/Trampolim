import { createContext, useContext } from 'react';
import type { TerritoryChoice, TerritorySettings } from './territory';
export const all: TerritoryChoice = { scope: 'all', label: 'Todas as localidades', includeRemote: true };
export const TerritoryContext = createContext<{
    choice: TerritoryChoice;
    settings: TerritorySettings | null;
    loading: boolean;
    error: string;
    choose: (choice: TerritoryChoice) => void;
}>({ choice: all, settings: null, loading: false, error: '', choose: () => { } });
export const useTerritory = () => useContext(TerritoryContext);
