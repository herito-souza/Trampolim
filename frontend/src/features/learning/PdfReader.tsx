import { usePanelFocus } from './accessibleFocus';
import { useEffect, useRef, useState } from 'react';
import { getDocument, GlobalWorkerOptions, TextLayer } from 'pdfjs-dist';
import type { PDFDocumentProxy } from 'pdfjs-dist';
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import 'pdfjs-dist/web/pdf_viewer.css';
import { API_URL, api } from '../account/api';
import { buttonClass } from './types';
import { libraryRoot } from './libraryTypes';
import type { Material } from './libraryTypes';

GlobalWorkerOptions.workerSrc = workerUrl;
export default function PdfReader({ token, material, manage, onClose }: { token: string; material: Material; manage: boolean; onClose: () => void }) {
  const focusRegion = usePanelFocus();
  const [version, setVersion] = useState(''); const [document, setDocument] = useState<PDFDocumentProxy | null>(null);
  const [page, setPage] = useState(1); const [zoom, setZoom] = useState(1); const [width, setWidth] = useState(600);
  const [error, setError] = useState(''); const [loading, setLoading] = useState(true); const [retry, setRetry] = useState(0);
  const [blobUrl, setBlobUrl] = useState(''); const [alternative, setAlternative] = useState<{ alternativeText: string; extractedText: string } | null>(null);
  const [downloading, setDownloading] = useState(false);
  const canvas = useRef<HTMLCanvasElement>(null); const text = useRef<HTMLDivElement>(null); const container = useRef<HTMLDivElement>(null);
  const query = version ? `?version=${version}` : '';
  useEffect(() => { const node = container.current; if (!node) return; const observer = new ResizeObserver(entries => setWidth(Math.max(200, entries[0].contentRect.width - 8))); observer.observe(node); return () => observer.disconnect(); }, []);
  useEffect(() => {
    const controller = new AbortController(); let active = true; let url = ''; let task: ReturnType<typeof getDocument> | undefined;
    setLoading(true); setError(''); setDocument(null); setPage(1); setBlobUrl(''); setAlternative(null);
    void (async () => {
      try {
        const response = await fetch(`${API_URL}${libraryRoot}/materials/${material.id}/file${query}`, { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal });
        if (!response.ok) throw new Error('Este PDF está indisponível ou você não tem permissão para consultá-lo.');
        const bytes = await response.arrayBuffer(); if (!active) return;
        url = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' })); setBlobUrl(url);
        task = getDocument({ data: bytes, enableXfa: false });
        const pdf = await task.promise; if (active) setDocument(pdf);
      } catch (failure) { if (active) setError(failure instanceof Error ? failure.message : 'Não foi possível abrir o PDF.'); }
      finally { if (active) setLoading(false); }
    })();
    return () => { active = false; controller.abort(); if (url) URL.revokeObjectURL(url); void task?.destroy(); };
  }, [token, material.id, query, retry]);
  useEffect(() => {
    if (!document || !canvas.current || !text.current) return;
    let cancelled = false; let render: ReturnType<Awaited<ReturnType<PDFDocumentProxy['getPage']>>['render']> | undefined; let layer: TextLayer | undefined;
    void (async () => {
      try {
        const pdfPage = await document.getPage(page); if (cancelled || !canvas.current || !text.current) return;
        const natural = pdfPage.getViewport({ scale: 1 }); const viewport = pdfPage.getViewport({ scale: Math.min(width / natural.width, 1.5) * zoom });
        const target = canvas.current; const ratio = Math.min(window.devicePixelRatio || 1, 2);
        target.width = Math.ceil(viewport.width * ratio); target.height = Math.ceil(viewport.height * ratio); target.style.width = `${viewport.width}px`; target.style.height = `${viewport.height}px`;
        text.current.replaceChildren(); text.current.style.width = `${viewport.width}px`; text.current.style.height = `${viewport.height}px`; text.current.style.setProperty('--scale-factor', String(viewport.scale)); text.current.style.setProperty('--total-scale-factor', String(viewport.scale));
        render = pdfPage.render({ canvas: target, viewport, transform: ratio === 1 ? undefined : [ratio, 0, 0, ratio, 0, 0] }); await render.promise;
        if (cancelled) return; layer = new TextLayer({ textContentSource: await pdfPage.getTextContent(), container: text.current!, viewport }); await layer.render();
      } catch (failure) { if (!cancelled) setError(failure instanceof Error ? failure.message : 'Falha ao renderizar a página.'); }
    })();
    return () => { cancelled = true; render?.cancel(); layer?.cancel(); };
  }, [document, page, zoom, width]);
  async function download() {
    setDownloading(true); setError('');
    try { const response = await fetch(`${API_URL}${libraryRoot}/materials/${material.id}/file?download=true${version ? `&version=${version}` : ''}`, { headers: { Authorization: `Bearer ${token}` } }); if (!response.ok) throw new Error('Download não autorizado ou documento indisponível.'); const url = URL.createObjectURL(await response.blob()); const link = window.document.createElement('a'); link.href = url; link.download = `material-${material.id}.pdf`; link.click(); setTimeout(() => URL.revokeObjectURL(url), 10000); }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'Falha no download.'); } finally { setDownloading(false); }
  }
  return <section ref={focusRegion} tabIndex={-1} aria-label={`Leitura de ${material.title}`} className="min-w-0 space-y-4 rounded-2xl border border-brand-blue/30 p-3 md:p-5">
    <header className="flex flex-wrap items-start justify-between gap-3"><div><h4 className="text-2xl font-bold">{material.title}</h4><p className="text-sm text-ink/70">{material.teacher_name} · {material.course_title}</p></div><button className={buttonClass} onClick={onClose}>Fechar documento</button></header>
    <p className="whitespace-pre-line text-sm text-ink/70">{material.description}</p>
    {manage && !!material.versions?.length && <label className="block">Versão do PDF<select className="ml-2 rounded-lg bg-[#121c2b] p-2 text-ink" value={version} onChange={event => setVersion(event.target.value)}><option value="">Versão atual</option>{material.versions.map(item => <option key={item.id} value={item.id}>Versão {item.version_number} · {new Date(item.created_at).toLocaleDateString('pt-BR')}</option>)}</select></label>}
    <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Controles do PDF"><button className={buttonClass} disabled={!document || page <= 1} onClick={() => setPage(value => value - 1)}>Página anterior</button><label className="flex items-center gap-2">Página<input className="w-16 rounded-lg bg-[#121c2b] p-2 text-ink" aria-label="Página do PDF" type="number" min={1} max={document?.numPages || 1} value={page} onChange={event => { const value = Number(event.target.value); if (value >= 1 && value <= (document?.numPages || 1)) setPage(value); }} /></label><span aria-live="polite">de {document?.numPages || '…'}</span><button className={buttonClass} disabled={!document || page >= document.numPages} onClick={() => setPage(value => value + 1)}>Próxima página</button><label>Zoom<select aria-label="Zoom do PDF" className="ml-2 rounded-lg bg-[#121c2b] p-2 text-ink" value={zoom} onChange={event => setZoom(Number(event.target.value))}>{[0.75, 1, 1.25, 1.5, 2].map(value => <option key={value} value={value}>{value * 100}%</option>)}</select></label>
      {blobUrl && <a href={blobUrl} target="_blank" rel="noopener noreferrer" className={buttonClass}>Abrir em visualizador compatível</a>}{(manage || material.allow_download) && <button className={buttonClass} disabled={downloading || !document} onClick={() => void download()}>Baixar PDF</button>}
    </div>
    {loading && <p role="status">Carregando PDF…</p>}{error && <div role="alert"><p className="text-red-200">{error}</p><button className={buttonClass} onClick={() => setRetry(value => value + 1)}>Tentar abrir novamente</button></div>}
    <div ref={container} className="max-w-full overflow-auto rounded-xl bg-slate-800 p-1" tabIndex={0} aria-label="Páginas do documento"><div className="relative mx-auto w-fit bg-white"><canvas ref={canvas} aria-label={`Página ${page} do PDF`} /><div ref={text} className="textLayer" /></div></div>
    <p className="text-sm text-ink/65">{!material.has_text ? 'Este PDF não possui texto selecionável detectado.' : !material.tagged ? 'O PDF possui texto, mas não foi detectada estrutura de acessibilidade.' : 'O PDF possui marcações estruturais; sua acessibilidade completa não foi certificada.'} Consulte a alternativa textual quando disponível.</p>
    <button className={buttonClass} onClick={() => api<{ alternativeText: string; extractedText: string }>(`${libraryRoot}/materials/${material.id}/text${query}`, token).then(setAlternative).catch(failure => setError(failure.message))}>Consultar alternativa textual</button>
    {alternative && <div className="space-y-3 rounded-xl border border-white/20 p-4">{alternative.alternativeText ? <><h5 className="font-bold">Alternativa fornecida pelo professor</h5><p className="whitespace-pre-wrap">{alternative.alternativeText}</p></> : <p>O professor ainda não forneceu uma alternativa textual.</p>}{alternative.extractedText ? <details><summary className="cursor-pointer font-bold">Texto extraído automaticamente — ordem e conteúdo não verificados</summary><p className="mt-3 whitespace-pre-wrap">{alternative.extractedText}</p></details> : <p>Não foi possível extrair texto. Solicite uma versão acessível ao professor pelo mural.</p>}</div>}
  </section>;
}
