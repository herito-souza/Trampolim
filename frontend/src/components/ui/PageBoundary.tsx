import { Component } from 'react';
import type { ReactNode, ErrorInfo } from 'react';

/** A failed network chunk must not turn the selected area into a blank screen. */
export class PageBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  componentDidCatch(_error: Error, _info: ErrorInfo) { /* Never log tokens or private component state. */ }
  render() {
    if (this.state.failed) return <main className="grid min-h-screen place-items-center p-8"><section className="max-w-lg space-y-4" role="alert"><h1 className="text-3xl font-bold">Não foi possível abrir esta área</h1><p>Verifique sua conexão e recarregue a página. Os dados já salvos continuam disponíveis.</p><button className="rounded-full border border-white/30 px-5 py-3 font-bold" onClick={() => window.location.reload()}>Recarregar página</button></section></main>;
    return this.props.children;
  }
}
