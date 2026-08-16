# Isolated text oracles

C runners that dump FreeType outlines and HarfBuzz shapes as JSON. They are not Gradle
production modules and must not appear on a shipped runtime classpath.

Build notes are in each subdirectory. Default `check` does not compile or download these
oracles. Set `HIMARI_FREETYPE_ORACLE` / `HIMARI_HARFBUZZ_ORACLE` or place the binaries next
to the sources to enable differential tests.

See `ORACLE_FORMAT.md` and the repository-root `DIFFERENCE_POLICY.md`.
