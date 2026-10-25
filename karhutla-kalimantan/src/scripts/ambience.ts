/** Original synthetic fire-and-wind ambience. No audio work starts before the sound gesture. */
const soundControl = document.querySelector<HTMLButtonElement>('[data-sound-toggle]');

if (soundControl) {
  const control = soundControl;
  const label = control.querySelector<HTMLElement>('[data-sound-label]');
  const AudioEngine = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  let context: AudioContext | undefined;
  let master: GainNode | undefined;
  let crackleBuffer: AudioBuffer | undefined;
  let crackleTimer: ReturnType<typeof setTimeout> | undefined;
  let suspendTimer: ReturnType<typeof setTimeout> | undefined;
  let wanted = false;
  let revision = 0;
  let disposed = false;
  const sources = new Set<AudioBufferSourceNode>();
  const nodes = new Set<AudioNode>();
  const shouldPlay = () => wanted && !document.hidden && !disposed;

  const updateControl = () => {
    control.setAttribute('aria-pressed', String(wanted));
    control.setAttribute('aria-label', wanted ? 'Matikan suara suasana' : 'Aktifkan suara suasana');
    if (label) label.textContent = wanted ? 'Suara aktif' : 'Suara mati';
    control.dataset.audioState = !wanted ? 'off' : document.hidden ? 'suspended' : context?.state === 'running' ? 'playing' : 'starting';
  };
  const clearTimers = () => {
    if (crackleTimer) clearTimeout(crackleTimer);
    if (suspendTimer) clearTimeout(suspendTimer);
    crackleTimer = undefined;
    suspendTimer = undefined;
  };
  const track = <T extends AudioNode>(node: T): T => { nodes.add(node); return node; };
  const failQuietly = () => {
    wanted = false;
    clearTimers();
    updateControl();
    control.title = 'Suara tidak tersedia di browser ini.';
  };
  const createGraph = () => {
    if (context || !AudioEngine) return;
    context = new AudioEngine();
    master = track(context.createGain());
    master.gain.value = 0;
    // One gentle limiter catches overlapping crackles; no sudden loud transients.
    const limiter = track(context.createDynamicsCompressor());
    limiter.threshold.value = -23;
    limiter.knee.value = 18;
    limiter.ratio.value = 4;
    limiter.attack.value = 0.008;
    limiter.release.value = 0.22;
    master.connect(limiter); limiter.connect(context.destination);

    const noise = context.createBuffer(1, Math.ceil(context.sampleRate * 5), context.sampleRate);
    const samples = noise.getChannelData(0);
    let brown = 0;
    for (let i = 0; i < samples.length; i++) {
      brown = (brown + (Math.random() * 2 - 1) * 0.026) / 1.026;
      samples[i] = brown * 3.2;
    }
    const wind = track(context.createBufferSource()); sources.add(wind);
    wind.buffer = noise; wind.loop = true;
    const high = track(context.createBiquadFilter()); high.type = 'highpass'; high.frequency.value = 85;
    const low = track(context.createBiquadFilter()); low.type = 'lowpass'; low.frequency.value = 720; low.Q.value = 0.3;
    const windGain = track(context.createGain()); windGain.gain.value = 0.2;
    wind.connect(high); high.connect(low); low.connect(windGain); windGain.connect(master);
    const air = track(context.createBiquadFilter()); air.type = 'bandpass'; air.frequency.value = 1100; air.Q.value = 0.4;
    const airGain = track(context.createGain()); airGain.gain.value = 0.032;
    high.connect(air); air.connect(airGain); airGain.connect(master);
    // A slow gain envelope gives wind depth without a repeating prerecorded sound.
    const gust = track(context.createOscillator()); gust.type = 'sine'; gust.frequency.value = 0.085;
    const gustDepth = track(context.createGain()); gustDepth.gain.value = 0.048;
    gust.connect(gustDepth); gustDepth.connect(windGain.gain);
    wind.start(); gust.start();

    crackleBuffer = context.createBuffer(1, Math.ceil(context.sampleRate * 0.18), context.sampleRate);
    const crackle = crackleBuffer.getChannelData(0);
    let last = 0;
    for (let i = 0; i < crackle.length; i++) {
      last = last * 0.42 + (Math.random() * 2 - 1) * 0.58;
      const t = i / crackle.length;
      crackle[i] = last * Math.exp(-t * 8) * Math.min(1, t * 95);
    }
  };
  const scheduleCrackle = () => {
    if (!shouldPlay() || !context || context.state !== 'running' || !master || !crackleBuffer) return;
    const at = context.currentTime + 0.015;
    const pop = context.createBufferSource(); sources.add(pop);
    const filter = context.createBiquadFilter(); filter.type = 'lowpass'; filter.frequency.value = 1450 + Math.random() * 950; filter.Q.value = 0.25;
    const envelope = context.createGain(); envelope.gain.setValueAtTime(0, at);
    envelope.gain.linearRampToValueAtTime(0.055 + Math.random() * 0.035, at + 0.006);
    envelope.gain.exponentialRampToValueAtTime(0.0001, at + 0.19);
    pop.buffer = crackleBuffer; pop.playbackRate.value = 0.72 + Math.random() * 0.6;
    const pan = context.createStereoPanner(); pan.pan.value = (Math.random() - 0.5) * 0.85;
    pop.connect(filter); filter.connect(envelope); envelope.connect(pan); pan.connect(master);
    pop.onended = () => { sources.delete(pop); pop.disconnect(); filter.disconnect(); envelope.disconnect(); pan.disconnect(); };
    pop.start(at); pop.stop(at + 0.25);
    crackleTimer = setTimeout(scheduleCrackle, 420 + Math.random() * 1050);
  };
  const sync = async () => {
    const current = ++revision;
    clearTimers();
    if (!shouldPlay()) {
      if (context && master) {
        const now = context.currentTime;
        master.gain.cancelScheduledValues(now);
        master.gain.setTargetAtTime(0, now, 0.035);
        if (document.hidden) void context.suspend().catch(() => {});
        else suspendTimer = setTimeout(() => { if (!shouldPlay()) void context?.suspend().catch(() => {}); }, 160);
      }
      updateControl(); return;
    }
    try {
      createGraph();
      if (!context || !master) { failQuietly(); return; }
      await context.resume();
      if (current !== revision || !shouldPlay()) {
        if (!shouldPlay()) void context.suspend().catch(() => {});
        return;
      }
      const now = context.currentTime;
      master.gain.cancelScheduledValues(now);
      master.gain.setValueAtTime(0, now);
      master.gain.linearRampToValueAtTime(0.34, now + 0.65);
      updateControl(); scheduleCrackle();
    } catch { failQuietly(); }
  };
  if (!AudioEngine) {
    control.disabled = true;
    control.title = 'Suara tidak tersedia di browser ini.';
  }
  control.addEventListener('click', () => {
    if (disposed || !AudioEngine) return;
    wanted = !wanted;
    control.title = '';
    updateControl();
    void sync();
  });
  document.addEventListener('visibilitychange', () => { void sync(); });
  window.addEventListener('pagehide', (event) => {
    clearTimers();
    if (event.persisted) { void context?.suspend().catch(() => {}); return; }
    disposed = true; wanted = false;
    sources.forEach(source => { try { source.stop(); } catch {} });
    nodes.forEach(node => node.disconnect());
    void context?.close().catch(() => {});
  });
  window.addEventListener('pageshow', (event) => { if (event.persisted) void sync(); });
  updateControl();
}
