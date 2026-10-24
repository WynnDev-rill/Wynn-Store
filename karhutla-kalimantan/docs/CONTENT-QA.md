> Catatan audit versi awal. Untuk perubahan fokus Oktober 2026, lihat [REVISION-2026.md](REVISION-2026.md) dan hasil pemeriksaan revisi pada [QA.md](QA.md).

# Documentary content QA

Checked: 24 October 2026. Reviewer scope: read-only `src/pages/index.astro`, `src/pages/sumber.astro`, `src/data/content.ts`, provincial data, media captions/rights and video captions. No implementation files changed.

## Verdict

No critical factual errors identified. Provincial values, geographic scope, historical cost estimate, health description and restoration framing agree with the research sources. Three small corrections recommended before release.

## Findings

| Priority | Location | Finding | Recommended repair |
|---|---|---|---|
| Minor | `public/media/fire-weather-id.vtt` | `ffprobe` measures the video at **30.500 seconds**; final cue runs24.000–40.000. The final cue can display only until the video ends. | Change last cue end to30.500. No missing audio transcript: file has video-only stream. |
| Minor | `index.astro` timeline + noscript fallback | Main 2019 panel correctly includes both NASA2019 and KLHK. The no-JavaScript2019 fallback retains only NASA2019, while text includes a KLHK-derived area total. 2023 panel's satellite sentence has only the KLHK citation. | Include KLHK with the2019 no-JavaScript text; appendNASA2023 for the2023 panel and fallback. |
| Minor | `content.ts` chronology2019/2023 | “KLHK mencatat total684.599…” and the2023 aggregate can imply a directly printed regional aggregate, whereas the project sums five provincial entries. Methodology correctly says these totals are calculated by the project. | Use “Penjumlahan data lima provinsi dari KLHK menghasilkan…” or an equally concise phrasing. |

Optional editorial polish: in `sumber.astro`, “Foto pembuka: Terra/MODIS” can become “Citra pembuka” for consistency with satellite terminology.

## Verified facts and citations

- All25 province/year values match *Statistik KLHK2023*,Table7.2, printedp161, PDF one-based177. Same edition consistently used, including Kalbar111848.43 and Kaltara796.36 in2023. Calculated five-province totals2019=684599ha;2023=508430.22ha. Map remains an annual archive; no fabricated live measurements.
- World Bank Rp221trillion is an estimated2015 Indonesian-wide damage/loss total, with long-term health/global exclusions described. About2.6million hectares,June–October2015, nationwide scope is correct. Fragment `#page=29` verified against PDF screenshot: opens printedp18 and the start of the fire section.
- Page2002 is correctly limited to CentralKalimantan research on1997; no study-area number is passed off as whole-island total. The1982–83 entry stays qualitative and is supported by IPB's historical institutional summary.
- NASA2019's IndianOceanDipole context is handled accurately; the copy avoids claiming every major fire year was ElNiño-driven.
- WHO-backed health copy accurately describes irritation, asthma aggravation and vulnerable groups. No invented casualty counts, modeled mortality presented as observed, or invented witness quotations.
- Peat mechanism is labeled schematic and unscaled. Restoration describes water retention/hydrological monitoring, vegetation and community participation; the counterfactual model is clearly identified as modeling.
- Satellite dates/instruments verified: Terra/MODIS2Oct2023; Landsat9 OLI-2 nearBanjarbaru7Aug2022 and27Sep2023. False-color labeling is present. The comparison isn't portrayed as isolating a cause.
- Sebangau photo caption matches Wikimedia metadata:21Mar2021, Mankdhayrahman, SungaiSebangau/PalangkaRaya riparian wetland. It is explicitly presented as landscape context and not evidence of restoration success. Attribution,CCBY-SA4.0 and resizing/compression notice are included.
- NASA media permissions acknowledge factual educational use and distinguish third-party rights; no blanket claim that all NASA-hosted assets are public domain. Geometric source metadata agrees with stored geoBoundaries edition2017/buildDec2023,OSM/Wambacher andODbL.

## Link verification

Read-only HTTP GETs checked the15 bibliographyURLs once, with first30KB read and no repeated failed probes. Existing successful web research also checked text/PDF content. **No actual404 or broken bibliography URL detected.**

| Source | Result | Interpretation |
|---|---|---|
| Main KLHK2023 PDF | HTTP200,application/pdf | Correct primary data source; page177 verified visually earlier. |
| NASA2023 | HTTP200,correct article title | Direct source works. |
| PageNature DOI | HTTP200,correct Nature article title | DOI resolves. Returned URL contains publisher cookie notice; original DOI is still appropriate. |
| IPB1982–83 | HTTP200,correct article title | Institutional history source works. |
| WorldBankPDF | Raw HTTP403,error1010 | Accessible through web retrieval and PDF screenshot. Environment/client filtering; do not call broken or remove. |
| NASA2015 | HTTP200,correct article title | Direct source works. |
| NASA2019 | HTTP200,correct article title | Direct source works. |
| WHOfact sheet | Raw HTTP403,error1010 | Full correct page successfully retrieved through web research. Client filtering, not proven broken. |
| CIFOR/ForestsNews | Raw HTTP403,error1010 | Web retrieval successfully returned correct27Oct2015 documentary article. Client filtering, not broken. |
| BNFhydrology | Raw HTTP403,error1010 | Full correct article successfully retrieved in research. Client filtering, not broken. |
| KielyNature DOI | HTTP200,correct NatureCommunications title | DOI works; cookie notice same as above. Abstract independently verified viaPMC metadata. |
| NASASVS4634 | HTTP200,correct title | Video scientific source works. |
| geoBoundariesADM1API | HTTP200,application/json | Web retrieval tool cannot parse thisURL, but direct HTTP verifies it works. Stored metadata inspected. |
| WikimediaSebangau | Raw HTTP403,robot-policy message | Correct file page retrieved successfully by web tool. Original image downloaded and itsSHA1 matched page metadata. |
| NASArights | HTTP200,correct guidelines title | Appropriate media-rights link works. |

The BMKG current-conditions destination `https://www.bmkg.go.id/cuaca/karhutla` was independently retrieved and has the correct official warning/monitoring page. Data there is not copied as a realtime feed.

## Video inspection

Command: `ffprobe -v error -show_entries format=duration:stream=codec_type,width,height,r_frame_rate -of json public/media/indonesia-fire-weather-2015.webm`.

Result:30.500s;1920×1080;30fps;one video stream;no audio. Frame extracted at15s visually checked: datedAug30,2015; black circles indicate active-fire detections; blue/white overlay is precipitation; green-to-red base isDroughtCode. The VTT's legend description agrees with this frame and the NASA source. Four explanatory Indonesian cues are descriptions, not an invented spoken transcript.

## Limits

This pass checks source adequacy, source existence and content. It is not a new satellite analysis, field investigation, legal opinion on licensing, health diagnosis, or performance audit. Root agent handles rendered browser, interaction, accessibility and deployment verification.

## Resolved before release

The root implementation now trims VTT to 30.500 seconds, cites both KLHK and NASA for 2019 and 2023 including no-JS text, labels provincial sums as project calculations, and starts the World Bank link at PDF page 29. Source-edition data remain unchanged.
