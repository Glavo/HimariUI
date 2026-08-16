# Isolated FreeType / HarfBuzz oracle JSON

Production `modules/font` and `modules/text` stay pure Java. These oracles live under `oracles/`
and emit JSON on stdout. They are not on the production runtime classpath.

## FreeType outline oracle

Command:

```
outline-oracle <font-file> <glyph-id>
```

Load flags: `FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP | FT_LOAD_NO_SCALE`.

```json
{
  "engine": "freetype",
  "glyphId": 2,
  "advance": 100,
  "commands": [
    {"op": "move", "x": 0, "y": 0},
    {"op": "quad", "cx": 50, "cy": 100, "x": 100, "y": 0},
    {"op": "close"}
  ]
}
```

`line` uses `x`/`y`. Cubic CFF oracles may emit `cubic` later; this slice only records `move`,
`line`, `quad`, and `close`.

## HarfBuzz shape oracle

Command:

```
shape-oracle <font-file> <utf8-text>
```

Features: default HarfBuzz set for the run script. JSON:

```json
{
  "engine": "harfbuzz",
  "glyphs": [
    {"id": 4, "cluster": 0, "xAdvance": 12, "yAdvance": 0, "xOffset": 0, "yOffset": 0}
  ]
}
```

`gid`/`ax` aliases are accepted by the Java parser.

## Environment

- `HIMARI_FREETYPE_ORACLE`: path to `outline-oracle`
- `HIMARI_HARFBUZZ_ORACLE`: path to `shape-oracle`

If neither the environment variable nor `oracles/freetype/outline-oracle[.exe]` /
`oracles/harfbuzz/shape-oracle[.exe]` exists, Java records `environment-blocked` and still
passes Java-only outline and shape tests.

## Compare

Java regularizes outlines like fauntlet (`external/fontations/fauntlet/src/pen.rs`): drop
`close` when already at the start, drop degenerate lines, optionally truncate unscaled
coordinates. Thresholds live in `DIFFERENCE_POLICY.md`.
