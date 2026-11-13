import { useEffect, useRef, useState } from 'react';
import './pwa.css';

type InstallPromptEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }> };

export function PwaRuntime() {
  const [online, setOnline] = useState(navigator.onLine);
  const [reconnected, setReconnected] = useState(false);
  const [installPrompt, setInstallPrompt] = useState<InstallPromptEvent | null>(null);
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const [reloadReady, setReloadReady] = useState(false);
  const [installHelp, setInstallHelp] = useState(false);
  const [iosInstall] = useState(() => /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1));
  const [installed, setInstalled] = useState(() => window.matchMedia('(display-mode: standalone)').matches || Boolean((navigator as Navigator & { standalone?: boolean }).standalone));
  const updateRequested = useRef(false);

  useEffect(() => {
    const wentOnline = () => { setOnline(true); setReconnected(true); window.setTimeout(() => setReconnected(false), 5000); };
    const wentOffline = () => { setOnline(false); setReconnected(false); };
    const beforeInstall = (event: Event) => { event.preventDefault(); setInstallPrompt(event as InstallPromptEvent); };
    const appInstalled = () => { setInstalled(true); setInstallPrompt(null); setInstallHelp(false); };
    const installRequested = () => { void install(); };
    const availabilityRequested = () => window.dispatchEvent(new CustomEvent('trampolim:install-availability', { detail: !installed && Boolean(installPrompt || iosInstall || installHelp) }));
    window.addEventListener('online', wentOnline);
    window.addEventListener('offline', wentOffline);
    window.addEventListener('beforeinstallprompt', beforeInstall);
    window.addEventListener('appinstalled', appInstalled);
    window.addEventListener('trampolim:install-click', installRequested);
    window.addEventListener('trampolim:request-install-availability', availabilityRequested);

    let registration: ServiceWorkerRegistration | undefined;
    let listeningForController = false;
    const showWaiting = () => setUpdateAvailable(true);
    const controllerChanged = () => { if (updateRequested.current) { setReloadReady(true); setUpdateAvailable(false); } };
    const watchInstalling = () => {
      const worker = registration?.installing;
      if (!worker) return;
      worker.addEventListener('statechange', () => {
        if (worker.state === 'installed' && navigator.serviceWorker.controller) showWaiting();
      });
    };

    if (import.meta.env.PROD && 'serviceWorker' in navigator) {
      void navigator.serviceWorker.register('/sw.js', { scope: '/' }).then(value => {
        registration = value;
        if (value.waiting && navigator.serviceWorker.controller) showWaiting();
        value.addEventListener('updatefound', watchInstalling);
        navigator.serviceWorker.addEventListener('controllerchange', controllerChanged);
        listeningForController = true;
        window.addEventListener('focus', () => { void value.update().catch(() => undefined); }, { once: true });
      }).catch(() => undefined);
    }

    return () => {
      window.removeEventListener('online', wentOnline);
      window.removeEventListener('offline', wentOffline);
      window.removeEventListener('beforeinstallprompt', beforeInstall);
      window.removeEventListener('appinstalled', appInstalled);
      window.removeEventListener('trampolim:install-click', installRequested);
      window.removeEventListener('trampolim:request-install-availability', availabilityRequested);
      if (listeningForController) navigator.serviceWorker.removeEventListener('controllerchange', controllerChanged);
    };
  }, []);

  useEffect(() => {
    window.dispatchEvent(new CustomEvent('trampolim:install-availability', { detail: !installed && Boolean(installPrompt || iosInstall || installHelp) }));
  }, [installed, installPrompt, iosInstall, installHelp]);

  async function install() {
    if (!installPrompt) { setInstallHelp(true); return; }
    await installPrompt.prompt();
    const choice = await installPrompt.userChoice;
    if (choice.outcome !== 'accepted') setInstallHelp(false);
    setInstallPrompt(null);
  }

  function requestUpdate() {
    updateRequested.current = true;
    // The waiting worker is obtained from the registration owning this page.
    void navigator.serviceWorker?.getRegistration('/').then(registration => {
      if (registration?.waiting) registration.waiting.postMessage({ type: 'SKIP_WAITING' });
      else setUpdateAvailable(false);
    });
  }

  return <>
    {!online && <div className="pwa-status" role="status" aria-live="polite"><strong>Sem conexão.</strong> Alguns conteúdos públicos salvos podem estar desatualizados. Ações e candidaturas só serão confirmadas quando o servidor responder.</div>}
    {online && reconnected && <div className="pwa-status pwa-status-online" role="status" aria-live="polite">Conexão restabelecida. Atualize os dados antes de continuar.</div>}
    {updateAvailable && <div className="pwa-update" role="status"><span>Há uma atualização do Trampolim pronta.</span><button type="button" onClick={requestUpdate}>Preparar atualização</button><button type="button" className="pwa-dismiss" aria-label="Depois" onClick={() => setUpdateAvailable(false)}>Depois</button></div>}
    {reloadReady && <div className="pwa-update" role="status"><span>Atualização pronta para uso.</span><button type="button" onClick={() => window.location.reload()}>Recarregar agora</button></div>}
    {installHelp && <div className="pwa-install-help" id="pwa-install-help" role="status">Para instalar no iPhone ou iPad, abra o menu Compartilhar do navegador e escolha “Adicionar à Tela de Início”. <button type="button" onClick={() => setInstallHelp(false)}>Fechar</button></div>}
  </>;
}
