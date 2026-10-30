import { Component, Suspense } from 'react';
import type { ReactNode } from 'react';
import { buttonClass } from './types';
export class LearningPanelBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  render() { return this.state.failed ? <div role="alert" className="rounded-xl border border-red-300/30 p-4"><p>Não foi possível abrir esta ferramenta. Verifique a conexão e recarregue o ambiente. Dados já salvos permanecem disponíveis.</p><button className={buttonClass} onClick={() => window.location.reload()}>Recarregar ambiente</button></div> : <Suspense fallback={<p role="status">Carregando ferramenta de aprendizagem…</p>}>{this.props.children}</Suspense>; }
}
