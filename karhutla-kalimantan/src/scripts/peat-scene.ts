import * as THREE from 'three';

/** An illustrative cutaway, not a measurement, forecast, or reconstruction. */
export function initPeatScene(container: HTMLElement) {
  const scene = new THREE.Scene();
  const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
  renderer.setClearColor(0x000000, 0);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.5;
  renderer.domElement.setAttribute('aria-hidden', 'true');
  renderer.domElement.style.cssText = 'display:block;width:100%;height:100%;pointer-events:none';
  container.appendChild(renderer.domElement);

  const camera = new THREE.OrthographicCamera(-5, 5, 4, -4, 0.1, 60);
  camera.position.set(7.5, 6.4, 9.2);
  camera.lookAt(0, 0.4, 0);
  scene.add(new THREE.HemisphereLight(0xf1f9f1, 0x51402f, 2.8));
  const sun = new THREE.DirectionalLight(0xfff2d4, 3.6);
  sun.position.set(-4, 9, 6);
  scene.add(sun);
  const rim = new THREE.DirectionalLight(0xb3e2ed, 1.7);
  rim.position.set(5, 4, -6);
  scene.add(rim);

  const world = new THREE.Group();
  scene.add(world);
  const materials: THREE.Material[] = [];
  const geometries: THREE.BufferGeometry[] = [];
  const standard = (color: number, extra: THREE.MeshStandardMaterialParameters = {}) => {
    const material = new THREE.MeshStandardMaterial({ color, roughness: 0.94, ...extra });
    materials.push(material);
    return material;
  };
  const mesh = (geometry: THREE.BufferGeometry, material: THREE.Material, parent: THREE.Object3D = world) => {
    geometries.push(geometry);
    const object = new THREE.Mesh(geometry, material);
    parent.add(object);
    return object;
  };

  // Slightly irregular strata make the accumulated organic material legible.
  const layerColors = [0x272b23, 0x403529, 0x51402e, 0x665038, 0x7a6143];
  const boundaries = [-1.62, -1.21, -0.77, -0.3, 0.15, 0.6];
  const contour = (x: number, level: number) => Math.sin(x * 1.8 + level * 2) * 0.032 + Math.cos(x * 3.1 - level) * 0.018;
  for (let layer = 0; layer < 5; layer++) {
    const shape = new THREE.Shape();
    const low = boundaries[layer]!;
    const high = boundaries[layer + 1]!;
    shape.moveTo(-2.8, low + contour(-2.8, layer));
    for (let i = 0; i <= 24; i++) {
      const x = -2.8 + (5.6 * i) / 24;
      shape.lineTo(x, low + contour(x, layer));
    }
    for (let i = 24; i >= 0; i--) {
      const x = -2.8 + (5.6 * i) / 24;
      shape.lineTo(x, high + contour(x, layer + 1));
    }
    shape.closePath();
    const earth = mesh(new THREE.ExtrudeGeometry(shape, { depth: 3.7, bevelEnabled: false, steps: 1 }), standard(layerColors[layer]!));
    earth.position.z = -1.85;
  }

  const surfaceMaterial = standard(0x566a3f);
  const surfaceLeft = mesh(new THREE.BoxGeometry(5.6, 0.2, 1.59), surfaceMaterial);
  surfaceLeft.position.set(0, 0.68, -1.055);
  const surfaceRight = mesh(new THREE.BoxGeometry(5.6, 0.2, 1.59), surfaceMaterial);
  surfaceRight.position.set(0, 0.68, 1.055);
  const canalFill = mesh(new THREE.BoxGeometry(5.6, 0.2, 0.52), surfaceMaterial);
  canalFill.position.set(0, 0.68, 0);

  const waterMaterial = standard(0x6da8af, { transparent: true, opacity: 0.8, roughness: 0.18, metalness: 0.1 });
  const canalWater = mesh(new THREE.PlaneGeometry(5.58, 0.49), waterMaterial);
  canalWater.rotation.x = -Math.PI / 2;
  canalWater.position.y = 0.74;
  const saturatedMaterial = standard(0x7aa9ac, { transparent: true, opacity: 0.28, roughness: 0.5, depthWrite: false });
  const saturatedFront = mesh(new THREE.PlaneGeometry(5.59, 1), saturatedMaterial);
  saturatedFront.position.z = 1.875;
  const saturatedSide = mesh(new THREE.PlaneGeometry(3.69, 1), saturatedMaterial);
  saturatedSide.rotation.y = Math.PI / 2;
  saturatedSide.position.x = 2.815;
  const waterlineMaterial = standard(0xa5d3d0, { emissive: 0x36666c, emissiveIntensity: 0.2 });
  const frontLine = mesh(new THREE.BoxGeometry(5.6, 0.025, 0.015), waterlineMaterial);
  frontLine.position.z = 1.89;
  const sideLine = mesh(new THREE.BoxGeometry(0.015, 0.025, 3.7), waterlineMaterial);
  sideLine.position.x = 2.83;

  // Visible roots/fibres on the cut face are schematic, not sampled data.
  const fibreMaterial = standard(0xbaa881);
  for (let i = 0; i < 24; i++) {
    const x = -2.58 + ((i * 1.73) % 5.12);
    const y = -1.41 + ((i * 0.37) % 1.8);
    const fibre = mesh(new THREE.CylinderGeometry(0.008, 0.004, 0.15 + (i % 4) * 0.055, 3), fibreMaterial);
    fibre.position.set(x, y, 1.883);
    fibre.rotation.z = Math.sin(i * 5) * 1.3;
  }

  const trunkMaterial = standard(0x5f503c);
  const crownMaterials = [standard(0x385238, { flatShading: true }), standard(0x4d6945, { flatShading: true }), standard(0x638451, { flatShading: true })];
  const crowns: THREE.Group[] = [];
  const placements = [
    [-2.08, -1.21, 1.14], [-1.14, -1.1, 1.48], [0.07, -1.29, 1.05],
    [1.31, -1.18, 1.54], [2.19, -0.88, 0.93], [-2.13, 0.92, 0.88],
    [-1.11, 1.18, 1.03], [0.16, 0.98, 0.83], [1.37, 1.16, 1.01],
    [2.24, 0.81, 0.75],
  ];
  placements.forEach(([x = 0, z = 0, height = 1], index) => {
    const tree = new THREE.Group();
    tree.position.set(x, 0.78, z);
    world.add(tree);
    const trunk = mesh(new THREE.CylinderGeometry(0.035, 0.075, height, 5), trunkMaterial, tree);
    trunk.position.y = height / 2;
    for (let root = 0; root < 3; root++) {
      const angle = (root * Math.PI * 2) / 3 + index;
      const buttress = mesh(new THREE.ConeGeometry(0.12, 0.38, 3), trunkMaterial, tree);
      buttress.position.set(Math.cos(angle) * 0.055, 0.17, Math.sin(angle) * 0.055);
      buttress.rotation.y = angle;
    }
    const crown = new THREE.Group();
    crown.position.y = height * 0.92;
    tree.add(crown);
    for (let branch = 0; branch < 4; branch++) {
      const leaf = mesh(new THREE.IcosahedronGeometry(0.33 + height * 0.08, 1), crownMaterials[(index + branch) % 3]!, crown);
      const angle = branch * 2.4 + index;
      leaf.position.set(Math.cos(angle) * 0.19, branch === 3 ? 0.24 : 0, Math.sin(angle) * 0.16);
      leaf.scale.set(1.1, branch === 3 ? 0.9 : 0.76, 1);
      leaf.rotation.set(index, branch * 0.7, index + branch);
    }
    crowns.push(crown);
  });

  const ashMaterial = standard(0x534b3d, { flatShading: true });
  const mossMaterial = standard(0x73885c, { flatShading: true });
  for (let i = 0; i < 15; i++) {
    const stone = mesh(new THREE.IcosahedronGeometry(0.05 + (i % 3) * 0.016, 0), i % 3 ? mossMaterial : ashMaterial);
    stone.position.set(-2.6 + ((i * 1.13) % 5.2), 0.81, (i % 2 ? -1 : 1) * (0.49 + ((i * 0.29) % 1.15)));
    stone.scale.set(1.4, 0.4, 1);
  }

  const smoulder = new THREE.Group();
  world.add(smoulder);
  const charMaterial = standard(0x4d291b, { emissive: 0x4d1607, emissiveIntensity: 0.65, flatShading: true });
  const emberMaterial = standard(0xdc6a24, { emissive: 0xe35212, emissiveIntensity: 1.8, roughness: 1 });
  const warmMaterial = standard(0xb74c1c, { emissive: 0x6f1c06, emissiveIntensity: 0.8, transparent: true, opacity: 0.9 });
  for (let i = 0; i < 16; i++) {
    const coal = mesh(new THREE.IcosahedronGeometry(0.11 + (i % 3) * 0.04, 0), i % 4 === 0 ? emberMaterial : charMaterial, smoulder);
    coal.position.set(0.27 + ((i * 0.31) % 1.95), 0.2 - ((i * 0.17) % 0.75), 1.89);
    coal.scale.set(1.75, 0.63, 0.2);
    coal.rotation.z = i * 0.6;
  }
  for (let i = 0; i < 9; i++) {
    const warm = mesh(new THREE.IcosahedronGeometry(0.026, 0), warmMaterial, smoulder);
    warm.position.set(0.24 + ((i * 0.53) % 2.2), 0.28 - ((i * 0.22) % 0.9), 1.95);
    warm.scale.set(2.7, 0.7, 0.3);
  }

  const smoke = new THREE.Group();
  world.add(smoke);
  const smokeMaterial = standard(0x9d9c8e, { transparent: true, opacity: 0.2, depthWrite: false, flatShading: true });
  for (let i = 0; i < 5; i++) {
    const puff = mesh(new THREE.IcosahedronGeometry(0.2 + i * 0.055, 1), smokeMaterial, smoke);
    puff.position.set(1.2 - i * 0.07, 0.96 + i * 0.39, 0.95 - i * 0.08);
    puff.scale.set(1.4, 0.9, 1);
  }

  const restoration = new THREE.Group();
  world.add(restoration);
  const damMaterial = standard(0x8c7856);
  const dam = mesh(new THREE.BoxGeometry(0.18, 0.26, 0.64), damMaterial, restoration);
  dam.position.set(1.7, 0.75, 0);
  const seedlingMaterial = standard(0x7d9a54, { flatShading: true });
  for (let i = 0; i < 7; i++) {
    const sapling = mesh(new THREE.ConeGeometry(0.09, 0.32, 5), seedlingMaterial, restoration);
    sapling.position.set(-2.5 + i * 0.72, 0.94, 1.55 - (i % 2) * 0.3);
  }

  const stageParameters = [
    { water: 0.52, earth: 0x566a3f, crown: [0x385238, 0x4d6945, 0x638451], foliage: 1, fire: 0, recovery: 0, fill: 1 },
    { water: -0.83, earth: 0x8d7950, crown: [0x626543, 0x77794c, 0x878951], foliage: 0.91, fire: 0, recovery: 0, fill: 0 },
    { water: -1.11, earth: 0x514739, crown: [0x4d4a38, 0x62563b, 0x746341], foliage: 0.55, fire: 1, recovery: 0, fill: 0 },
    { water: 0.31, earth: 0x6e7f4b, crown: [0x405c38, 0x547546, 0x6a8a51], foliage: 0.72, fire: 0, recovery: 1, fill: 0 },
  ];
  const stateFor = (stage: number) => {
    const state = stageParameters[stage]!;
    return { ...state, earth: new THREE.Color(state.earth), crown: state.crown.map((color) => new THREE.Color(color)) };
  };
  let current = stateFor(0);
  let target = 0;
  let frame = 0;
  let disposed = false;
  let visible = true;
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');

  function drawStage() {
    const value = (key: 'water' | 'foliage' | 'fire' | 'recovery' | 'fill') => current[key];
    surfaceMaterial.color.copy(current.earth);
    crownMaterials.forEach((material, index) => material.color.copy(current.crown[index]!));
    const water = value('water');
    const saturatedHeight = Math.max(0.03, water + 1.61);
    saturatedFront.scale.y = saturatedHeight;
    saturatedFront.position.y = -1.61 + saturatedHeight / 2;
    saturatedSide.scale.y = saturatedHeight;
    saturatedSide.position.y = saturatedFront.position.y;
    frontLine.position.y = water;
    sideLine.position.y = water;
    // Canal surface is a schematic inset; cross-section indicates relative saturation.
    canalWater.position.y = THREE.MathUtils.lerp(0.61, 0.755, (water + 1.11) / 1.63);
    canalFill.visible = value('fill') > 0.5;
    canalWater.visible = !canalFill.visible;
    crowns.forEach((crown, index) => crown.scale.setScalar(value('foliage') * (index > 6 && value('fire') > 0.1 ? 0.9 : 1)));
    const fire = value('fire');
    smoulder.visible = fire > 0.015;
    smoulder.scale.setScalar(Math.max(0.001, fire));
    smoke.visible = fire > 0.015;
    smokeMaterial.opacity = fire * 0.22;
    restoration.visible = value('recovery') > 0.015;
    restoration.scale.y = Math.max(0.001, value('recovery'));
    if (visible && !disposed) renderer.render(scene, camera);
  }

  function setStage(stage: number) {
    if (!Number.isFinite(stage) || disposed) return;
    target = THREE.MathUtils.clamp(Math.round(stage), 0, 3);
    cancelAnimationFrame(frame);
    if (reduced.matches || !visible) {
      current = stateFor(target);
      drawStage();
      return;
    }
    const from = current;
    const to = stateFor(target);
    const started = performance.now();
    const animate = (now: number) => {
      const progress = Math.min(1, (now - started) / 650);
      const eased = progress * progress * (3 - 2 * progress);
      current = {
        water: THREE.MathUtils.lerp(from.water, to.water, eased),
        foliage: THREE.MathUtils.lerp(from.foliage, to.foliage, eased),
        fire: THREE.MathUtils.lerp(from.fire, to.fire, eased),
        recovery: THREE.MathUtils.lerp(from.recovery, to.recovery, eased),
        fill: THREE.MathUtils.lerp(from.fill, to.fill, eased),
        earth: from.earth.clone().lerp(to.earth, eased),
        crown: from.crown.map((color, index) => color.clone().lerp(to.crown[index]!, eased)),
      };
      drawStage();
      if (progress < 1 && !disposed && visible) frame = requestAnimationFrame(animate);
      else current = to;
    };
    frame = requestAnimationFrame(animate);
  }

  function resize() {
    const width = Math.max(1, container.clientWidth);
    const height = Math.max(1, container.clientHeight);
    const aspect = width / height;
    const verticalSize = aspect < 1.1 ? 8.1 : 7.5;
    camera.left = (-verticalSize * aspect) / 2;
    camera.right = (verticalSize * aspect) / 2;
    camera.top = verticalSize / 2;
    camera.bottom = -verticalSize / 2;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height, false);
    drawStage();
  }
  const observer = new ResizeObserver(resize);
  observer.observe(container);
  const intersection = new IntersectionObserver(([entry]) => {
    visible = !!entry?.isIntersecting;
    if (!visible) {
      cancelAnimationFrame(frame);
      current = stateFor(target);
    } else drawStage();
  });
  intersection.observe(container);
  const onStage = (event: Event) => setStage((event as CustomEvent<{ stage: number }>).detail?.stage);
  const onReduced = () => {
    if (reduced.matches) {
      cancelAnimationFrame(frame);
      current = stateFor(target);
      drawStage();
    }
  };
  const onContextLost = (event: Event) => {
    event.preventDefault();
    container.dataset.ready = 'false';
    cancelAnimationFrame(frame);
  };
  container.addEventListener('peat-stage', onStage);
  reduced.addEventListener('change', onReduced);
  renderer.domElement.addEventListener('webglcontextlost', onContextLost);
  resize();
  // The fallback is removed only after a successful first WebGL draw.
  container.dataset.ready = 'true';

  return {
    setStage,
    dispose() {
      disposed = true;
      cancelAnimationFrame(frame);
      observer.disconnect();
      intersection.disconnect();
      container.removeEventListener('peat-stage', onStage);
      reduced.removeEventListener('change', onReduced);
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost);
      geometries.forEach((geometry) => geometry.dispose());
      materials.forEach((material) => material.dispose());
      renderer.dispose();
      renderer.domElement.remove();
      delete container.dataset.ready;
    },
  };
}
