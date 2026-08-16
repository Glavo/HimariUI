/*
 * Isolated HarfBuzz shape dump. Not linked into HimariUI production modules.
 *
 * Usage: shape-oracle <font-file> <utf8-text>
 */

#include <harfbuzz/hb.h>
#include <harfbuzz/hb-ot.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv)
{
    hb_blob_t *blob;
    hb_face_t *face;
    hb_font_t *font;
    hb_buffer_t *buffer;
    unsigned int count;
    unsigned int index;
    hb_glyph_info_t *info;
    hb_glyph_position_t *pos;

    if (argc != 3) {
        fprintf(stderr, "usage: shape-oracle <font-file> <utf8-text>\n");
        return 2;
    }
    blob = hb_blob_create_from_file(argv[1]);
    if (hb_blob_get_length(blob) == 0) {
        fprintf(stderr, "failed to read font\n");
        hb_blob_destroy(blob);
        return 1;
    }
    face = hb_face_create(blob, 0);
    font = hb_font_create(face);
    hb_ot_font_set_funcs(font);
    buffer = hb_buffer_create();
    hb_buffer_add_utf8(buffer, argv[2], -1, 0, -1);
    hb_buffer_guess_segment_properties(buffer);
    hb_shape(font, buffer, NULL, 0);
    count = hb_buffer_get_length(buffer);
    info = hb_buffer_get_glyph_infos(buffer, NULL);
    pos = hb_buffer_get_glyph_positions(buffer, NULL);
    printf("{\"engine\":\"harfbuzz\",\"glyphs\":[");
    for (index = 0; index < count; index++) {
        if (index > 0) {
            fputs(",", stdout);
        }
        printf("{\"id\":%u,\"cluster\":%u,\"xAdvance\":%d,\"yAdvance\":%d,\"xOffset\":%d,\"yOffset\":%d}",
                info[index].codepoint,
                info[index].cluster,
                pos[index].x_advance,
                pos[index].y_advance,
                pos[index].x_offset,
                pos[index].y_offset);
    }
    printf("]}\n");
    hb_buffer_destroy(buffer);
    hb_font_destroy(font);
    hb_face_destroy(face);
    hb_blob_destroy(blob);
    return 0;
}
