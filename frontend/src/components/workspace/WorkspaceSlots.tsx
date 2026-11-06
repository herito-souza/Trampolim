import {createPortal} from 'react-dom';
import type {ComponentProps,ReactNode} from 'react';
import {useWorkspace} from './workspaceContext';
export function LegacyPageHeader(props:ComponentProps<'header'>){const shell=useWorkspace();return shell?null:<header {...props}/>}
export function NotificationSlot({children}:{children:ReactNode}){const shell=useWorkspace();return shell?(shell.notificationHost?createPortal(children,shell.notificationHost):null):children}
