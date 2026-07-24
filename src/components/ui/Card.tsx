import type { HTMLAttributes } from 'react';
export function Card({className='',...props}:HTMLAttributes<HTMLDivElement>){return <div className={`glass rounded-2xl ${className}`} {...props}/>}
