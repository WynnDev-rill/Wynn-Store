import { LoaderCircle, PackageOpen, TriangleAlert } from 'lucide-react';
export function Loading(){return <div className="flex min-h-64 items-center justify-center"><LoaderCircle className="animate-spin text-violet-500" aria-label="Memuat"/></div>}
export function Skeleton({className=''}:{className?:string}){return <div className={`animate-pulse rounded-xl bg-zinc-200 dark:bg-white/10 ${className}`}/>}
export function EmptyState({message='Belum ada data'}:{message?:string}){return <div className="py-16 text-center text-muted"><PackageOpen className="mx-auto mb-3"/><p>{message}</p></div>}
export function ErrorState({message='Terjadi kesalahan'}:{message?:string}){return <div className="py-16 text-center text-red-400"><TriangleAlert className="mx-auto mb-3"/><p>{message}</p></div>}
