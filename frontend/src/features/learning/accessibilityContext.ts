import { createContext, useContext, useEffect } from 'react';
export type AccessibilityPreferences = { fontPercent: number; highContrast: boolean; reducedMotion: boolean; textSpacing: boolean; playbackSpeed: number; captions: boolean; muted: boolean };
export const defaultPreferences: AccessibilityPreferences = { fontPercent: 100, highContrast: false, reducedMotion: false, textSpacing: false, playbackSpeed: 1, captions: true, muted: false };
export const AccessibilityContext = createContext({ preferences: defaultPreferences, activate: (_token?: string): (() => void) => () => {}, update: (_change: Partial<AccessibilityPreferences>) => {} });
export function useAccessibility() { return useContext(AccessibilityContext); }
export function useCapacitaSurface(token?: string, enabled = true) { const { activate } = useContext(AccessibilityContext); useEffect(() => enabled ? activate(token) : undefined, [activate, token, enabled]); }
