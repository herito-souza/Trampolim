import { TerritoryProvider } from './features/geography/TerritoryProvider';
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { CapacitaAccessibilityProvider } from './features/learning/AccessibilityPreferences'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <CapacitaAccessibilityProvider><TerritoryProvider><App /></TerritoryProvider></CapacitaAccessibilityProvider>
  </StrictMode>,
)
