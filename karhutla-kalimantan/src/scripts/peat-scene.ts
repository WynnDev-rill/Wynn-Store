import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';

/** Schematic cutaway. Textures and geometry are illustrative, not measurements. */
export function initPeatScene(container: HTMLElement) {
  const scene = new THREE.Scene();
  const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
  renderer.setClearColor(0x000000, 0);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.08;
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFShadowMap;
  renderer.shadowMap.autoUpdate = false;
  renderer.domElement.setAttribute('aria-hidden', 'true');
  renderer.domElement.style.cssText = 'display:block;width:100%;height:100%;pointer-events:none';
  container.appendChild(renderer.domElement);

  const camera = new THREE.OrthographicCamera(-5, 5, 4, -4, 0.1, 60);
  scene.add(new THREE.HemisphereLight(0xe9f1e7, 0x3f4438, 1.6));
  const sun = new THREE.DirectionalLight(0xffefd0, 3.25);
  sun.position.set(-4.5, 10, 6); sun.castShadow = true;
  sun.shadow.mapSize.setScalar(container.clientWidth < 560 ? 512 : 1024);
  Object.assign(sun.shadow.camera, { left: -5, right: 5, top: 5, bottom: -5, near: 0.5, far: 24 });
  sun.shadow.bias = -0.0005; sun.shadow.normalBias = 0.035; sun.shadow.radius = 3;
  scene.add(sun);
  const rim = new THREE.DirectionalLight(0xb0d4cc, 1.3);
  rim.position.set(4, 4, -6); scene.add(rim);

  const world = new THREE.Group(), surface = new THREE.Group(), hydrology = new THREE.Group();
  world.add(surface, hydrology); scene.add(world);
  const materials = new Set<THREE.Material>();
  const geometries = new Set<THREE.BufferGeometry>();
  const textures = new Set<THREE.Texture>();
  let seed = 17419;
  const random = () => { seed ^= seed << 13; seed ^= seed >>> 17; seed ^= seed << 5; return (seed >>> 0) / 4294967296; };
  const standard = (color: number, extra: THREE.MeshStandardMaterialParameters = {}) => {
    const material = new THREE.MeshStandardMaterial({ color, roughness: 0.96, ...extra }); materials.add(material); return material;
  };
  const mesh = (geometry: THREE.BufferGeometry, material: THREE.Material, parent: THREE.Object3D = world, shadow = true) => {
    geometries.add(geometry);
    const object = new THREE.Mesh(geometry, material);
    object.castShadow = shadow; object.receiveShadow = shadow; parent.add(object); return object;
  };
  const mergedMesh = (parts: THREE.BufferGeometry[], material: THREE.Material, parent: THREE.Object3D = surface) => {
    // Primitives have mixed indexed/non-indexed representations; normalize first.
    const normalized = parts.map((part) => part.index ? part.toNonIndexed() : part);
    const merged = mergeGeometries(normalized, false);
    if (!merged) throw new Error('Unable to construct peat illustration geometry');
    new Set([...parts, ...normalized]).forEach((part) => part.dispose()); return mesh(merged, material, parent);
  };
  const transform = (geometry: THREE.BufferGeometry, position: THREE.Vector3, scale = new THREE.Vector3(1, 1, 1), rotation = new THREE.Euler()) => {
    geometry.applyMatrix4(new THREE.Matrix4().compose(position, new THREE.Quaternion().setFromEuler(rotation), scale)); return geometry;
  };
  const cylinderBetween = (a: THREE.Vector3, b: THREE.Vector3, radius: number, endRadius = radius * 0.6, segments = 5) => {
    const direction = b.clone().sub(a);
    const geometry = new THREE.CylinderGeometry(endRadius, radius, direction.length(), segments);
    geometry.applyQuaternion(new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.clone().normalize()));
    geometry.translate(...a.clone().add(b).multiplyScalar(0.5).toArray()); return geometry;
  };

  // Small deterministic textures add organic fibres without remote assets.
  const soilCanvas = document.createElement('canvas'); soilCanvas.width = 512; soilCanvas.height = 256;
  const soilContext = soilCanvas.getContext('2d');
  if (!soilContext) throw new Error('Unable to create illustration texture');
  const pixels = soilContext.createImageData(512, 256);
  for (let y = 0; y < 256; y++) for (let x = 0; x < 512; x++) {
    const base = 160 + random() * 48 + Math.sin(y * 0.26 + Math.sin(x * 0.02) * 0.6) * 12;
    const i = (y * 512 + x) * 4;
    pixels.data[i] = base + 8; pixels.data[i + 1] = base + 3; pixels.data[i + 2] = base - 7; pixels.data[i + 3] = 255;
  }
  soilContext.putImageData(pixels, 0, 0);
  for (let i = 0; i < 290; i++) {
    const x = random() * 512, y = random() * 256, length = 3 + random() * 35;
    soilContext.strokeStyle = i % 3 ? 'rgba(72,57,37,.28)' : 'rgba(245,226,184,.38)';
    soilContext.lineWidth = 0.4 + random() * 1.2;
    soilContext.beginPath(); soilContext.moveTo(x, y);
    soilContext.bezierCurveTo(x + length * 0.3, y - 2, x + length * 0.8, y + 4, x + length, y + random() * 5); soilContext.stroke();
  }
  const soilTexture = new THREE.CanvasTexture(soilCanvas);
  soilTexture.colorSpace = THREE.SRGBColorSpace; soilTexture.wrapS = soilTexture.wrapT = THREE.RepeatWrapping;
  soilTexture.repeat.set(0.8, 0.7); soilTexture.anisotropy = Math.min(renderer.capabilities.getMaxAnisotropy(), 4); textures.add(soilTexture);
  const roughTexture = soilTexture.clone(); roughTexture.colorSpace = THREE.NoColorSpace; textures.add(roughTexture);

  const layers: THREE.Group[] = [];
  const layerColors = [0x35352a, 0x53432d, 0x6a4d33, 0x80613f, 0x8f754c];
  const boundaries = [-1.55, -1.16, -0.73, -0.27, 0.16, 0.59];
  const contour = (x: number, level: number) => Math.sin(x * 2.1 + level * 1.7) * 0.031 + Math.cos(x * 4.3 - level) * 0.014;
  const rootMaterial = standard(0xb9a779, { roughness: 1 });
  for (let layer = 0; layer < 5; layer++) {
    const group = new THREE.Group(); world.add(group); layers.push(group);
    const shape = new THREE.Shape();
    for (let i = 0; i <= 40; i++) {
      const x = -2.8 + (5.6 * i) / 40, y = boundaries[layer]! + contour(x, layer);
      if (!i) shape.moveTo(x, y); else shape.lineTo(x, y);
    }
    for (let i = 40; i >= 0; i--) { const x = -2.8 + (5.6 * i) / 40; shape.lineTo(x, boundaries[layer + 1]! + contour(x, layer + 1)); }
    shape.closePath();
    const earth = mesh(new THREE.ExtrudeGeometry(shape, { depth: 3.7, bevelEnabled: true, bevelSegments: 1, steps: 1, bevelSize: 0.012, bevelThickness: 0.012 }), standard(layerColors[layer]!, { map: soilTexture, bumpMap: roughTexture, bumpScale: 0.043 }), group);
    earth.position.z = -1.85;
    // Branching fibres stay attached to their stratum during the reveal.
    const roots: THREE.BufferGeometry[] = [];
    for (let i = 0; i < 11 - layer; i++) {
      const x = -2.6 + random() * 5.15, top = boundaries[layer + 1]! - 0.04;
      const mid = new THREE.Vector3(x + (random() - 0.5) * 0.22, top - 0.14, 1.885);
      const end = new THREE.Vector3(mid.x + (random() - 0.5) * 0.19, Math.max(boundaries[layer]! + 0.04, top - 0.32), 1.889);
      roots.push(cylinderBetween(new THREE.Vector3(x, top, 1.885), mid, 0.009, 0.005, 4));
      roots.push(cylinderBetween(mid, end, 0.005, 0.002, 3));
      roots.push(cylinderBetween(mid, new THREE.Vector3(mid.x + 0.07 + random() * 0.1, mid.y - 0.035, 1.889), 0.004, 0.001, 3));
    }
    mergedMesh(roots, rootMaterial, group);
  }

  const surfaceMaterial = standard(0x708652, { map: soilTexture, bumpMap: roughTexture, bumpScale: 0.065 });
  const left = mesh(new THREE.BoxGeometry(5.6, 0.22, 1.59), surfaceMaterial, surface);
  const right = mesh(new THREE.BoxGeometry(5.6, 0.22, 1.59), surfaceMaterial, surface);
  left.position.set(0, 0.69, -1.055); right.position.set(0, 0.69, 1.055);
  const canalFill = mesh(new THREE.BoxGeometry(5.6, 0.22, 0.52), surfaceMaterial, surface); canalFill.position.set(0, 0.69, 0);
  const canalBanks = standard(0x5b4930, { map: soilTexture, bumpMap: roughTexture, bumpScale: 0.025 });
  for (const z of [-0.266, 0.266]) { const bank = mesh(new THREE.BoxGeometry(5.58, 0.18, 0.025), canalBanks, surface); bank.position.set(0, 0.66, z); }
  const waterMaterial = standard(0x63918c, { transparent: true, opacity: 0.83, roughness: 0.2, metalness: 0.12 });
  const canalWater = mesh(new THREE.PlaneGeometry(5.57, 0.49), waterMaterial, surface, false); canalWater.rotation.x = -Math.PI / 2;
  const waterHighlights: THREE.BufferGeometry[] = [];
  for (let i = 0; i < 9; i++) waterHighlights.push(transform(new THREE.BoxGeometry(0.13 + random() * 0.24, 0.004, 0.008), new THREE.Vector3(-2.5 + i * 0.58, 0.004, (random() - 0.5) * 0.3)));
  const reflections = mergedMesh(waterHighlights, standard(0xa7c7b6, { transparent: true, opacity: 0.48 }), surface); reflections.castShadow = false;
  const saturatedMaterial = standard(0x73a7a0, { transparent: true, opacity: 0.19, roughness: 0.63, depthWrite: false });
  const saturatedFront = mesh(new THREE.PlaneGeometry(5.59, 1), saturatedMaterial, hydrology, false); saturatedFront.position.z = 1.89;
  const saturatedSide = mesh(new THREE.PlaneGeometry(3.69, 1), saturatedMaterial, hydrology, false); saturatedSide.rotation.y = Math.PI / 2; saturatedSide.position.x = 2.825;
  const waterlineMaterial = standard(0xa5d0c0, { emissive: 0x315448, emissiveIntensity: 0.15 });
  const frontLine = mesh(new THREE.BoxGeometry(5.6, 0.022, 0.013), waterlineMaterial, hydrology, false); frontLine.position.z = 1.907;
  const sideLine = mesh(new THREE.BoxGeometry(0.013, 0.022, 3.7), waterlineMaterial, hydrology, false); sideLine.position.x = 2.84;

  // Broadleaf silhouettes, tapered branching trunks, buttress roots, and emergents.
  // Merged geometry keeps the varied crowns inexpensive to draw.
  const trunkMaterial = standard(0x66563b, { map: soilTexture, bumpMap: roughTexture, bumpScale: 0.035 });
  const crownMaterial = standard(0x476641, { vertexColors: true, roughness: 0.88, side: THREE.DoubleSide });
  const crowns: THREE.Mesh[] = [], wood: THREE.BufferGeometry[] = [];
  const placements = [
    [-2.23, -1.32, 1.65, 0.75], [-1.47, -1.12, 2.02, 0.85], [-0.48, -1.35, 1.27, 0.72],
    [0.45, -1.11, 1.81, 0.88], [1.42, -1.29, 2.22, 0.86], [2.34, -1.1, 1.31, 0.63],
    [-1.93, -0.54, 1.01, 0.59], [-0.8, -0.56, 1.42, 0.68], [0.94, -0.54, 1.05, 0.55],
    [-2.25, 1.25, 1.05, 0.58], [-1.34, 0.92, 1.38, 0.67], [-0.33, 1.28, 1.06, 0.59],
    [0.55, 0.87, 1.22, 0.63], [1.51, 1.28, 1.04, 0.58], [2.33, 0.91, 0.86, 0.54],
  ];
  const base = 0.8;
  placements.forEach(([x = 0, z = 0, treeHeight = 1, radius = 0.6], index) => {
    const leanX = (random() - 0.5) * 0.17, leanZ = (random() - 0.5) * 0.14;
    const foot = new THREE.Vector3(x, base, z);
    const fork = new THREE.Vector3(x + leanX, base + treeHeight * 0.66, z + leanZ);
    const tip = new THREE.Vector3(x + leanX * 1.4, base + treeHeight, z + leanZ * 1.4);
    const trunkRadius = 0.038 + treeHeight * 0.018;
    wood.push(cylinderBetween(foot, fork, trunkRadius, trunkRadius * 0.66, 7)); wood.push(cylinderBetween(fork, tip, trunkRadius * 0.66, 0.012, 6));
    for (let r = 0; r < 4; r++) {
      const angle = r * Math.PI * 0.5 + index;
      wood.push(cylinderBetween(new THREE.Vector3(x + Math.cos(angle) * 0.2, base + 0.013, z + Math.sin(angle) * 0.17), new THREE.Vector3(x, base + 0.31, z), 0.018, 0.04, 3));
    }
    const leaves: THREE.BufferGeometry[] = [];
    for (let branch = 0; branch < 6; branch++) {
      const angle = branch * 2.39996 + index, reach = radius * (branch === 5 ? 0.12 : 0.46 + random() * 0.14);
      const endpoint = new THREE.Vector3(tip.x + Math.cos(angle) * reach, tip.y - 0.12 + random() * 0.22, tip.z + Math.sin(angle) * reach);
      wood.push(cylinderBetween(fork.clone().lerp(tip, 0.35), endpoint, 0.019, 0.006, 4));
      // Leaves, not a solid cluster: small curved blades preserve a broken organic
      // silhouette and let light pass through the crown without alpha-texture overdraw.
      const lobeRadius = radius * (0.42 + random() * 0.13);
      for (let leaf = 0; leaf < 72; leaf++) {
        const phi = random() * Math.PI * 2, cosine = random() * 2 - 1;
        const shell = Math.pow(random(), 0.28), sine = Math.sqrt(1 - cosine * cosine);
        const local = new THREE.Vector3(Math.cos(phi) * sine * lobeRadius * shell, cosine * lobeRadius * shell * 0.68, Math.sin(phi) * sine * lobeRadius * shell);
        const leafSize = 0.075 + random() * 0.066;
        const geometry = new THREE.BufferGeometry();
        // Six triangles meet a raised midrib; both faces are shaded, and a slight
        // longitudinal fold gives the leaf physical depth at grazing angles.
        const vertices = new Float32Array([
          0,0,-1, -.38,0,-.46, 0,.15,-.22,
          -.38,0,-.46, -.43,.015,.28, 0,.15,-.22,
          -.43,.015,.28, 0,.035,1, 0,.15,-.22,
          0,0,-1, 0,.15,-.22, .38,0,-.46,
          .38,0,-.46, 0,.15,-.22, .43,.015,.28,
          .43,.015,.28, 0,.15,-.22, 0,.035,1,
        ]);
        geometry.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
        geometry.computeVertexNormals();
        const colors = new Float32Array(18 * 3);
        const tint = 0.62 + random() * 0.5 + local.y * 0.24;
        for (let vertex = 0; vertex < 18; vertex++) {
          colors[vertex * 3] = tint * (0.91 + random() * 0.08);
          colors[vertex * 3 + 1] = tint;
          colors[vertex * 3 + 2] = tint * 0.75;
        }
        geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));
        geometry.scale(leafSize * (0.9 + random() * 0.35), leafSize, leafSize);
        geometry.rotateX((random() - 0.5) * 1.6); geometry.rotateY(random() * 6.28); geometry.rotateZ((random() - 0.5) * 1.1);
        geometry.translate(endpoint.x - tip.x + local.x, endpoint.y - tip.y + local.y, endpoint.z - tip.z + local.z);
        leaves.push(geometry);
      }
    }
    const crown = mergedMesh(leaves, crownMaterial, surface); crown.position.copy(tip); crowns.push(crown);
  });
  mergedMesh(wood, trunkMaterial);

  const groundcover: THREE.BufferGeometry[] = [], litter: THREE.BufferGeometry[] = [];
  for (let i = 0; i < 75; i++) {
    const x = -2.66 + random() * 5.32, z = (i % 2 ? -1 : 1) * (0.37 + random() * 1.35);
    if (i % 5 === 0) {
      const center = new THREE.Vector3(x, 0.81, z);
      for (let leaf = 0; leaf < 5; leaf++) {
        const angle = leaf * 1.256 + i, end = new THREE.Vector3(x + Math.cos(angle) * 0.14, 0.91 + random() * 0.07, z + Math.sin(angle) * 0.14);
        groundcover.push(cylinderBetween(center, end, 0.014, 0.003, 3));
      }
    } else litter.push(transform(new THREE.IcosahedronGeometry(0.025 + random() * 0.035, 0), new THREE.Vector3(x, 0.81, z), new THREE.Vector3(1.4, 0.17, 0.75), new THREE.Euler(0, random() * 6.28, 0)));
  }
  const understoryMaterial = standard(0x6a8042); mergedMesh(groundcover, understoryMaterial); mergedMesh(litter, standard(0x95865d));
  const fallen = mesh(new THREE.CylinderGeometry(0.055, 0.074, 0.76, 7), trunkMaterial, surface);
  fallen.position.set(-0.6, 0.86, 1.56); fallen.rotation.set(0.2, 0.7, 1.49);
  // A softly baked grounding shadow avoids a large stair-stepped cast silhouette
  // at mobile shadow-map resolutions. Tree/terrain shadows remain real PCF shadows.
  const contactCanvas = document.createElement('canvas'); contactCanvas.width = contactCanvas.height = 128;
  const contactContext = contactCanvas.getContext('2d');
  if (!contactContext) throw new Error('Unable to create illustration texture');
  const contactGradient = contactContext.createRadialGradient(64, 64, 12, 64, 64, 63);
  contactGradient.addColorStop(0, 'rgba(18,30,20,.24)');
  contactGradient.addColorStop(0.5, 'rgba(18,30,20,.14)');
  contactGradient.addColorStop(1, 'rgba(18,30,20,0)');
  contactContext.fillStyle = contactGradient; contactContext.fillRect(0, 0, 128, 128);
  const contactTexture = new THREE.CanvasTexture(contactCanvas); textures.add(contactTexture);
  const shadowMaterial = new THREE.MeshBasicMaterial({ map: contactTexture, transparent: true, depthWrite: false, toneMapped: false }); materials.add(shadowMaterial);
  const floor = mesh(new THREE.PlaneGeometry(9, 6.5), shadowMaterial, scene, false);
  floor.rotation.x = -Math.PI / 2; floor.position.set(0.12, -1.95, 0.08);

  // The combustion cue stays below the surface: an ember seam, never flames.
  const smoulder = new THREE.Group(); layers[3]!.add(smoulder);
  const emberMaterial = standard(0xdc7435, { emissive: 0xf56621, emissiveIntensity: 1.35, roughness: 1 });
  const charMaterial = standard(0x3c2c20, { roughness: 1 });
  const coalParts: THREE.BufferGeometry[] = [], emberParts: THREE.BufferGeometry[] = [];
  for (let i = 0; i < 38; i++) {
    const x = 0.16 + random() * 2.26, y = -0.2 + random() * 0.34;
    const part = transform(new THREE.IcosahedronGeometry(0.035 + random() * 0.055, 0), new THREE.Vector3(x, y, 1.902), new THREE.Vector3(1.7, 0.61, 0.3), new THREE.Euler(0, 0, random() * 0.8));
    (i % 4 ? coalParts : emberParts).push(part);
  }
  mergedMesh(coalParts, charMaterial, smoulder).castShadow = false; mergedMesh(emberParts, emberMaterial, smoulder).castShadow = false;
  const glowingVeins: THREE.BufferGeometry[] = [];
  for (let i = 0; i < 5; i++) {
    const x = 0.22 + i * 0.43;
    const points = [new THREE.Vector3(x, 0.01 + Math.sin(i) * 0.05, 1.927), new THREE.Vector3(x + 0.19, -0.035, 1.929), new THREE.Vector3(x + 0.37, 0.04, 1.925)];
    glowingVeins.push(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(points), 6, 0.009, 3, false));
  }
  mergedMesh(glowingVeins, emberMaterial, smoulder).castShadow = false;

  const smokeCanvas = document.createElement('canvas'); smokeCanvas.width = smokeCanvas.height = 64;
  const smokeContext = smokeCanvas.getContext('2d'); if (!smokeContext) throw new Error('Unable to create illustration texture');
  const gradient = smokeContext.createRadialGradient(32, 32, 2, 32, 32, 31);
  gradient.addColorStop(0, 'rgba(170,174,163,.38)'); gradient.addColorStop(0.45, 'rgba(170,174,163,.2)'); gradient.addColorStop(1, 'rgba(170,174,163,0)');
  smokeContext.fillStyle = gradient; smokeContext.fillRect(0, 0, 64, 64);
  const smokeTexture = new THREE.CanvasTexture(smokeCanvas); textures.add(smokeTexture);
  const emberHaloMaterial = new THREE.MeshBasicMaterial({ map: smokeTexture, color: 0xd85d24, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending, depthWrite: false, toneMapped: false }); materials.add(emberHaloMaterial);
  const emberHalo = mesh(new THREE.PlaneGeometry(2.8, 0.72), emberHaloMaterial, smoulder, false); emberHalo.position.set(1.3, 0.015, 1.898);
  const emberLight = new THREE.PointLight(0xed772c, 0, 2.2, 2); emberLight.position.set(1.4, 0.1, 2.05); smoulder.add(emberLight);
  const smokeMaterial = new THREE.SpriteMaterial({ map: smokeTexture, transparent: true, opacity: 0, depthWrite: false }); materials.add(smokeMaterial);
  const smoke = new THREE.Group(); surface.add(smoke);
  for (let i = 0; i < 5; i++) {
    const puff = new THREE.Sprite(smokeMaterial); puff.position.set(1.55 - i * 0.055, 0.96 + i * 0.34, 0.92 - i * 0.06);
    puff.scale.set(0.5 + i * 0.19, 0.54 + i * 0.2, 1); smoke.add(puff);
  }

  const restoration = new THREE.Group(); surface.add(restoration);
  const damMaterial = standard(0x897653, { map: soilTexture }), damParts: THREE.BufferGeometry[] = [];
  for (let i = 0; i < 4; i++) damParts.push(transform(new THREE.BoxGeometry(0.11, 0.09, 0.64), new THREE.Vector3(1.82, 0.64 + i * 0.065, 0)));
  mergedMesh(damParts, damMaterial, restoration);
  const seedlings: THREE.BufferGeometry[] = [], seedlingMaterial = standard(0x93a35d, { roughness: 1 });
  for (let i = 0; i < 7; i++) {
    const x = -2.36 + i * 0.71, z = 1.53 - (i % 2) * 0.3;
    seedlings.push(cylinderBetween(new THREE.Vector3(x, 0.81, z), new THREE.Vector3(x, 1.12, z), 0.013, 0.007, 4));
    for (let leaf = 0; leaf < 3; leaf++) seedlings.push(transform(new THREE.IcosahedronGeometry(0.09, 1), new THREE.Vector3(x + Math.cos(leaf * 2.1) * 0.06, 1.06 + leaf * 0.045, z + Math.sin(leaf * 2.1) * 0.06), new THREE.Vector3(1, 0.45, 0.65)));
  }
  mergedMesh(seedlings, seedlingMaterial, restoration);

  const stages = [
    { water: 0.52, earth: 0x708652, crown: 0x476641, foliage: 1, fire: 0, recovery: 0, fill: 1 },
    { water: -0.83, earth: 0x9c8557, crown: 0x6d714a, foliage: 0.92, fire: 0, recovery: 0, fill: 0 },
    { water: -1.1, earth: 0x685b42, crown: 0x5b5945, foliage: 0.65, fire: 1, recovery: 0, fill: 0 },
    { water: 0.31, earth: 0x7a8d55, crown: 0x4c704b, foliage: 0.83, fire: 0, recovery: 1, fill: 0 },
  ];
  const stateFor = (stage: number) => { const state = stages[stage]!; return { ...state, earth: new THREE.Color(state.earth), crown: new THREE.Color(state.crown) }; };
  let current = stateFor(0), from = current, destination = current;
  let targetStage = 0, currentProgress = 0, targetProgress = 0;
  let frame = 0, disposed = false, visible = true, contextLost = false;
  let stateStarted = 0, stateAnimating = false, lastTime = 0;
  let width = Math.max(1, container.clientWidth), height = Math.max(1, container.clientHeight);
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
  let motionEnabled=document.body.dataset.cinematicMotion!=='paused';
  const lerp = THREE.MathUtils.lerp, smooth = (value: number) => value * value * (3 - 2 * value);

  function draw() {
    if (disposed || contextLost || !visible) return;
    const p = reduced.matches ? 0.56 : currentProgress;
    // Layers separate briefly to explain the cutaway, then reunite for recovery.
    const gap = reduced.matches ? 0 : Math.sin(Math.PI * p) ** 2 * 0.16;
    layers.forEach((layer, index) => { layer.position.y = (index - 2) * gap; }); surface.position.y = gap * 3;
    surfaceMaterial.color.copy(current.earth); crownMaterial.color.copy(current.crown);
    understoryMaterial.color.copy(current.crown).lerp(new THREE.Color(0x8e9c50), 0.35);
    crowns.forEach((crown, index) => crown.scale.set(1, current.foliage * (index > 10 ? 0.95 : 1), 1));
    const saturatedHeight = Math.max(0.03, current.water + 1.55);
    saturatedFront.scale.y = saturatedSide.scale.y = saturatedHeight;
    saturatedFront.position.y = saturatedSide.position.y = -1.55 + saturatedHeight / 2;
    saturatedMaterial.opacity = 0.19 * (1 - gap * 3); frontLine.position.y = sideLine.position.y = current.water;
    canalWater.position.y = lerp(0.615, 0.76, (current.water + 1.1) / 1.62); reflections.position.y = canalWater.position.y;
    canalFill.visible = current.fill > 0.5; canalWater.visible = reflections.visible = !canalFill.visible;
    smoulder.visible = current.fire > 0.01; emberMaterial.emissiveIntensity = current.fire * 1.7; emberLight.intensity = current.fire * 1.5; emberHaloMaterial.opacity = current.fire * 0.6;
    smoke.visible = current.fire > 0.01; smokeMaterial.opacity = current.fire * 0.65;
    restoration.visible = current.recovery > 0.01; restoration.scale.y = Math.max(0.001, current.recovery);
    const angle = lerp(0.74, -0.06, smooth(p));
    camera.position.set(Math.sin(angle) * 12, lerp(9.4, 5.5, smooth(p)), Math.cos(angle) * 12);
    camera.lookAt(0, lerp(0.64, 0.18, p), 0);
    const aspect = width / height, vertical = Math.max(7.65 + gap * 4, 7.55 / aspect);
    camera.left = -vertical * aspect / 2; camera.right = vertical * aspect / 2; camera.top = vertical / 2; camera.bottom = -vertical / 2;
    camera.updateProjectionMatrix(); renderer.shadowMap.needsUpdate = true; renderer.render(scene, camera);
    container.dataset.ready = 'true';
  }
  function tick(now: number) {
    frame = 0; if (disposed || !visible || contextLost) return;
    if(!motionEnabled){draw();return;}
    const delta = Math.min(50, lastTime ? now - lastTime : 16.7); lastTime = now;
    if (stateAnimating) {
      const fraction = reduced.matches ? 1 : Math.min(1, (now - stateStarted) / 700), t = smooth(fraction);
      current = {
        water: lerp(from.water, destination.water, t), foliage: lerp(from.foliage, destination.foliage, t),
        fire: lerp(from.fire, destination.fire, t), recovery: lerp(from.recovery, destination.recovery, t), fill: lerp(from.fill, destination.fill, t),
        earth: from.earth.clone().lerp(destination.earth, t), crown: from.crown.clone().lerp(destination.crown, t),
      };
      if (fraction === 1) { current = destination; stateAnimating = false; }
    }
    currentProgress = reduced.matches ? targetProgress : lerp(currentProgress, targetProgress, 1 - Math.exp(-delta / 75));
    if (Math.abs(currentProgress - targetProgress) < 0.0005) currentProgress = targetProgress;
    draw();
    if (stateAnimating || currentProgress !== targetProgress) frame = requestAnimationFrame(tick); else lastTime = 0;
  }
  function invalidate() { if (!frame && !disposed && visible && !contextLost) frame = requestAnimationFrame(tick); }
  function setStage(stage: number) {
    if (!Number.isFinite(stage) || disposed) return;
    const next = THREE.MathUtils.clamp(Math.round(stage), 0, 3);
    if (next === targetStage) return;
    targetStage = next; from = current; destination = stateFor(next); stateStarted = performance.now(); stateAnimating = true;
    if (reduced.matches || !visible || !motionEnabled) { current = destination; stateAnimating = false; } invalidate();
  }
  function setProgress(progress: number) {
    if (!Number.isFinite(progress) || disposed) return;
    const next = THREE.MathUtils.clamp(progress, 0, 1);
    if (Math.abs(next - targetProgress) < 0.0001) return;
    targetProgress = next;
    if (reduced.matches || !visible) currentProgress = targetProgress;
    // Reduced motion keeps a single explanatory viewpoint; scroll need not redraw it.
    if (!reduced.matches&&motionEnabled) invalidate();
  }
  function resize() {
    width = Math.max(1, container.clientWidth); height = Math.max(1, container.clientHeight);
    renderer.setSize(width, height, false); invalidate();
  }
  const observer = new ResizeObserver(resize); observer.observe(container);
  const intersection = new IntersectionObserver(([entry]) => {
    visible = !!entry?.isIntersecting;
    if (!visible) { cancelAnimationFrame(frame); frame = 0; lastTime = 0; current = destination; currentProgress = targetProgress; stateAnimating = false; } else invalidate();
  });
  intersection.observe(container);
  const onStage = (event: Event) => setStage((event as CustomEvent<{ stage: number }>).detail?.stage);
  const onProgress = (event: Event) => setProgress((event as CustomEvent<{ progress: number }>).detail?.progress);
  const onReduced = () => { if (reduced.matches) { current = destination; stateAnimating = false; currentProgress = targetProgress; } invalidate(); };
  const onGlobalMotion=(event:Event)=>{motionEnabled=(event as CustomEvent<{enabled:boolean}>).detail.enabled;cancelAnimationFrame(frame);frame=0;lastTime=0;if(motionEnabled){from=current;stateStarted=performance.now();invalidate();}};
  document.addEventListener('documentary-motion',onGlobalMotion);
  const onContextLost = (event: Event) => { event.preventDefault(); contextLost = true; container.dataset.ready = 'false'; cancelAnimationFrame(frame); frame = 0; };
  const onContextRestored = () => { contextLost = false; invalidate(); };
  container.addEventListener('peat-stage', onStage); container.addEventListener('peat-progress', onProgress); reduced.addEventListener('change', onReduced);
  renderer.domElement.addEventListener('webglcontextlost', onContextLost); renderer.domElement.addEventListener('webglcontextrestored', onContextRestored);
  renderer.setSize(width, height, false); draw();
  return {
    setStage, setProgress,
    dispose() {
      disposed = true; document.removeEventListener('documentary-motion',onGlobalMotion);cancelAnimationFrame(frame); observer.disconnect(); intersection.disconnect();
      container.removeEventListener('peat-stage', onStage); container.removeEventListener('peat-progress', onProgress); reduced.removeEventListener('change', onReduced);
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost); renderer.domElement.removeEventListener('webglcontextrestored', onContextRestored);
      geometries.forEach((geometry) => geometry.dispose()); materials.forEach((material) => material.dispose()); textures.forEach((texture) => texture.dispose());
      sun.shadow.map?.dispose(); renderer.dispose(); renderer.domElement.remove(); delete container.dataset.ready;
    },
  };
}
