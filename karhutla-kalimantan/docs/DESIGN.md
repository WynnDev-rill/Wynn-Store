> Catatan rancangan awal. Galeri/pemutar dan pembuka telah diganti pada revisi pengalaman; lihat [EXPERIENCE-REDESIGN.md](EXPERIENCE-REDESIGN.md) untuk desain yang diterbitkan sekarang.

# Kalimantan: Di balik asap — art direction

## Brief and research decision
Original documentary, Indonesian, factual and public-facing. User references OHZI and Minh Pham supply ambition and interaction craft, not a template. Research of 12+ live websites precedes visual implementation; see DESIGN-REFERENCES.md. No borrowed layouts, no gate loader, cursor replacement or scroll hijacking.

## Concept: Dari langit ke lapisan tanah
A satellite opens a journey that descends into peat, follows the people affected and returns to water and recovery. Fire orange is a data signal, not ambient visual noise. The homepage is a continuous documentary with chapter anchors; /sumber/ is an auditable bibliography and methodology.

## Tokens
- Paper #f7f8f5: reading surface, clean mineral white.
- Ink #182c28: type and hydrological storytelling.
- Deep forest #142823: immersive areas.
- Silt #dce4dc: scientific canvas/background.
- Ember #aa3e20: burned area and fire emphasis (accessible on paper).
- Water #2d666c: water state and comparison labels.
Typography: self-hosted Manrope Variable for large geological scale and clear UI; Newsreader Variable for a quieter editorial counterpoint in reading passages. Headings left aligned, finite width and intentional line breaks.

## Composition and interaction
Full-bleed real satellite opening with an oversized geographic title. White reading chapters alternate with dark map/peat stages. Main reading width 65ch, desktop grid 12 columns, mobile 20px gutter. Generous image ratios and one photodocumentary credit per image. Timeline is user selected and always navigable without swiping; SVG province map is keyboard reachable, synchronized to a select/table and year buttons. Scientific peat diorama is dynamically imported on intersection, rendered only on changes. Before/after is a native range (keyboard accessible), not an unlabeled drag handle. Archive dialog has focus restoration and Escape; real NASA video requires an explicit play action and has text description and captions.

## Restraint review
Initial skill query suggested generic spatial glass UI; rejected as irrelevant to environmental journalism. Narrower editorial guidance adopted for legibility only. No SaaS cards, arbitrary metric strip, fake live feed, fake witness quotes, synthetic flames or AI documentary photographs. 3D kept in one explanatory chapter; satellite hero receives a restrained scroll-linked transform only when motion is allowed.

## Quality floor
Semantic static HTML survives JS failure. No sound autoplay, no continuous heavy GPU loop, no externally hosted font dependency, no cookies/trackers. Reduced motion, keyboard paths, 44px targets, source citations, mobile layouts. Historical data clearly dated and scoped. Data map is not hotspot positions or a realtime fire-warning service.
