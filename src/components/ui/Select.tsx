import type { SelectHTMLAttributes } from 'react';
export function Select({className='',...props}:SelectHTMLAttributes<HTMLSelectElement>){return <select className={`w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm outline-none dark:border-white/10 dark:bg-zinc-900 ${className}`} {...props}/>}
