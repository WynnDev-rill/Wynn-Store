# Kalimantan karhutla — verified research dossier

Research checked 24 October 2026. This is an intermediate editorial dossier, not a live monitoring feed. Use the historical scope explicitly (main statistical edition: KLHK 2023). All suggested prose is paraphrase; no invented witness testimony.

## 1. Consistent provincial dataset: use this table

**Primary source:** *Statistik Kementerian Lingkungan Hidup dan Kehutanan 2023*, Volume IX, Table 7.2, printed page 161 (PDF zero-based page 176). Provider: Direktorat Jenderal Pengendalian Perubahan Iklim.

PDF: https://statistik.kehutanan.go.id/storage/publication/attachment/publication/buku-statistik-2023-kementerian-lingkungan-hidup-dan-kehutanan.pdf

Publication page: https://statistik.kehutanan.go.id/publication/show/buku-statistik-2023-kementerian-lingkungan-hidup-dan-kehutanan

| Province | 2019 ha | 2020 ha | 2021 ha | 2022 ha | 2023 ha |
|---|---:|---:|---:|---:|---:|
| Kalimantan Barat | 151919 | 7646 | 20590 | 21836 | 111848.43 |
| Kalimantan Selatan | 137848 | 4017 | 8625 | 429 | 190394.58 |
| Kalimantan Tengah | 317749 | 7681 | 3653 | 1554 | 165896.44 |
| Kalimantan Timur | 68524 | 5221 | 3029 | 373 | 39494.41 |
| Kalimantan Utara | 8559 | 1721 | 1678 | 370 | 796.36 |

PDF screenshot of this exact page checked visually using web tool. Use its English-style decimal punctuation as numbers, and format Indonesian punctuation in UI. Sum of these five provinces, **calculated by this project**, 2019: **684599 ha**, 2021: **37575 ha**, 2023: **508430.22 ha**. Do not call these totals national totals. All five years are annual area, not hotspot counts, not number of fire incidents, not necessarily forest-only area. Chloropleth should encode absolute area in hectares, not percentage of a province burned. Cite edition and table on the map and CSV.

**Resolved source discrepancy:** An earlier Direktorat PKHL performance report appendix contains Kalbar 2023 printed `111,484,43` and Kaltara `796,39`. Main national 2023 statistical publication above has **111848.43 and 796.36**. Use one edition consistently; do not merge the appendix values. BMKG independently states Kalbar around 111848 ha in 2023: https://www.bmkg.go.id/siaran-pers/cegah-karhutla-bmkg-semai-13-ton-naci-powder-di-langit-kalbar . The old statistik.menlhk.go.id domain failed DNS; migrated kehutanan.go.id PDF works in web retrieval.

## 2. Chronology for story, with careful geography

### 1982–1983: warning in East Kalimantan

Suggested prose: “Kekeringan parah diikuti kebakaran besar di Kalimantan Timur pada 1982–1983. Peristiwa ini menjadi salah satu penanda awal dalam pembahasan karhutla modern Indonesia.”

IPB University reports around **3.6 million hectares of forest and land in East Kalimantan** affected. If using this number label it an historical estimate, not a modern comparable SiPongi annual series.

Primary institutional source (14 Apr 2014): https://www.ipb.ac.id/news/index/2014/04/ipb-offered-the-solution-to-control-the-fire-of-forest-and-land-844c7753c94bb1f85f6526dc738b2cdf/

Academic history reference: Rona Dennis (1999), *A review of fire projects in Indonesia (1982–1998)*, CIFOR. DOI https://doi.org/10.17528/cifor/000564 ; publisher PDF https://www.cifor-icraf.org/publications/pdf_files/FireReport.pdf ; catalog https://agris.fao.org/search/en/records/672359e7b605bda15e0a796e . Publisher PDF not fetchable during this research, so do not claim its full contents inspected.

### 1997–1998: peat carbon becomes visible to science

Page et al. (2002), *Nature* 420, 61–65, DOI https://doi.org/10.1038/nature01131 . Abstract verified via PubMed PMID12422213. In a **2.5-million-ha study area in Central Kalimantan**, authors estimated **0.79 million ha burned in 1997**, 91.5% of that burned area peatland. These are study-area results, not totals for all Kalimantan. Drainage and forest clearing make tropical peat susceptible to fire. Suggested UI use: qualitative chronology point; optionally use study area with full scope, not national extrapolated carbon estimates.

### 2015: severe drought and an Indonesian-wide crisis

NASA institutional research summary: https://www.giss.nasa.gov/research/features/201608_heat/ and https://www.nasa.gov/centers-and-facilities/goddard/severe-2015-indonesian-fire-season-linked-to-el-ni%C3%B1o-drought/ . July–October dryness associated with 2015–16 El Niño helped fires escape into drained/degraded peat.

