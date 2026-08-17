package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

/// Encodes and decodes one independent image format.
///
/// Providers must not require filesystem paths. They must reject truncated input, oversized
/// extents, and decompression that would exceed [`PixelBuffer#MAX_EDGE`].
@NotNullByDefault
public interface ImageCodec {
    /// Returns the format name, such as `jpeg`.
    ///
    /// @return the name
    String name();

    /// Returns whether `bytes` begin with this format's signature.
    ///
    /// @param bytes the candidate stream
    /// @return whether this codec should decode the stream
    boolean recognizes(byte[] bytes);

    /// Decodes `bytes` into a pixel buffer.
    ///
    /// @param bytes the encoded stream
    /// @return the pixels
    PixelBuffer decode(byte[] bytes);

    /// Encodes `pixels` in this format.
    ///
    /// @param pixels the source buffer
    /// @return the encoded stream
    byte @Unmodifiable [] encode(PixelBuffer pixels);
}
