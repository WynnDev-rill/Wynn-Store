/* Production integration checks: real Chromium, built assets, production CSP. */
const { chromium } = require('playwright');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');

const root = path.resolve('dist');
const out = path.resolve('test-results');
const report = { checks: [], errors: [], screenshots: [], environment: 'Chromium software WebGL; viewport emulation, not a physical phone' };
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const digest = buffer => createHash('sha256').update(buffer).digest('hex');
const hero = '[data-cinematic-landscape]';
const forest = '[data-forest-world]';
let server, browser, base;

async function check(name, run) {
  const start = Date.now();
  try {
    const detail = await run();
    report.checks.push({ name, pass: true, ms: Date.now() - start, ...(detail ? { detail } : {}) });
    console.log('PASS', name);
  } catch (error) {
    report.checks.push({ name, pass: false, ms: Date.now() - start, error: error.message });
    throw error;
  }
}

async function forestState(page) {
  return page.locator(forest).evaluate(element => {
    const section = element.closest('section');
    const rect = section.getBoundingClientRect();
    return { scrollY, height: innerHeight, sectionTop: rect.top, sectionHeight: rect.height, progress: section.style.getPropertyValue('--scene-progress'), camera: element.dataset.camera, motion: element.dataset.motion, ready: element.dataset.ready, globalMotion: document.body.dataset.cinematicMotion };
  });
}

async function session(options = {}, expectedErrors = []) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, ...options });
  context.setDefaultTimeout(45000);
  context.setDefaultNavigationTimeout(60000);
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', error => {
    const detail = `${page.url()}\n${error.stack || error.message}`;
    errors.push(detail);
    console.error('BROWSER PAGE ERROR', detail);
  });
  page.on('console', message => {
    if (message.type() === 'error' && !expectedErrors.some(pattern => pattern.test(message.text()))) errors.push(message.text());
  });
  page.on('response', response => {
    if (response.status() >= 400 && !expectedErrors.some(pattern => pattern.test(response.url()))) errors.push(`HTTP ${response.status()} ${response.url()}`);
  });
  return {
    context, page,
    async close() {
      await context.close();
      report.errors.push(...errors);
      assert.deepEqual(errors, [], 'Unexpected browser errors: ' + errors.join('; '));
    },
  };
}

async function ready(page, selector) {
  await page.waitForFunction(selector => document.querySelector(selector)?.dataset.ready === 'true', selector, { timeout: 30000 });
  assert.equal(await page.locator(selector + ' canvas').count(), 1);
}

async function gotoForest(page, fraction = .15) {
  await page.locator('#ruang').evaluate((section, fraction) => {
    window.scrollTo({ top: window.scrollY + section.getBoundingClientRect().top + fraction * Math.max(0, section.clientHeight - innerHeight), behavior: 'instant' });
  }, fraction);
  await ready(page, forest);
}

async function shot(page, name) {
  const filename = 'cinematic-' + name + '.png';
  await page.screenshot({ path: path.join(out, filename) });
  report.screenshots.push(filename);
}

