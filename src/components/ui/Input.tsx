import type { InputHTMLAttributes } from 'react';
export function Input({className='',...props}:InputHTMLAttributes<HTMLInputElement>){return <input className={`w-full rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-3 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-500/20 dark:border-white/10 dark:bg-white/5 ${className}`} {...props}/>}
