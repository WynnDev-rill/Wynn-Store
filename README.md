# Wynn Store

Simulasi website top up game premium yang dibangun dengan React, TypeScript, Vite, Tailwind CSS, dan React Router.

## Persyaratan

- Node.js 20.19 atau versi LTS yang lebih baru
- npm 10 atau lebih baru

Gunakan versi Node yang ditentukan proyek:

```bash
nvm use
```

## Menjalankan proyek secara lokal

```bash
npm install
npm run dev
```

## Validasi dan production build

```bash
npm run typecheck
npm run lint
npm run build
npm run preview
```

Hasil production build berada di direktori `dist/`. File `public/_redirects`
memastikan route React Router tetap dapat dibuka langsung ketika aplikasi
ditayangkan sebagai situs statis.

## Deploy ke GitHub Actions

Pada runner GitHub dengan akses npm registry, langkah build minimumnya adalah:

```yaml
- uses: actions/setup-node@v4
  with:
    node-version-file: .nvmrc
- run: npm install
- run: npm run lint
- run: npm run build
```

Setelah `package-lock.json` pertama kali berhasil dibuat dan di-commit dari
environment yang memiliki akses registry, ganti `npm install` pada CI menjadi
`npm ci` agar instalasi reproducible.

## Deploy ke Cloudflare Pages

Hubungkan repository ke Cloudflare Pages dan gunakan konfigurasi berikut:

| Pengaturan | Nilai |
| --- | --- |
| Framework preset | Vite |
| Build command | `npm run build` |
| Build output directory | `dist` |
| Root directory | `/` |
| Environment variable | `NODE_VERSION=20.19.0` |

## Catatan environment Codex

Jika instalasi di Codex menghasilkan `403 Forbidden`, periksa dengan
`npm config list` dan `curl -I https://registry.npmjs.org/react`. Pada sesi
pengembangan proyek ini, seluruh trafik npm dipaksa melalui proxy environment
dan proxy tersebut menolak tunnel ke `registry.npmjs.org`; akses langsung juga
tidak memiliki DNS. Ini merupakan pembatasan jaringan environment, bukan error
dari manifest npm atau konfigurasi Vite.

> Proyek ini hanya simulasi UI. Tidak ada pembayaran atau transaksi nyata yang diproses.