async function stableCanvas(page, selector) {
  // Browser compositor pixels catch continuing renders even when the WebGL
  // drawing buffer is cleared and canvas.toDataURL() would be falsely stable.
  const canvas = page.locator(selector + ' canvas');
  const state = () => page.locator(selector).evaluate(element => ({ camera: element.dataset.camera || '', progress: element.dataset.cameraProgress || '', globalMotion: document.body.dataset.cinematicMotion }));
  await sleep(1200);
  const firstState = await state();
  const firstPixels = await canvas.screenshot({ animations: 'allow' });
  await sleep(400);
  const secondPixels = await canvas.screenshot({ animations: 'allow' });
  const secondState = await state();
  assert.deepEqual(secondState, firstState, selector + ' camera changes while paused');
  // Chromium/SwiftShader compositor crops can vary by 1–2 RGB levels despite
  // identical camera state (verified diff: max delta2; no geometry movement).
  // Decode through the browser, require ZERO pixels beyond a 3-level tolerance.
  // This avoids mistaking GPU quantization for motion without ignoring moving
  // silhouettes, lights or particles. No test-only renderer changes are made.
  const difference = await page.evaluate(async ({ first, second }) => {
    const decode = async data => {
      const image = new Image(); image.src = 'data:image/png;base64,' + data; await image.decode();
      const target = document.createElement('canvas'); target.width = image.width; target.height = image.height;
      const context = target.getContext('2d'); context.drawImage(image, 0, 0);
      return { width: image.width, height: image.height, data: context.getImageData(0, 0, image.width, image.height).data };
    };
    const a = await decode(first), b = await decode(second);
    if (a.width !== b.width || a.height !== b.height) return { dimensionMismatch: true };
    let overTolerance = 0, maximum = 0;
    for (let index = 0; index < a.data.length; index += 4) {
      const delta = Math.max(Math.abs(a.data[index] - b.data[index]), Math.abs(a.data[index + 1] - b.data[index + 1]), Math.abs(a.data[index + 2] - b.data[index + 2]), Math.abs(a.data[index + 3] - b.data[index + 3]));
      maximum = Math.max(maximum, delta); if (delta > 3) overTolerance++;
    }
    return { maximum, overTolerance, pixels: a.width * a.height };
  }, { first: firstPixels.toString('base64'), second: secondPixels.toString('base64') });
  if (difference.dimensionMismatch || difference.overTolerance) {
    const label = selector.includes('landscape') ? 'hero' : 'forest';
    fs.writeFileSync(path.join(out, `cinematic-pause-${label}-first.png`), firstPixels);
    fs.writeFileSync(path.join(out, `cinematic-pause-${label}-second.png`), secondPixels);
    console.log('PAUSE pixel difference', selector, JSON.stringify(await page.evaluate(selector => ({ scrollY, camera: document.querySelector(selector).dataset, animations: document.getAnimations().filter(animation => animation.playState === 'running').map(animation => ({ target: animation.effect?.target?.className, time: animation.currentTime })) }), selector)));
  }
  console.log('PAUSE pixel comparison', selector, JSON.stringify(difference));
  assert(!difference.dimensionMismatch, 'Paused canvas dimensions must remain stable');
  assert.equal(difference.overTolerance, 0, selector + ' continues moving while paused');
}

async function contextCycle(page, selector, fallback) {
  const canvas = page.locator(selector + ' canvas');
  const supported = await canvas.evaluate(canvas => {
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
    const extension = gl?.getExtension('WEBGL_lose_context');
    if (!extension) return false;
    window.__cinematicLossExtension = extension;
    extension.loseContext();
    return true;
  });
  assert(supported, 'WEBGL_lose_context must be available in this verification browser');
  await page.waitForFunction(selector => document.querySelector(selector)?.dataset.ready === 'false', selector);
  assert(await page.locator(fallback).isVisible(), 'Static fallback must be visible after actual context loss');
  assert.equal(await canvas.evaluate(canvas => getComputedStyle(canvas).visibility), 'hidden');
  await sleep(250);
  await page.evaluate(() => window.__cinematicLossExtension.restoreContext());
  await ready(page, selector);
  assert.equal(await page.locator(fallback).evaluate(element => getComputedStyle(element).visibility), 'hidden');
}

