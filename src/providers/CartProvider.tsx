import { useMemo, useState, type ReactNode } from 'react';
import { CartContext } from '../contexts/cartContext';
import type { CartItem, Game, Product } from '../types';

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const value = useMemo(
    () => ({
      items,
      addItem: (product: Product, game: Game, userId: string, serverId?: string) =>
        setItems((currentItems) => {
          const existing = currentItems.find((item) => item.product.id === product.id);
          return existing
            ? currentItems.map((item) =>
                item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item,
              )
            : [...currentItems, { product, game, quantity: 1, userId, serverId }];
        }),
      removeItem: (id: string) => setItems((currentItems) => currentItems.filter((item) => item.product.id !== id)),
      total: items.reduce((sum, item) => sum + item.product.price * item.quantity, 0),
      count: items.reduce((sum, item) => sum + item.quantity, 0),
      clear: () => setItems([]),
    }),
    [items],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