World Bank, *Indonesia Economic Quarterly*, December 2015, printed pages18–23: https://documents1.worldbank.org/curated/en/352041467995622739/pdf/105664-WP-PUBLIC-ADD-SERIES-IEQ-DEC-2015-ENG.pdf . **Indonesia-wide**, June–October2015: around **2.6 million ha** burned; estimated damage and losses **Rp221trillion / US$16.1billion**, 1.9% of2015 GDP. Estimate covers multiple sectors, includes short-term health/school closure costs, excludes full long-term health burden and regional/global losses. Do not label as Kalimantan-only. Important to label as an estimate and historical2015 value.

### 2019: recurrence, with a different ocean-climate context

NASA,18Sep2019: https://science.nasa.gov/earth/earth-observatory/smoke-blankets-borneo-145614/ . Smoke over Borneo and peat fires are documented in satellite imagery. It explains that El Niño was neutral in that fire season and positive Indian Ocean Dipole helped dry conditions. **Avoid blanket claim that every major fire year, or 2019 fire peak, was caused by El Niño.**

Use map dataset2019 values from Table7.2 for scale, not numbers copied from contemporary partial-year reports.

### 2023: fire returns after relatively quiet seasons

NASA,7Oct2023: https://science.nasa.gov/earth/earth-observatory/indonesian-fires-return-in-2023-151929/ . Terra MODIS image2Oct2023 shows smoke over South/Central Kalimantan; Landsat9 images compare the same area near Banjarbaru on7Aug2022 and27Sep2023. False color must be stated. Dry conditions related to El Niño and positive IOD helped intensify fires. Do not repeat the article’s early-October cumulative figure as a full-year total.

## 3. Mechanism and causes

Suggested mechanism copy (scientific schematic, not quantitative simulation):

1. “Dalam keadaan basah, gambut menyimpan air dan sisa tumbuhan yang terurai lambat.”
2. “Kanal dan drainase menurunkan muka air. Bagian gambut di atas air menjadi lebih kering.”
3. “Saat ada sumber penyalaan, bahan organik yang kering dapat terbakar hingga di bawah permukaan.”
4. “Bara dapat merambat perlahan dan terus mengeluarkan asap. Memadamkan nyala di permukaan belum tentu menghentikan pembakaran di dalam gambut.”

Primary scientific source for peat/drainage: Page2002 above; primary operational illustration https://svs.gsfc.nasa.gov/4634/ . Label any drawn cross-section “Ilustrasi mekanisme · tidak berskala”; no fabricated depths, rates, temperatures, water-table forecasts, or “risk percentages.” A slider changing moisture is conceptual, no causal prediction of exact fire risk.

Separate ignition sources from enabling conditions: agricultural/land-clearing burning and escaped fire can provide ignition; drainage/forest clearing alter fuel/water; drought lengthens dryness; governance/tenure/economic incentives affect decisions. Avoid blaming an entire community or claiming any named company lit a particular fire without case evidence.

Economic/governance study for deeper reading: Purnomo et al.(2017), “Fire economy and actor network of forest and land fires in Indonesia”, https://www.sciencedirect.com/science/article/pii/S1389934117300023 . Publisher metadata verified, full text not retrieved here.

## 4. Impacts: use precise claims, no unsupported witness quotes

WHO fact sheet: https://www.who.int/news-room/fact-sheets/detail/wildfires-and-health . Smoke contains fine particulate matter, including PM2.5. Exposure can irritate eyes and airways and aggravate asthma. Children, older people, pregnant women and people with preexisting lung/heart conditions are especially vulnerable. Pollution can travel far downwind. Suggested safety link to official authorities rather than dynamic AQI generated by the site. **No specific mortality number needed.** Modeled excess mortality and officially reported acute deaths are different measurements and should never be conflated.

CIFOR contemporaneous documentary report27Oct2015: https://www.forestsnews.org/36564 . Its field visit in midOctober2015 to Palangka Raya documented school disruption, harvest loss, orangutan habitat impacts and smoky landscapes. Safe short UI prose: “Asap mengganggu sekolah dan kehidupan sehari-hari; kebakaran juga merusak habitat.” Link factual article instead of inventing an individual experience.

Carbon stored in peat is released when organic material burns (Page2002). Repeated loss of vegetation and peat changes ecosystem structure. Avoid substituting tree-cover loss data for fire area.

## 5. Restoration: water, vegetation, livelihoods, lasting monitoring

Borneo Nature Foundation,12May2020: https://borneonaturefoundation.org/conservation/hydrological-monitoring-for-protecting-peatlands/ . Work in Sebangau Central Kalimantan combines hydrological measurements, canal dams, rewetting, reforestation, and community nurseries. Dams aim to reduce loss of water through canals; their placement depends on hydrological monitoring. Suggested UI sequence: “Jaga air → Pulihkan vegetasi lokal → Dukung penghidupan dan patroli warga → Pantau dalam jangka panjang.” Do not imply tree planting alone restores drained peat, or a single dam makes all peat safe.

UNEP pilot2021 https://www.unep.org/en/news-and-stories/technical-highlight/community-based-peatlands-restoration-protecting-ancestor describes the3R approach in **North Sumatra**, not Kalimantan. Useful explanation/reference, but do not relabel its people/photos as Kalimantan.