async function run() {
  assert(fs.existsSync(path.join(root, 'index.html')), 'Build dist first: npm run build');
  fs.mkdirSync(out, { recursive: true });
  const policy = fs.readFileSync(path.join(root, '_headers'), 'utf8').split('\n').find(line => line.trim().startsWith('Content-Security-Policy:'))?.trim().slice('Content-Security-Policy:'.length).trim();
  assert(policy, 'The production CSP must be present');
  const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'application/javascript', '.jpg': 'image/jpeg', '.png': 'image/png', '.webp': 'image/webp', '.woff2': 'font/woff2', '.svg': 'image/svg+xml', '.json': 'application/json', '.geojson': 'application/geo+json', '.csv': 'text/csv', '.vtt': 'text/vtt', '.webm': 'video/webm', '.glb': 'model/gltf-binary' };
  server = http.createServer((request, response) => {
    try {
      const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
      let file = path.resolve(root, '.' + pathname);
      if (file !== root && !file.startsWith(root + path.sep)) { response.writeHead(403).end(); return; }
      if (fs.existsSync(file) && fs.statSync(file).isDirectory()) file = path.join(file, 'index.html');
      if (!fs.existsSync(file) || !fs.statSync(file).isFile()) { file = path.join(root, '404.html'); response.statusCode = 404; }
      response.setHeader('Content-Security-Policy', policy);
      response.setHeader('X-Content-Type-Options', 'nosniff');
      response.setHeader('Content-Type', types[path.extname(file)] || 'application/octet-stream');
      response.setHeader('Content-Length', fs.statSync(file).size);
      if (request.method === 'HEAD') { response.end(); return; }
      fs.createReadStream(file).on('error', () => response.destroy()).pipe(response);
    } catch { response.writeHead(400).end('Bad request'); }
  });
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(0, '127.0.0.1', resolve); });
  base = 'http://127.0.0.1:' + server.address().port;
  const executablePath = process.env.BROWSER_EXECUTABLE_PATH || (fs.existsSync('/usr/bin/chromium') ? '/usr/bin/chromium' : undefined);
  browser = await chromium.launch({ ...(executablePath ? { executablePath } : {}), headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage', '--enable-webgl', '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });

  const normal = await session();
  const page = normal.page;
  await page.goto(base, { waitUntil: 'networkidle' });
  await check('Desktop hero renders under production CSP', async () => {
    await ready(page, hero);
    assert(await page.locator('h1').isVisible());
    assert.match(await page.locator('h1').getAttribute('aria-label'), /Kalimantan/);
    await shot(page, 'desktop-hero');
  });
  await check('Hero camera follows forward and reverse native scroll', async () => {
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
    await sleep(1000);
    const before = Number(await page.locator(hero).getAttribute('data-camera-progress'));
    await page.evaluate(() => window.scrollTo({ top: 330, behavior: 'instant' }));
    await page.waitForFunction(({ selector, before }) => Number(document.querySelector(selector).dataset.cameraProgress) > before + .12, { selector: hero, before });
    const forward = Number(await page.locator(hero).getAttribute('data-camera-progress'));
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
    await page.waitForFunction(({ selector, forward }) => Number(document.querySelector(selector).dataset.cameraProgress) < forward - .1, { selector: hero, forward });
    return { before, forward, reverse: Number(await page.locator(hero).getAttribute('data-camera-progress')) };
  });
  await check('Global pause freezes hero pixels and resume restarts camera', async () => {
    await page.locator('[data-cinematic-pause]').click();
    assert.equal(await page.locator('body').getAttribute('data-cinematic-motion'), 'paused');
    await stableCanvas(page, hero);
    await page.locator('[data-cinematic-pause]').click();
    await page.evaluate(() => window.scrollTo({ top: 250, behavior: 'instant' }));
    await page.waitForFunction(selector => Number(document.querySelector(selector).dataset.cameraProgress) > .18, hero);
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
  });
  await check('Hero context loss exposes fallback and restoration recovers', async () => contextCycle(page, hero, '.landscape-fallback'));
  await gotoForest(page);
  await check('Forest renders and camera travels in both scroll directions', async () => {
    await sleep(1200);
    const first = await page.locator(forest).getAttribute('data-camera');
    console.log('FOREST start', JSON.stringify(await forestState(page)));
    await gotoForest(page, .8);
    console.log('FOREST forward target', JSON.stringify(await forestState(page)));
    try {
      await page.waitForFunction(({ selector, z }) => Number(document.querySelector(selector).dataset.camera?.split(',')[2]) < Math.min(0,z - 1), { selector: forest, z: Number(first.split(',')[2]) });
    } catch (error) {
      console.log('FOREST forward timeout', JSON.stringify(await forestState(page)));
      await shot(page, 'forest-forward-failure');
      throw error;
    }
    const forward = await page.locator(forest).getAttribute('data-camera');
    assert.notEqual(forward, first);
    await gotoForest(page, .15);
    console.log('FOREST reverse target', JSON.stringify(await forestState(page)));
    try {
      await page.waitForFunction(({ selector, z }) => Number(document.querySelector(selector).dataset.camera?.split(',')[2]) > Math.max(0,z + 1), { selector: forest, z: Number(forward.split(',')[2]) });
    } catch (error) {
      console.log('FOREST reverse timeout', JSON.stringify(await forestState(page)));
      await shot(page, 'forest-reverse-failure');
      throw error;
    }
    assert.notEqual(await page.locator(forest).getAttribute('data-camera'), forward);
    await shot(page, 'desktop-forest');
    return await forestState(page);
  });
  await check('Forest stage controls settle into three distinct compositions', async () => {
    const pictures = [];
    for (let index = 0; index < 3; index++) {
      await page.locator(`button[data-forest-stage="${index}"]`).click();
      await sleep(1600);
      const selected = page.locator('button[data-forest-stage][aria-pressed="true"]');
      assert.equal(await selected.count(), 1, 'Exactly one stage must be selected');
      assert.equal(await selected.getAttribute('data-forest-stage'), String(index));
      assert.match(await page.locator('[data-forest-status]').textContent(), [/Sebelum api/, /Di dalam asap/, /Setelah hujan/][index]);
      await page.locator('[data-cinematic-pause]').click();
      assert.equal(await page.locator('body').getAttribute('data-cinematic-motion'), 'paused');
      await sleep(250);
      pictures.push(digest(await page.locator(forest + ' canvas').screenshot()));
      await shot(page, 'desktop-stage-' + index);
      await page.locator('[data-cinematic-pause]').click();
    }
    assert.equal(new Set(pictures).size, 3, 'Each settled stage needs a distinct visible result');
  });
  await check('Forest keyboard remains focused and consumes navigation arrows', async () => {
    await page.locator(forest).focus();
    const before = await page.evaluate(() => scrollY);
    await page.keyboard.press('ArrowRight');
    await page.keyboard.press('ArrowUp');
    await sleep(250);
    assert.equal(await page.evaluate(() => scrollY), before, 'Arrow keys should control the forest rather than scroll the page');
    assert.equal(await page.locator(forest).evaluate(element => element === document.activeElement), true);
    await page.keyboard.press('Escape');
  });
  await check('Global pause freezes forest pixels and resumes rendering', async () => {
    await page.locator('[data-cinematic-pause]').click();
    assert.equal(await page.locator(forest).getAttribute('data-motion'), 'paused');
    await stableCanvas(page, forest);
    const pausedCamera = await page.locator(forest).getAttribute('data-camera');
    await page.locator('[data-cinematic-pause]').click();
    await gotoForest(page, .7);
    await page.waitForFunction(({ selector, pausedCamera }) => document.querySelector(selector).dataset.camera !== pausedCamera && document.querySelector(selector).dataset.motion === 'running', { selector: forest, pausedCamera });
  });
  await check('Forest context loss exposes fallback and restoration recovers', async () => contextCycle(page, forest, '.forest-fallback'));
  await check('Pausing an entrance keeps heading readable and resume never replays completed words', async () => {
    const heading = page.locator('h2[data-reveal]:not(.in-view)').first();
    const id = await heading.getAttribute('id');
    assert(id, 'An unvisited heading is required for the entrance regression check');
    await heading.evaluate(element => window.scrollTo({ top: scrollY + element.getBoundingClientRect().top - 200, behavior: 'instant' }));
    // Pause in the same browser callback that observes the active entrance;
    // this avoids a tool round trip accidentally letting the entrance finish.
    await page.waitForFunction(id => {
      const element = document.getElementById(id);
      if (!element.getAnimations({ subtree: true }).some(animation => animation.playState === 'running')) return false;
      document.querySelector('[data-cinematic-pause]').click();
      return true;
    }, id);
    const readable = await page.locator('#' + id).evaluate(element => [...element.querySelectorAll('.motion-word')].every(word => {
      const style = getComputedStyle(word);
      return Number(style.opacity) > .9 && (style.transform === 'none' || Math.abs(new DOMMatrix(style.transform).m42) < .5);
    }));
    assert(readable, 'Pause must finish any clipped entrance and expose every word');
    await page.locator('[data-cinematic-pause]').click();
    await sleep(100);
    assert.equal(await page.locator('#' + id).evaluate(element => element.getAnimations({ subtree: true }).some(animation => animation.playState === 'running')), false, 'Resume must not replay completed entrances');
    await page.locator('[data-cinematic-pause]').click();
    await page.locator('[data-cinematic-pause]').click();
    assert.equal(await page.locator('#' + id).evaluate(element => element.getAnimations({ subtree: true }).some(animation => animation.playState === 'running')), false, 'Repeated pause/resume must preserve completed text');
  });
  await normal.close();

  await check('Early opening pause exposes the complete title without clipped lines', async () => {
    const s = await session();
    try {
      await s.context.addInitScript(() => {
        const observer = new MutationObserver(() => {
          const button = document.querySelector('[data-cinematic-pause]');
          if (!button || document.body?.dataset.cinematicMotion !== 'running') return;
          observer.disconnect();
          button.click();
          window.__earlyTitlePause = true;
        });
        observer.observe(document, { subtree: true, childList: true, attributes: true, attributeFilter: ['data-cinematic-motion'] });
      });
      await s.page.goto(base, { waitUntil: 'networkidle' });
      assert.equal(await s.page.evaluate(() => window.__earlyTitlePause), true, 'Pause must be exercised as soon as its controller initializes');
      assert.equal(await s.page.locator('body').getAttribute('data-cinematic-motion'), 'paused');
      assert(await s.page.locator('#hero-title').isVisible());
      const titleState = await s.page.locator('#hero-title').evaluate(element => [...element.querySelectorAll('.hero-word,.hero-second')].map(line => {
        const style = getComputedStyle(line);
        return { clip: style.clipPath, transformY: style.transform === 'none' ? 0 : new DOMMatrix(style.transform).m42, text: line.textContent.trim() };
      }));
      assert.equal(titleState.length, 2);
      assert(titleState.every(line => line.clip === 'none' && Math.abs(line.transformY) < .5 && line.text.length), 'Every opening line must be exposed when paused early');
      await shot(s.page, 'early-title-pause');
    } finally { await s.close(); }
  });

  await check('Desktop and mobile layouts keep chapters and captions clear of controls', async () => {
    for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }, { width: 320, height: 740 }]) {
      // Fresh contexts exercise the renderer's actual mobile initialization
      // branch, rather than merely resizing an already-created desktop scene.
      const s = await session({ viewport, isMobile: viewport.width < 760, hasTouch: viewport.width < 760 });
      const mobilePage = s.page;
      try {
        await mobilePage.goto(base, { waitUntil: 'networkidle' });
        await ready(mobilePage, hero);
        for (const id of ['main', 'ruang', 'gambut', 'peta', 'pulih', 'wynn']) {
          await mobilePage.locator('#' + id).scrollIntoViewIfNeeded();
          await sleep(150);
          const overflow = await mobilePage.evaluate(() => document.documentElement.scrollWidth - innerWidth);
          assert(overflow <= 1, `${viewport.width}px #${id}: ${overflow}px horizontal overflow`);
        }
        await mobilePage.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
        await sleep(300);
        await shot(mobilePage, viewport.width + '-hero');
        await mobilePage.locator('.weather-dock summary').click();
        assert(await mobilePage.locator('.weather-panel').isVisible());
        assert.equal(await mobilePage.locator('button[data-weather-choice]').count(), 4);
        const dockBounds = await mobilePage.locator('.weather-panel').boundingBox();
        assert(dockBounds.x >= 0 && dockBounds.x + dockBounds.width <= viewport.width + 1, 'Expanded weather panel must stay within the viewport');
        await shot(mobilePage, viewport.width + '-weather-expanded');
        await mobilePage.locator('.weather-dock summary').click();
        await gotoForest(mobilePage);
        await sleep(400);
        for (const subject of ['.forest-caption','.forest-navigation']) {
          const a=await mobilePage.locator(subject).boundingBox();
          assert(a,subject+' must remain visible');
          for(const overlay of ['.weather-dock>summary','[data-cinematic-pause]','[data-sound-toggle]','.forest-state-controls']) {
            const b=await mobilePage.locator(overlay).boundingBox();
            assert(b,overlay+' must remain visible');
            assert(!(a.x<b.x+b.width&&a.x+a.width>b.x&&a.y<b.y+b.height&&a.y+a.height>b.y),viewport.width+'px '+subject+' is obscured by '+overlay);
          }
        }
        await shot(mobilePage, viewport.width + '-forest');
      } finally { await s.close(); }
    }
  });

  await check('Reduced motion starts paused and keeps scenes static', async () => {
    const s = await session({ viewport: { width: 390, height: 844 }, reducedMotion: 'reduce' });
    try {
      await s.page.goto(base, { waitUntil: 'networkidle' });
      await ready(s.page, hero);
      assert.equal(await s.page.locator('body').getAttribute('data-cinematic-motion'), 'paused');
      assert.equal(await s.page.locator('[data-cinematic-pause]').getAttribute('aria-pressed'), 'true');
      assert.equal(await s.page.locator('[data-cinematic-pause]').isDisabled(), true);
      await stableCanvas(s.page, hero);
      await gotoForest(s.page);
      assert.equal(await s.page.locator(forest).getAttribute('data-motion'), 'paused');
      await stableCanvas(s.page, forest);
      await s.page.locator('button[data-forest-stage="2"]').click();
      assert.equal(await s.page.locator('button[data-forest-stage="2"]').getAttribute('aria-pressed'), 'true');
      await shot(s.page, 'reduced-forest');
    } finally { await s.close(); }
  });
  await check('No JavaScript retains readable story, chapters and sources', async () => {
    const s = await session({ viewport: { width: 390, height: 844 }, javaScriptEnabled: false });
    try {
      await s.page.goto(base, { waitUntil: 'load' });
      assert(await s.page.locator('h1').isVisible());
      assert(await s.page.locator('.landscape-fallback').isVisible());
      for (const id of ['ruang', 'gambut', 'peta', 'dampak', 'pulih', 'wynn']) {
        const section = s.page.locator('#' + id);
        await section.scrollIntoViewIfNeeded();
        assert(await section.locator('h2').first().isVisible(), id + ' title must not require script');
      }
      assert(await s.page.locator('a[href="/sumber/"]').count() > 0);
      await s.page.goto(base + '/sumber/', { waitUntil: 'load' });
      assert(await s.page.locator('h1').isVisible());
      assert(await s.page.locator('a[href^="https://"]').count() > 5);
    } finally { await s.close(); }
  });
  await check('Unavailable WebGL retains both illustrated fallbacks', async () => {
    const s = await session({}, [/Error creating WebGL context/, /THREE.WebGLRenderer/]);
    try {
      await s.context.addInitScript(() => {
        const original = HTMLCanvasElement.prototype.getContext;
        HTMLCanvasElement.prototype.getContext = function (kind, ...args) {
          return /webgl|experimental-webgl/.test(kind) ? null : original.call(this, kind, ...args);
        };
      });
      await s.page.goto(base, { waitUntil: 'networkidle' });
      await s.page.waitForFunction(selector => document.querySelector(selector)?.dataset.ready === 'false', hero);
      assert(await s.page.locator('.landscape-fallback').isVisible());
      await s.page.locator('#ruang').scrollIntoViewIfNeeded();
      await s.page.waitForFunction(selector => document.querySelector(selector)?.dataset.ready === 'false', forest);
      assert(await s.page.locator('.forest-fallback').isVisible());
      assert(await s.page.locator('#forest-title').isVisible());
      await shot(s.page, 'no-webgl');
    } finally { await s.close(); }
  });
  await check('Missing photographic textures retain the complete forest', async () => {
    const s = await session({}, [/Failed to load resource.*ERR_FAILED/]);
    try {
      let blocked = 0;
      await s.context.route('**/textures/**', route => { blocked++; return route.abort('failed'); });
      await s.page.goto(base, { waitUntil: 'networkidle' });
      await gotoForest(s.page);
      await sleep(500);
      assert(blocked >= 2, 'Both local texture requests must have been exercised');
      assert.equal(await s.page.locator(forest).getAttribute('data-ready'), 'true');
      await s.page.locator('button[data-forest-stage="2"]').click();
      assert.equal(await s.page.locator('button[data-forest-stage="2"]').getAttribute('aria-pressed'), 'true');
      await shot(s.page, 'missing-textures');
    } finally { await s.close(); }
  });
  await check('Direct map anchor, source route and 404 remain usable', async () => {
    const s = await session({}, [/HTTP 404/, /Failed to load resource.*404/, /\/missing-cinematic-route\//]);
    try {
      await s.page.goto(base + '/#peta', { waitUntil: 'networkidle' });
      await sleep(400);
      assert(await s.page.locator('#data-title').isVisible());
      const y = await s.page.locator('#peta').evaluate(element => element.getBoundingClientRect().top);
      assert(y < 250 && y > -300, 'Direct-entry anchor should be near the viewport top');
      await s.page.goto(base + '/sumber/', { waitUntil: 'networkidle' });
      assert(await s.page.locator('h1').isVisible());
      assert(await s.page.locator('a[href^="https://"]').count() > 5);
      const response = await s.page.goto(base + '/missing-cinematic-route/', { waitUntil: 'networkidle' });
      assert.equal(response.status(), 404);
      assert.match(await s.page.locator('h1').textContent(), /Jejak ini berakhir/);
      await s.page.getByRole('link', { name: 'Kembali ke dokumenter', exact: true }).click();
      assert(await s.page.locator('#hero-title').isVisible());
    } finally { await s.close(); }
  });
}

(async () => {
  let failed = false;
  try { await run(); } catch (error) { failed = true; console.error(error); report.failure = error.message; }
  finally {
    if (browser) await browser.close().catch(() => {});
    if (server) { server.closeAllConnections?.(); await new Promise(resolve => server.close(resolve)); }
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, 'cinematic-qa.json'), JSON.stringify(report, null, 2));
    console.log(JSON.stringify({ passed: report.checks.filter(check => check.pass).length, failed: report.checks.filter(check => !check.pass).length, errors: report.errors.length, report: 'test-results/cinematic-qa.json' }));
    if (failed) process.exitCode = 1;
  }
})();
