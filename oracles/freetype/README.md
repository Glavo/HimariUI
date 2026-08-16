# FreeType outline oracle

`outline_oracle.c` writes unhinted `glyf` commands as JSON.

This host may not have FreeType headers or a compiled binary. Java tests treat a missing
binary as `environment-blocked`.

Example (when FreeType is installed):

```
cc -O2 -o outline-oracle outline_oracle.c -lfreetype
./outline-oracle sample.ttf 2
```
