/** Light Cone inspired presentation. The supplied illustration remains unmodified.
 * Input is event-driven; sensors are attached only after the enable button gesture. */
const reducedPortraitMotion = matchMedia('(prefers-reduced-motion: reduce)');
const clampPortrait = (value: number, min = -1, max = 1) => Math.max(min, Math.min(max, value));
type OrientationPermissionAPI = typeof DeviceOrientationEvent & {
  requestPermission?: () => Promise<PermissionState>;
};

document.querySelectorAll<HTMLElement>('[data-portrait]').forEach((portrait) => {
  const region = portrait.closest('.creator-section') ?? document;
  const toggle = region.querySelector<HTMLButtonElement>('[data-enable-gyro]');
  const status = region.querySelector<HTMLElement>('[data-sensor-status]');
  const canUseOrientation = isSecureContext && typeof DeviceOrientationEvent !== 'undefined';
  let x = 0, y = 0, frame = 0, enabled = false, visible = true;
  let baseline: { beta: number; gamma: number } | null = null;
  let sensorReceived = false, pendingPermission = false;
  let sensorTimer: ReturnType<typeof setTimeout> | undefined;
  const say = (message: string) => { if (status) status.textContent = message; };

  const paint = () => {
    frame = 0;
    portrait.style.setProperty('--portrait-rx', `${(-y * 7).toFixed(2)}deg`);
    portrait.style.setProperty('--portrait-ry', `${(x * 9).toFixed(2)}deg`);
    portrait.style.setProperty('--portrait-lx', `${(50 + x * 35).toFixed(2)}%`);
    portrait.style.setProperty('--portrait-ly', `${(42 + y * 32).toFixed(2)}%`);
    portrait.style.setProperty('--portrait-shift', `${(x * 14).toFixed(2)}%`);
  };
  const setTilt = (nextX: number, nextY: number, active: boolean) => {
    x = clampPortrait(nextX); y = clampPortrait(nextY);
    portrait.dataset.portraitActive = String(active && !reducedPortraitMotion.matches);
    if (!frame) frame = requestAnimationFrame(paint);
  };
  const reset = () => setTilt(0, 0, false);
  const updateControl = () => {
    if (!toggle) return;
    toggle.disabled = pendingPermission || reducedPortraitMotion.matches || !canUseOrientation;
    toggle.setAttribute('aria-pressed', String(enabled));
    toggle.textContent = enabled ? 'Nonaktifkan gerak perangkat' : 'Aktifkan gerak perangkat';
    toggle.title = reducedPortraitMotion.matches ? 'Gerak dinonaktifkan mengikuti pengaturan pengurangan animasi.' : !canUseOrientation ? 'Sensor gerak tidak tersedia di browser ini.' : '';
  };
  const stopSensor = () => {
    enabled = false; baseline = null;
    window.removeEventListener('deviceorientation', orient);
    if (sensorTimer) clearTimeout(sensorTimer);
    sensorTimer = undefined;
    reset(); updateControl();
  };
  const shortestDelta = (value: number, initial: number) => ((value - initial + 540) % 360) - 180;
  function orient(event: DeviceOrientationEvent) {
    if (!enabled || !visible || document.hidden || reducedPortraitMotion.matches) return;
    if (event.beta === null || event.gamma === null || !Number.isFinite(event.beta) || !Number.isFinite(event.gamma)) return;
    if (!sensorReceived) {
      sensorReceived = true;
      if (sensorTimer) clearTimeout(sensorTimer);
      say('Gerak perangkat aktif. Miringkan perlahan untuk menggerakkan kartu.');
    }
    baseline ??= { beta: event.beta, gamma: event.gamma };
    setTilt(shortestDelta(event.gamma, baseline.gamma) / 22, shortestDelta(event.beta, baseline.beta) / 28, true);
  }

  portrait.addEventListener('pointermove', (event) => {
    if (reducedPortraitMotion.matches) return;
    const bounds = portrait.getBoundingClientRect();
    setTilt(((event.clientX - bounds.left) / bounds.width - 0.5) * 2, ((event.clientY - bounds.top) / bounds.height - 0.5) * 2, true);
  }, { passive: true });
  portrait.addEventListener('pointerleave', reset);
  portrait.addEventListener('pointercancel', reset);
  portrait.addEventListener('pointerup', (event) => { if (event.pointerType !== 'mouse') reset(); });
  portrait.addEventListener('blur', reset);
  portrait.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') { reset(); return; }
    if (reducedPortraitMotion.matches || !['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) return;
    event.preventDefault();
    setTilt(x + (event.key === 'ArrowRight' ? 0.25 : event.key === 'ArrowLeft' ? -0.25 : 0), y + (event.key === 'ArrowDown' ? 0.25 : event.key === 'ArrowUp' ? -0.25 : 0), true);
  });

  toggle?.addEventListener('click', async () => {
    if (enabled) { stopSensor(); say('Gerak perangkat dinonaktifkan.'); return; }
    if (pendingPermission || reducedPortraitMotion.matches || !canUseOrientation) return;
    pendingPermission = true; updateControl();
    try {
      const api = DeviceOrientationEvent as OrientationPermissionAPI;
      if (typeof api.requestPermission === 'function') {
        const permission = await api.requestPermission();
        if (permission !== 'granted') { say('Izin sensor tidak diberikan. Kartu tetap dapat digerakkan dengan sentuhan atau kursor.'); return; }
      }
      // Recheck after the asynchronous permission prompt in case preferences changed.
      if (reducedPortraitMotion.matches) return;
      enabled = true; sensorReceived = false; baseline = null;
      window.addEventListener('deviceorientation', orient, { passive: true });
      say('Menunggu data gerak perangkat…');
      sensorTimer = setTimeout(() => {
        if (!sensorReceived) {
          stopSensor();
          say('Data sensor belum tersedia di browser ini. Gunakan sentuhan atau kursor pada kartu.');
        }
      }, 5000);
    } catch {
      stopSensor();
      say('Sensor tidak dapat diaktifkan di browser ini. Gunakan sentuhan atau kursor pada kartu.');
    } finally {
      pendingPermission = false; updateControl();
    }
  });

  const syncPreference = () => {
    if (reducedPortraitMotion.matches) {
      const wasEnabled = enabled;
      stopSensor(); say(wasEnabled ? 'Gerak kartu dinonaktifkan mengikuti pengaturan pengurangan animasi.' : '');
    } else {
      reset(); say('');
    }
    updateControl();
  };
  reducedPortraitMotion.addEventListener('change', syncPreference);
  document.addEventListener('visibilitychange', () => { if (document.hidden) reset(); });
  new IntersectionObserver(([entry]) => {
    visible = !!entry?.isIntersecting;
    if (!visible) { baseline = null; reset(); }
  }, { threshold: 0 }).observe(portrait);
  syncPreference();
});