Research benefits caveat: Kiely et al.(2021), *Nature Communications*12,7044 https://doi.org/10.1038/s41467-021-27353-x . Abstract verified via PMC8639972 metadata. Counterfactual modeling estimates restoration could reduce burning/emissions/health burden. Such results are modeled scenarios, not measured completed restoration impact. Recommend qualitative statement only.

## 6. Media assets and exact captions

### Satellite images (best reuse choice)

NASA2023 article above credits Lauren Dauphin, using MODIS LANCE/GIBS and Landsat USGS data.

- Smoke Borneo, **2Oct2023**, Terra/MODIS, natural-color image: https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/imagerecords/151000/151929/indsmoke_tmo_2023275_lrg.jpg
- Banjarbaru vicinity, **7Aug2022**, Landsat9 OLI2, **false color**: https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/imagerecords/151000/151929/indofires_oli2_2022219_lrg.jpg
- Same area, **27Sep2023**, Landsat9 OLI2, **false color**: https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/imagerecords/151000/151929/indofires_oli2_2023270_lrg.jpg
- Borneo smoke **14Sep2019**, Aqua/MODIS: https://assets.science.nasa.gov/dynamicimage/assets/science/esd/eo/images/imagerecords/145000/145614/indonesia_amo_2019257_lrg.jpg . Credit NASA Earth Observatory/Joshua Stevens, MODIS EOSDIS/LANCE/GIBS.

NASA download links currently append crop/150px query by default; omit dynamic sizing query to retrieve full source asset. Confirm downloaded image dimensions/content before use. Do not present false-color vegetation/burn scars as normal-color flames.

### Short scientific video

Source/credits https://svs.gsfc.nasa.gov/4634/ ; credit **NASA’s Scientific Visualization Studio** (visualizer ChengZhang, data visualizer HoraceMitchell).

-3.9MB WebM1080p: https://svs.gsfc.nasa.gov/vis/a000000/a004600/a004634/Indonesia_Jun_Nov_2015_activeFirePrecipDC_1080p30.webm
-Smaller simple layer3.2MB: https://svs.gsfc.nasa.gov/vis/a000000/a004600/a004634/Indonesia_Jun_Nov_2015_activeFireDC_1080p30.webm
-Poster: https://svs.gsfc.nasa.gov/vis/a000000/a004600/a004634/Indonesia_Jun_Nov_2015_activeFireDC_1080p30.00694_print.jpg

Caption: “Visualisasi ilmiah: deteksi kebakaran aktif, hujan, dan indeks kekeringan Indonesia, Juni–November2015. Lingkaran hitam adalah deteksi api aktif; bukan luas terbakar.” Native video controls, no autoplay audio; `preload=none` with poster.

### Documentary ground photos (optional, more restrictive rights)

- https://www.flickr.com/photos/cifor-icraf/35084189403 ; file https://live.staticflickr.com/4305/35084189403_2935a8acbd.jpg . “Gambut terbakar di hutan penelitian Tumbang Nusa, luar Palangka Raya, Kalimantan Tengah.14Oktober2015. Foto:AuliaErlangga/CIFOR.”
- https://www.flickr.com/photos/cifor-icraf/35084266763 ; file https://live.staticflickr.com/4327/35084266763_569348a893.jpg . “Kebakaran gambut di luar Palangka Raya, Kalimantan Tengah.14Oktober2015. Foto:AuliaErlangga/CIFOR.”

License links on these individual Flickr pages were inspected: **CCBY-NC-ND2.0**, https://creativecommons.org/licenses/by-nc-nd/2.0/ . Can redistribute noncommercial unchanged photo with attribution/source/license. Avoid color treatments/cropping/AI modification; prefer full original in archive. Rehosting editorial image under this license is distinct from repository code license. Image fetch through web tool403, parent may download via permitted HTTP. No messages sent to photographers.

### NASA media permissions

Current source: https://www.nasa.gov/nasa-brand-center/images-and-media/ . NASA material generally may be used for factual educational/informational pages with acknowledgment, without endorsement. Third-party material on NASA pages can remain copyrighted; NASA does not convey third-party rights. **Do not blanket-label all NASA-hosted photos public domain.** Use explicit NASA/USGS-produced satellite assets above; maintain credits and usage note.

## 7. Content QA rules

- Freeze data source edition and access date. Do not label archive as realtime.
- Area, hotspot, incident, peat extent, tree-cover loss and exposure are distinct units.
- Five Indonesian provinces are Kalimantan; Borneo also contains Malaysia/Brunei. Maps should preserve context without suggesting all Borneo belongs to Indonesia.
- Diagrams are labeled schematic and no generated imagery passes as documentary.
- No quote marks around invented composite testimony. Use paraphrases with source links.
- Before/after2022/2023 image is temporal comparison; it is not a controlled causal proof.
- Restore water management and living local systems, not simplistic tree-planting solution.
- Use current official portals for ongoing conditions: https://sipongi.gakkum.kehutanan.go.id/ and https://www.bmkg.go.id/cuaca/karhutla . Official portal homepage sometimes renders zeros in crawler before dynamic data; do not treat those zeros as actual monitoring observations.
