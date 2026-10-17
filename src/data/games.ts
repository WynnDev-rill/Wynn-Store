import mobileLegendsArtwork from '../assets/games/mobile-legends.svg';
import freeFireArtwork from '../assets/games/free-fire.svg';
import pubgMobileArtwork from '../assets/games/pubg-mobile.svg';
import honorOfKingsArtwork from '../assets/games/honor-of-kings.svg';
import genshinImpactArtwork from '../assets/games/genshin-impact.svg';
import wutheringWavesArtwork from '../assets/games/wuthering-waves.svg';
import type { Game, Product, Promo } from '../types';
export const games: Game[] = [
  ['mobile-legends','Mobile Legends','Moonton','MOBA',mobileLegendsArtwork,'from-violet-600 to-blue-600','Pertarungan MOBA 5v5 paling seru dengan top up instan.',4.9,'24K'],
  ['free-fire','Free Fire','Garena','Battle Royale',freeFireArtwork,'from-orange-500 to-red-600','Bertahan hidup, raih Booyah, dan tampil lebih keren.',4.8,'18K'],
  ['pubg-mobile','PUBG Mobile','Tencent Games','Battle Royale',pubgMobileArtwork,'from-amber-500 to-yellow-700','Pengalaman battle royale realistis di perangkat mobile.',4.9,'16K'],
  ['honor-of-kings','Honor of Kings','Level Infinite','MOBA',honorOfKingsArtwork,'from-cyan-500 to-blue-700','Jelajahi medan pertempuran kompetitif kelas dunia.',4.7,'9K'],
  ['genshin-impact','Genshin Impact','HoYoverse','RPG',genshinImpactArtwork,'from-sky-400 to-indigo-600','Mulai petualangan epikmu di dunia Teyvat.',4.9,'12K'],
  ['wuthering-waves','Wuthering Waves','Kuro Games','RPG',wutheringWavesArtwork,'from-teal-400 to-slate-700','Bangkit sebagai Rover dalam dunia pasca-apokaliptik.',4.8,'7K'],
].map(([id,title,publisher,category,image,accent,description,rating,sold]) => ({id,title,publisher,category,image,accent,description,rating,sold} as Game));
export const products: Product[] = ['86 Diamonds','172 Diamonds','257 Diamonds','344 Diamonds','429 Diamonds','514 Diamonds'].map((amount,i)=>({id:`diamond-${i+1}`,gameId:'mobile-legends',amount,bonus:i>2?`+${i*3} Bonus`:undefined,price:[22000,43000,63000,84000,104000,124000][i]}));
export const promos: Promo[] = [
  {id:'p1',title:'Weekend Gaming',description:'Nikmati harga spesial untuk semua game pilihan.',code:'WEEKEND20',discount:'20% OFF'},
  {id:'p2',title:'First Top Up',description:'Bonus untuk transaksi pertamamu di Wynn Store.',code:'NEWPLAYER',discount:'15% OFF'},
  {id:'p3',title:'Mabar Lebih Hemat',description:'Ajak squad top up dan dapatkan cashback.',code:'MABAR10',discount:'10% CASHBACK'},
];
