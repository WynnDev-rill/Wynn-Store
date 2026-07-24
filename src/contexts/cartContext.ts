import { createContext } from 'react';
import type { CartItem, Game, Product } from '../types';

export interface CartContextValue {
  items: CartItem[];
  addItem: (product: Product, game: Game, userId: string, serverId?: string) => void;
  removeItem: (id: string) => void;
  total: number;
  count: number;
  clear: () => void;
}

export const CartContext = createContext<CartContextValue | null>(null);
