export interface Game { id: string; title: string; publisher: string; category: string; image: string; accent: string; description: string; rating: number; sold: string; }
export interface Product { id: string; gameId: string; amount: string; bonus?: string; price: number; }
export interface CartItem { product: Product; game: Game; quantity: number; userId: string; serverId?: string; }
export interface Promo { id: string; title: string; description: string; code: string; discount: string; }
