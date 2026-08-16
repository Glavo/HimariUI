# HarfBuzz shape oracle

`shape_oracle.c` writes glyph id, cluster, and advances as JSON.

This host may not have HarfBuzz headers or a compiled binary. Java tests treat a missing
binary as `environment-blocked`.

Example (when HarfBuzz is installed):

```
cc -O2 -o shape-oracle shape_oracle.c -lharfbuzz
./shape-oracle sample.ttf $'bbb'
```
