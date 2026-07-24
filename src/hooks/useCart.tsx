import { useContext } from 'react';
import { CartContext } from '../contexts/cartContext';

export function useCart() {
  const value = useContext(CartContext);
  if (!value) throw new Error('useCart must be used inside CartProvider');
  return value;
}
