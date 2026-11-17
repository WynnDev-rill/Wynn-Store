# Assets
- app_icon.png (drawable-nodpi): adapted with image generation from the user-supplied icon, 2026-11-17. Full square without pre-applied outer mask; Android adaptive mask applies at runtime. Original generated master retained in working environment; shipping asset in drawable-nodpi.
- Nunito variable TTF: official google/fonts ofl/nunito; OFL text bundled.
- Dictionary and examples: see dictionary.json input hashes and ATTRIBUTION.txt. No stock or reference artwork redistributed.

Monochrome Android themed icon: authored native vector representing layered memo cards, no third-party artwork.

Nunito weights 400/500/600/700/800 instantiated with fontTools from the official variable master in design-assets/. This avoids the variable font defaulting to its lightest weight on Android. Reproduction: scripts/prepare-fonts.py. OFL retained.
