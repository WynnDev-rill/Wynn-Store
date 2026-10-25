/** Editorial chronology. Dates refer to the event or reporting period stated by each source. */
export const seasonResearchDate = '25 Oktober 2026';

export interface SeasonEntry {
  id: string;
  month: string;
  date: string;
  dateTime: string;
  place: string;
  title: string;
  lead: string;
  detail: string;
  publicationDate: string;
  sourceTitle: string;
  sourceUrl: string;
}

export const seasonEntries: SeasonEntry[] = [
  {
    id: 'musim-januari',
    month: 'Januari',
    date: '17 Januari',
    dateTime: '2026-01-17',
    place: 'Kalimantan Barat',
    title: 'Catatan kebakaran sejak awal tahun',
    lead: 'Rilis Gakkum 28 Januari menyebut kebakaran di Kalimantan Barat mulai terjadi sejak 17 Januari. Pemadaman berlangsung di sejumlah kabupaten, termasuk Kubu Raya, Mempawah, Sambas, dan Ketapang.',
    detail: 'Sebagian lokasi dinyatakan aman pada 25 Januari, sementara pemadaman masih berlangsung di titik lain. Tanggal 17 Januari adalah catatan awal untuk Kalimantan Barat dalam laporan ini, bukan tanggal penyalaan seluruh Kalimantan.',
    publicationDate: '28 Januari 2026',
    sourceTitle: 'Gakkum Kehutanan · respons awal Kalbar',
    sourceUrl: 'https://gakkum.kehutanan.go.id/siaran-pers/detail/cegah-karhut-meluas-manggala-agni-bergerak-cepat-di-kalimantan-barat',
  },
  {
    id: 'musim-juli',
    month: 'Juli',
    date: '6–17 Juli',
    dateTime: '2026-07-17',
    place: 'Kalimantan Selatan',
    title: 'Kesiagaan diperkuat menjelang kemarau',
    lead: 'Kalimantan Selatan menetapkan siaga darurat sejak 6 Juli. Rilis 17 Juli mencatat 180 personel Manggala Agni disiagakan di Tanah Bumbu, Banjarbaru, dan Tanah Laut untuk patroli, deteksi dini, dan pemadaman.',
    detail: 'Posko BPBD mulai aktif pada 15 Juli. Hingga 16 Juli, petugas bersama para pihak telah melakukan 107 operasi pemadaman di provinsi ini. Jumlah operasi tersebut berbeda dari jumlah hotspot maupun luas lahan terbakar.',
    publicationDate: '17 Juli 2026',
    sourceTitle: 'Kemenhut · kesiagaan Kalimantan Selatan',
    sourceUrl: 'https://www.kehutanan.go.id/pers/kementerian-kehutanan-perkuat-pengendalian-karhutla-di-kalimantan-selatan-dan-seluruh-wilayah-rawan-hadapi-puncak-musim-kemarau-2026',
  },
  {
    id: 'musim-agustus',
    month: 'Agustus',
    date: '20 Agustus',
    dateTime: '2026-08-20',
    place: 'Kalimantan Barat, Tengah, dan Selatan',
    title: 'Asap dan keterbatasan jarak pandang',
    lead: 'Pemerintah memperkuat operasi terpadu setelah kenaikan hotspot dan laporan kabut asap. Penanganan mencakup pemadaman darat, sumber air, dan dukungan udara, dengan prioritas pada Kalimantan Barat, Tengah, dan Selatan.',
    detail: 'Rilis ini menindaklanjuti laporan warga dari Pontianak, Palangka Raya, Banjarbaru, Banjarmasin, dan Sungai Tabuk. Pengamatan jarak pandang berbeda menurut lokasi dan waktu. Operasi udara serta modifikasi cuaca bergantung pada cuaca dan keselamatan penerbangan.',
    publicationDate: '20 Agustus 2026',
    sourceTitle: 'Kemenhut · penguatan operasi terpadu',
    sourceUrl: 'https://www.kehutanan.go.id/news/pemerintah-perkuat-operasi-terpadu-karhutla-di-kalimantan-tambah-dukungan-udara-dan-pemadaman-darat',
  },
  {
    id: 'musim-september',
    month: 'September',
    date: '20–21 September',
    dateTime: '2026-09-21',
    place: 'Palangka Raya, Kalimantan Tengah',
    title: 'Asap ikut menghambat penerbangan',
    lead: 'Pada 20 September, jarak pandang di Bandara Tjilik Riwut sempat turun hingga sekitar 600 meter. Rilis Kemenhut sehari kemudian mencatat gangguan pada 14 pergerakan pesawat: tujuh kedatangan dan tujuh keberangkatan.',
    detail: 'Pemadaman, pembasahan, dan pendinginan terus dilakukan bersama patroli serta operasi udara. Angka armada gabungan dalam rilis mencakup berbagai wilayah terdampak di Indonesia; angka itu tidak dipakai sebagai jumlah armada Kalimantan saja.',
    publicationDate: '21 September 2026',
    sourceTitle: 'Kemenhut · operasi dan dampak asap September',
    sourceUrl: 'https://www.kehutanan.go.id/news/pengendalian-karhutla-kalteng-sumsel-diperkuat-64-armada-udara-dikerahkan',
  },
  {
    id: 'musim-oktober',
    month: 'Oktober',
    date: '24–25 Oktober',
    dateTime: '2026-10-25',
    place: 'Kalimantan Barat, Tengah, dan Selatan',
    title: 'Deteksi berkurang, operasi berlanjut',
    lead: 'Rilis 25 Oktober melaporkan penurunan hotspot pada cuplikan 24 Oktober pukul 21.00 WIB. Pemadaman, pemeriksaan lapangan, dan pendinginan tetap dilakukan untuk memastikan api serta bara di bawah permukaan benar-benar padam.',
    detail: 'Kemenhut juga melaporkan pelepasliaran dua orangutan di Ketapang pada 24 Oktober setelah evakuasi dan pemeriksaan. Respons kebakaran mencakup perlindungan habitat; berkurangnya deteksi satelit belum menjadi bukti seluruh lokasi telah aman.',
    publicationDate: '25 Oktober 2026',
    sourceTitle: 'Kemenhut · pembaruan operasi 25 Oktober',
    sourceUrl: 'https://www.kehutanan.go.id/news/hotspot-turun-1-019-titik-operasi-karhutla-dan-perlindungan-satwa-tetap-diperkuat-2',
  },
];

export const latestSeasonSnapshot = {
  observedAt: '2026-10-24T21:00:00+07:00',
  observedLabel: '24 Oktober 2026 · 21.00 WIB',
  publicationDate: '2026-10-25',
  publicationLabel: '25 Oktober 2026',
  sourceUrl: seasonEntries[4].sourceUrl,
  unit: 'hotspot high confidence',
  values: { tengah: 60, barat: 55, selatan: 0, timur: null, utara: null },
  national: 317,
  note: 'Kalimantan Timur dan Utara tidak dirinci. Nilai nihil di Kalimantan Selatan hanya berlaku untuk deteksi berkepercayaan tinggi dalam cuplikan ini.',
} as const;
