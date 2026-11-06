import {useWorkspace} from '../workspace/workspaceContext';
export function RibbonStrip() {
  const shell=useWorkspace();
  if(shell)return null;
  return (
    <div className="h-1 bg-gradient-to-r from-brand-yellow via-brand-green to-brand-yellow">
    </div>
  );
}
