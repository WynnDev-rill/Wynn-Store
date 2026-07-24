import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { DEFAULT_OG_IMAGE, pageSeo, SITE_NAME, SITE_URL, type PageSeo } from '../../constants/seo';
import { games } from '../../data/games';

function setMeta(selector: string, attributes: Record<string, string>) {
  let element = document.head.querySelector<HTMLMetaElement>(selector);
  if (!element) {
    element = document.createElement('meta');
    document.head.appendChild(element);
  }
  Object.entries(attributes).forEach(([key, value]) => element?.setAttribute(key, value));
}

function setCanonical(url: string) {
  let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!canonical) {
    canonical = document.createElement('link');
    canonical.rel = 'canonical';
    document.head.appendChild(canonical);
  }
  canonical.href = url;
}

function resolveSeo(pathname: string): PageSeo {
  if (pathname.startsWith('/games/')) {
    const game = games.find(({ id }) => pathname === `/games/${id}`);
    if (game) {
      return {
        title: `Top Up ${game.title} Murah & Cepat | ${SITE_NAME}`,
        description: `Top up ${game.title} dengan proses cepat, pilihan nominal lengkap, dan harga kompetitif di ${SITE_NAME}.`,
      };
    }
  }

  return pageSeo[pathname] ?? {
    title: `Halaman Tidak Ditemukan | ${SITE_NAME}`,
    description: 'Halaman yang kamu cari tidak ditemukan di Wynn Store.',
    noIndex: true,
  };
}

export function SeoManager() {
  const { pathname } = useLocation();

  useEffect(() => {
    const seo = resolveSeo(pathname);
    const canonicalUrl = `${SITE_URL}${pathname === '/' ? '' : pathname}`;

    document.title = seo.title;
    document.documentElement.lang = 'id';
    setCanonical(canonicalUrl);
    setMeta('meta[name="description"]', { name: 'description', content: seo.description });
    setMeta('meta[name="robots"]', { name: 'robots', content: seo.noIndex ? 'noindex, nofollow' : 'index, follow, max-image-preview:large' });
    setMeta('meta[property="og:title"]', { property: 'og:title', content: seo.title });
    setMeta('meta[property="og:description"]', { property: 'og:description', content: seo.description });
    setMeta('meta[property="og:url"]', { property: 'og:url', content: canonicalUrl });
    setMeta('meta[property="og:image"]', { property: 'og:image', content: DEFAULT_OG_IMAGE });
    setMeta('meta[name="twitter:title"]', { name: 'twitter:title', content: seo.title });
    setMeta('meta[name="twitter:description"]', { name: 'twitter:description', content: seo.description });
    setMeta('meta[name="twitter:image"]', { name: 'twitter:image', content: DEFAULT_OG_IMAGE });

    let schema = document.head.querySelector<HTMLScriptElement>('#page-schema');
    if (!schema) {
      schema = document.createElement('script');
      schema.id = 'page-schema';
      schema.type = 'application/ld+json';
      document.head.appendChild(schema);
    }
    schema.textContent = JSON.stringify({
      '@context': 'https://schema.org',
      '@type': pathname.startsWith('/games/') ? 'Product' : 'WebPage',
      name: seo.title,
      description: seo.description,
      url: canonicalUrl,
      isPartOf: { '@type': 'WebSite', name: SITE_NAME, url: SITE_URL },
    });
  }, [pathname]);

  return null;
}
