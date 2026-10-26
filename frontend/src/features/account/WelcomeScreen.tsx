import { useEffect, useRef, useState } from 'react';
import { API_URL, api } from './api';
import type { Profile } from './api';
import { AccountNavigation } from './AccountNavigation';
import type { AccountDestination } from './AccountNavigation';
import { AccountHeaderActions } from './AccountHeaderActions';
import { RibbonStrip } from '../../components/ui/RibbonStrip';

interface WelcomeScreenProps {
  token: string;
  profile: Profile;
  onProfileChange: (profile: Profile) => void;
  onContinue: () => void;
  onLogout: () => void;
  onNavigate: (destination: AccountDestination) => void;
  canManage?: boolean;
}

export function WelcomeScreen({ token, profile, onProfileChange, onContinue, onLogout, onNavigate, canManage }: WelcomeScreenProps) {
  const fileInput = useRef<HTMLInputElement>(null);
  const [photoUrl, setPhotoUrl] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!profile.photoFileName) { setPhotoUrl(''); return; }
    let objectUrl = '';
    fetch(`${API_URL}/profile/photo`, { headers: { Authorization: `Bearer ${token}` } })
      .then(response => response.ok ? response.blob() : Promise.reject(new Error('Não foi possível carregar sua foto.')))
      .then(blob => { objectUrl = URL.createObjectURL(blob); setPhotoUrl(objectUrl); })
      .catch(cause => setError(cause instanceof Error ? cause.message : 'Não foi possível carregar sua foto.'));
    return () => { if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [profile.photoFileName, token]);

  async function choosePhoto(file?: File) {
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) { setError('Escolha uma imagem JPG, PNG ou WEBP.'); return; }
    if (file.size > 5 * 1024 * 1024) { setError('A imagem deve ter no máximo 5 MB.'); return; }
    setBusy(true); setError('');
    const form = new FormData(); form.append('file', file);
    try {
      const result = await api<Profile>('/profile/photo', token, { method: 'POST', body: form });
      onProfileChange(result);
      onContinue();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível enviar a foto.');
    } finally {
      setBusy(false);
      if (fileInput.current) fileInput.current.value = '';
    }
  }

  return (
    <div className="min-h-screen pb-16">
      <RibbonStrip />
      <a href="#welcome-main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-white focus:px-4 focus:py-3 focus:font-bold">Pular para o conteúdo</a>
      <header className="flex items-center justify-between border-b border-ink/10 px-[6vw] py-5">
        <span className="font-serif text-2xl font-black text-ink">Trampol<span className="text-brand-red">i</span>m</span>
        <AccountHeaderActions onProfile={() => onNavigate('profile')} onLogout={onLogout} />
      </header>
      <AccountNavigation active="profile" onNavigate={onNavigate} canManage={canManage} />
      <main id="welcome-main" className="mx-auto flex min-h-[75vh] max-w-3xl flex-col items-center justify-center py-14 text-center">
        <p className="text-sm font-extrabold uppercase tracking-[0.18em] text-brand-green">Vamos personalizar seu perfil</p>
        <h1 className="mt-3 text-4xl font-black md:text-5xl">Boas-vindas, {profile.name.split(' ')[0]}!</h1>
        <p className="mt-4 max-w-xl text-ink/65">Adicione uma foto para personalizar seu perfil. Depois, você poderá contar sua trajetória e incluir experiências, formação e currículo.</p>

        <div className="relative mt-9">
          <button type="button" aria-label="Adicionar ou trocar foto de perfil" onClick={() => fileInput.current?.click()} disabled={busy} className="group relative flex h-36 w-36 items-center justify-center overflow-hidden rounded-full border-4 border-brand-white bg-surface-raised shadow-lg ring-2 ring-brand-green/30 cursor-pointer disabled:opacity-60">
            {photoUrl ? <img src={photoUrl} alt="Foto do perfil" className="h-full w-full object-cover" /> : <span className="font-serif text-5xl font-black text-brand-blue">{profile.name.charAt(0).toUpperCase()}</span>}
            <span className="absolute bottom-0 right-0 flex h-11 w-11 items-center justify-center rounded-full border-4 border-brand-white bg-brand-red text-white" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5" stroke="currentColor" strokeWidth="2"><path d="M4 8h3l1.5-2h7L17 8h3v11H4z"/><circle cx="12" cy="13" r="3.5"/></svg>
            </span>
          </button>
          <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={event => choosePhoto(event.target.files?.[0])} />
        </div>
        <button onClick={() => fileInput.current?.click()} disabled={busy} className="mt-4 border-0 bg-transparent font-extrabold text-brand-red cursor-pointer disabled:opacity-60">{busy ? 'Enviando foto...' : profile.photoFileName ? 'Trocar foto' : 'Adicionar foto'}</button>
        <p className="mt-2 text-xs text-ink/50">JPG, PNG ou WEBP · até 5 MB</p>
        {error && <p role="alert" className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-brand-red">{error}</p>}
        <button onClick={onContinue} disabled={busy} className="mt-8 rounded-full border-0 bg-ink px-7 py-4 font-extrabold text-white cursor-pointer disabled:opacity-60">Continuar para meu perfil</button>
      </main>
    </div>
  );
}
