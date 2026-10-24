import { defineConfig } from 'astro/config';
export default defineConfig({
  site: 'https://karhutla-kalimantan.pages.dev',
  output: 'static',
  vite: { build: { target: 'es2022' } },
});
