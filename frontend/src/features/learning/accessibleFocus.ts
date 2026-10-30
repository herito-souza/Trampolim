import { useEffect, useRef } from 'react';
export function useErrorFocus(error: string) { const ref = useRef<HTMLParagraphElement>(null); useEffect(() => { if (error) ref.current?.focus(); }, [error]); return ref; }
export function usePanelFocus() {
  const ref = useRef<HTMLElement>(null);
  useEffect(() => { const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null; const node = ref.current; const frame = requestAnimationFrame(() => { if (node) { node.tabIndex = -1; node.focus(); } }); return () => { cancelAnimationFrame(frame); if (previous?.isConnected && (document.activeElement === document.body || node?.contains(document.activeElement))) previous.focus(); }; }, []);
  return ref;
}
export function useDialogFocus(open: boolean, close: () => void) {
  const ref = useRef<HTMLDivElement>(null); const latestClose = useRef(close); latestClose.current = close;
  useEffect(() => { if (!open) return; const dialog = ref.current; if (!dialog) return; const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null; const altered: { node: HTMLElement; inert: boolean }[] = []; let branch: HTMLElement = dialog;
    while (branch.parentElement && branch.parentElement !== document.documentElement) { for (const child of branch.parentElement.children) if (child !== branch && child instanceof HTMLElement) { altered.push({ node: child, inert: child.inert }); child.inert = true; } branch = branch.parentElement; }
    const getTargets = () => Array.from(dialog.querySelectorAll<HTMLElement>('button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex="0"]')).filter(node => !node.hidden && node.getClientRects().length);
    (getTargets()[0] || dialog).focus(); const key = (event: KeyboardEvent) => { if (event.key === 'Escape') { event.preventDefault(); latestClose.current(); } if (event.key === 'Tab') { const nodes = getTargets(); const first = nodes[0], last = nodes[nodes.length - 1]; if (!first) { event.preventDefault(); dialog.focus(); } else if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog)) { event.preventDefault(); last.focus(); } else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); } } };
    dialog.addEventListener('keydown', key); return () => { dialog.removeEventListener('keydown', key); altered.forEach(item => { item.node.inert = item.inert; }); if (previous?.isConnected) previous.focus(); };
  }, [open]);return ref;
}
