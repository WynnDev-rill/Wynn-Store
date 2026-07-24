export const SITE_URL = 'https://wynn-store.pages.dev';
export const SITE_NAME = 'Wynn Store';
export const DEFAULT_OG_IMAGE = `${SITE_URL}/og-cover.svg`;

export interface PageSeo {
  title: string;
  description: string;
  noIndex?: boolean;
}

export const pageSeo: Record<string, PageSeo> = {
  '/': {
    title: 'Top Up Game Cepat & Aman | Wynn Store',
    description: 'Top up Mobile Legends, Free Fire, PUBG Mobile, dan game favorit lainnya dengan proses cepat, harga hemat, dan pengalaman premium.',
  },
  '/games': {
    title: 'Daftar Game untuk Top Up | Wynn Store',
    description: 'Temukan katalog game populer dan pilih nominal top up yang sesuai kebutuhanmu di Wynn Store.',
  },
  '/promo': {
    title: 'Promo Top Up Game Terbaru | Wynn Store',
    description: 'Dapatkan promo, diskon, dan cashback top up game pilihan terbaru dari Wynn Store.',
  },
  '/history': {
    title: 'Riwayat Transaksi | Wynn Store',
    description: 'Lihat riwayat simulasi transaksi top up game Wynn Store dengan mudah.',
    noIndex: true,
  },
  '/profile': {
    title: 'Profil Pengguna | Wynn Store',
    description: 'Kelola informasi profil pengguna simulasi Wynn Store.',
    noIndex: true,
  },
  '/cart': {
    title: 'Keranjang Top Up | Wynn Store',
    description: 'Periksa produk dan nominal top up game di keranjang Wynn Store sebelum checkout.',
    noIndex: true,
  },
  '/checkout': {
    title: 'Checkout Pesanan | Wynn Store',
    description: 'Tinjau ringkasan pesanan dan metode pembayaran simulasi Wynn Store.',
    noIndex: true,
  },
  '/admin': {
    title: 'Dashboard Admin | Wynn Store',
    description: 'Dashboard administrasi simulasi Wynn Store.',
    noIndex: true,
  },
};
