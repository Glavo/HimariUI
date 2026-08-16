# Difference policy (font outlines and Tier-1 shaping)

This file records how Java outline and shaping results are compared to isolated FreeType and
HarfBuzz oracles. Thresholds start from the constructed-font self-tests and the first successful
oracle run. They are not a promise of pixel-identical hinting.

## Status

- Java-only outline, raster, and shaping tests on constructed fonts are the `check` gate.
- Native oracles are optional. A host without `outline-oracle` / `shape-oracle` records
  `environment-blocked` and still passes `check`.
- Matching FreeType or HarfBuzz latency and RSS is an optimization goal after these
  correctness thresholds exist. It is not a gate on the first reference path.

## Outline / unhinted coverage (FreeType)

Corpus: `OutlineSampleFont` (quadratic bump, implied on-curve, composite) and later checked-in
TTF files.

Compare after fauntlet-style regularization (`OutlineCompare`):

| Field | Threshold | Rationale |
|---|---|---|
| Command verbs | exact after regularize | Close and degenerate lines are not semantic |
| Coordinates | 0.5 font units (unscaled) | FreeType `NO_SCALE` truncates odd implied midpoints |
| Horizontal advance | exact font units | `hmtx` / `FT_Glyph_Metrics.horiAdvance` |
| Grayscale mask | not compared to FT bitmaps in this slice | Java 4×4 SSAA is a reference coverage, not FT's rasterizer |

Pass when every constructed-font glyph is within the table, or when the oracle is
`environment-blocked` and Java self-tests pass.

## Shaping (HarfBuzz)

Corpus: Latin one-to-one on `BitmapSfntFont` / `ScriptSampleFont`; Arabic `isol`/`init`/`medi`/`fina`
on `GsubSampleFont`; Unicode fallback paths on `ScriptSampleFont`.

| Field | Threshold | Rationale |
|---|---|---|
| Glyph id | exact | GSUB type 1 and `cmap` identities |
| Cluster | exact | First-stable cluster preservation |
| `xAdvance` | 1 font unit | Integer `hmtx` advances; HB may report 26.6 later |
| `yAdvance` / offsets | 0 on this slice | No GPOS yet |

Presentation-form fallback (no GSUB) is compared as mapped glyph ids, not as Unicode
Presentation Forms-B code points.

## Revisions

Record a new row here when an oracle run on this host first produces numbers, or when a
reviewed difference is accepted. Do not silently widen thresholds to hide a regression.
