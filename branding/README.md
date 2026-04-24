# TuneFlow Branding

This folder is the canonical home for approved TuneFlow brand assets.

Use it for:
- editable/source branding files
- implementation-ready background exports
- logo and icon masters/exports
- store-facing brand assets
- prompt records for AI-assisted concept generation

Do not scatter new brand files across `app/src/main/res/` unless they are final implementation copies needed by Android.

## Subfolders

`logo/svg/`
- master vector logo files

`logo/png/`
- final raster logo exports

`icon/android_tv/`
- Android TV launcher/export assets

`icon/fire_tv/`
- Fire TV specific launcher/export assets

`backgrounds/1080p/`
- implementation-ready 1920x1080 background exports

`backgrounds/4k/`
- 3840x2160 retained high-resolution background exports

`store/`
- store listing imagery and promotional exports

`prompts/`
- prompt text and concept provenance for AI-assisted assets

`source/`
- retained source concepts or pre-cleanup source files

## Naming Convention

Follow `docs/design-system.md` §15.6:

Pattern:
- `tuneflow_[category]_[context]_[variant]_[size]`

Examples:
- `tuneflow_bg_splash_dark_1080.png`
- `tuneflow_bg_hero_wave_4k.png`
- `tuneflow_bg_nowplaying_dark_1080.png`

Rules:
- lowercase only
- underscore-separated
- no spaces
- no version strings in filenames unless required for true parallel variants

## Design System References

See:
- `docs/design-system.md` §14.5 Branded Background Assets
- `docs/design-system.md` §14.6 AI Asset Generation Prompts
- `docs/design-system.md` §14.8 Brand Approval Criteria
- `docs/design-system.md` §15.3 Resolution Standards
- `docs/design-system.md` §15.5 Performance & Optimization Rules
- `docs/design-system.md` §15.6 Naming Conventions
- `docs/design-system.md` §15.7 Folder Structure

## Approval Checklist

Before shipping a new asset:
- readable on dark UI
- low visual noise
- aligned with Premium Restraint
- no animation or decorative motion assets
- optimized file size
- 1080p delivery export present
- 4K retained export present when required
- correct naming
- correct folder placement

## Current Implementation Note

The Android implementation may still keep final runtime copies in `app/src/main/res/` where resource packaging requires them.
Keep `/branding/` as the source-of-truth package for approved brand assets.
