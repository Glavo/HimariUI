/*
 * Isolated FreeType outline dump. Not linked into HimariUI production modules.
 *
 * Usage: outline-oracle <font-file> <glyph-id>
 *
 * Loads with FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP | FT_LOAD_NO_SCALE and writes
 * move/line/quad/close JSON to stdout.
 */

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_OUTLINE_H

#include <stdio.h>
#include <stdlib.h>

static int g_first_command = 1;

static void comma(void)
{
    if (!g_first_command) {
        fputs(",", stdout);
    }
    g_first_command = 0;
}

static int move_to(const FT_Vector *to, void *user)
{
    (void) user;
    comma();
    printf("{\"op\":\"move\",\"x\":%ld,\"y\":%ld}", (long) to->x, (long) to->y);
    return 0;
}

static int line_to(const FT_Vector *to, void *user)
{
    (void) user;
    comma();
    printf("{\"op\":\"line\",\"x\":%ld,\"y\":%ld}", (long) to->x, (long) to->y);
    return 0;
}

static int quad_to(const FT_Vector *control, const FT_Vector *to, void *user)
{
    (void) user;
    comma();
    printf("{\"op\":\"quad\",\"cx\":%ld,\"cy\":%ld,\"x\":%ld,\"y\":%ld}",
            (long) control->x, (long) control->y, (long) to->x, (long) to->y);
    return 0;
}

static int cubic_to(const FT_Vector *c1, const FT_Vector *c2, const FT_Vector *to, void *user)
{
    (void) user;
    comma();
    printf("{\"op\":\"cubic\",\"c1x\":%ld,\"c1y\":%ld,\"c2x\":%ld,\"c2y\":%ld,\"x\":%ld,\"y\":%ld}",
            (long) c1->x, (long) c1->y, (long) c2->x, (long) c2->y, (long) to->x, (long) to->y);
    return 0;
}

int main(int argc, char **argv)
{
    FT_Library library;
    FT_Face face;
    FT_Outline_Funcs funcs;
    unsigned long glyph_id;
    char *end = NULL;

    if (argc != 3) {
        fprintf(stderr, "usage: outline-oracle <font-file> <glyph-id>\n");
        return 2;
    }
    glyph_id = strtoul(argv[2], &end, 10);
    if (end == argv[2]) {
        fprintf(stderr, "invalid glyph id\n");
        return 2;
    }
    if (FT_Init_FreeType(&library) != 0) {
        fprintf(stderr, "FT_Init_FreeType failed\n");
        return 1;
    }
    if (FT_New_Face(library, argv[1], 0, &face) != 0) {
        fprintf(stderr, "FT_New_Face failed\n");
        FT_Done_FreeType(library);
        return 1;
    }
    if (FT_Load_Glyph(face, (FT_UInt) glyph_id,
            FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP | FT_LOAD_NO_SCALE) != 0) {
        fprintf(stderr, "FT_Load_Glyph failed\n");
        FT_Done_Face(face);
        FT_Done_FreeType(library);
        return 1;
    }
    funcs.move_to = move_to;
    funcs.line_to = line_to;
    funcs.conic_to = quad_to;
    funcs.cubic_to = cubic_to;
    funcs.shift = 0;
    funcs.delta = 0;
    printf("{\"engine\":\"freetype\",\"glyphId\":%lu,\"advance\":%ld,\"commands\":[",
            glyph_id, (long) face->glyph->metrics.horiAdvance);
    if (FT_Outline_Decompose(&face->glyph->outline, &funcs, NULL) != 0) {
        fprintf(stderr, "FT_Outline_Decompose failed\n");
        FT_Done_Face(face);
        FT_Done_FreeType(library);
        return 1;
    }
    printf("]}\n");
    FT_Done_Face(face);
    FT_Done_FreeType(library);
    return 0;
}
