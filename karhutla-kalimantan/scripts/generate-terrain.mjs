/** Reproducible simplified outline derivative. See docs/licenses/cinematic-geography.md. */
import {readFileSync,writeFileSync} from 'node:fs';
const data=JSON.parse(readFileSync(new URL('../public/data/kalimantan-boundaries.geojson',import.meta.url)));
const result=data.features.map(feature=>({name:feature.properties.shapeName,rings:feature.geometry.coordinates.flatMap(polygon=>{
 const ring=polygon[0];
 const area=Math.abs(ring.reduce((sum,a,i)=>{const b=ring[(i+ring.length-1)%ring.length];return sum+a[0]*b[1]-b[0]*a[1];},0))/2;
 if(area<.018)return [];
 const points=[];for(const [lon,lat] of ring){const xy=[Number(((lon-113.5)*.82).toFixed(3)),Number(((lat+.5)*.82).toFixed(3))];if(!points.length||Math.hypot(xy[0]-points.at(-1)[0],xy[1]-points.at(-1)[1])>.035)points.push(xy);}
 return points.length>2?[points]:[];
})}));
writeFileSync(new URL('../src/data/terrain-outlines.json',import.meta.url),JSON.stringify(result));
console.log('Wrote five-province illustrative terrain outlines.');
