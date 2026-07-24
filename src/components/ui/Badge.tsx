import type { ReactNode } from 'react';
export function Badge({children,className=''}:{children:ReactNode;className?:string}){return <span className={`inline-flex rounded-full border border-violet-400/20 bg-violet-500/10 px-2.5 py-1 text-xs font-bold text-violet-600 dark:text-violet-300 ${className}`}>{children}</span>}
