package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Dispatches independent first-stable image codec providers.
@NotNullByDefault
public final class ImageCodecs {
    /// PNG provider.
    private static final ImageCodec PNG = new StaticCodec("png", PngImage::isPng, PngImage::decode, PngImage::encode);

    /// JPEG baseline provider.
    private static final ImageCodec JPEG = new StaticCodec("jpeg", JpegImage::isJpeg, JpegImage::decode, JpegImage::encode);

    /// GIF89a provider.
    private static final ImageCodec GIF = new StaticCodec("gif", GifImage::isGif, GifImage::decode, GifImage::encode);

    /// WebP lossless provider.
    private static final ImageCodec WEBP = new StaticCodec("webp", WebpImage::isWebp, WebpImage::decode, WebpImage::encode);

    /// AVIF still-image provider.
    private static final ImageCodec AVIF = new StaticCodec("avif", AvifImage::isAvif, AvifImage::decode, AvifImage::encode);

    /// Prevents instantiation.
    private ImageCodecs() {
    }

    /// Returns the PNG provider.
    ///
    /// @return the provider
    public static ImageCodec png() {
        return PNG;
    }

    /// Returns the JPEG provider.
    ///
    /// @return the provider
    public static ImageCodec jpeg() {
        return JPEG;
    }

    /// Returns the GIF provider.
    ///
    /// @return the provider
    public static ImageCodec gif() {
        return GIF;
    }

    /// Returns the WebP provider.
    ///
    /// @return the provider
    public static ImageCodec webp() {
        return WEBP;
    }

    /// Returns the AVIF provider.
    ///
    /// @return the provider
    public static ImageCodec avif() {
        return AVIF;
    }

    /// Returns every first-stable codec provider in probe order.
    ///
    /// @return the providers
    public static @Unmodifiable List<ImageCodec> providers() {
        return List.of(PNG, JPEG, GIF, WEBP, AVIF);
    }

    /// Probes `bytes` and decodes with the first recognizing provider.
    ///
    /// @param bytes the encoded stream
    /// @return the pixels
    public static PixelBuffer decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        for (ImageCodec codec : providers()) {
            if (codec.recognizes(bytes)) {
                return codec.decode(bytes);
            }
        }
        throw new IllegalArgumentException("No first-stable codec recognizes the stream");
    }

    /// Adapts static encode/decode methods onto [`ImageCodec`].
    private static final class StaticCodec implements ImageCodec {
        /// Format name.
        private final String name;

        /// Signature probe.
        private final Recognizer recognizer;

        /// Decoder.
        private final Decoder decoder;

        /// Encoder.
        private final Encoder encoder;

        /// Creates a provider.
        private StaticCodec(String name, Recognizer recognizer, Decoder decoder, Encoder encoder) {
            this.name = name;
            this.recognizer = recognizer;
            this.decoder = decoder;
            this.encoder = encoder;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean recognizes(byte[] bytes) {
            return recognizer.test(bytes);
        }

        @Override
        public PixelBuffer decode(byte[] bytes) {
            return switch (decoder.decode(bytes)) {
                case PngImage.Decoded decoded -> PixelBuffer.srgbUnassociated(
                        decoded.width(),
                        decoded.height(),
                        decoded.rgba()
                );
                case JpegImage.Decoded decoded -> PixelBuffer.srgbUnassociated(
                        decoded.width(),
                        decoded.height(),
                        decoded.rgba()
                );
                case GifImage.Decoded decoded -> PixelBuffer.srgbUnassociated(
                        decoded.width(),
                        decoded.height(),
                        decoded.rgba()
                );
                case WebpImage.Decoded decoded -> PixelBuffer.srgbUnassociated(
                        decoded.width(),
                        decoded.height(),
                        decoded.rgba()
                );
                case AvifImage.Decoded decoded -> PixelBuffer.srgbUnassociated(
                        decoded.width(),
                        decoded.height(),
                        decoded.rgba()
                );
                default -> throw new IllegalStateException("Unsupported decoded image");
            };
        }

        @Override
        public byte @Unmodifiable [] encode(PixelBuffer pixels) {
            Objects.requireNonNull(pixels, "pixels");
            return encoder.encode(pixels.width(), pixels.height(), pixels.rgba());
        }
    }

    /// Signature probe.
    @FunctionalInterface
    private interface Recognizer {
        /// Returns whether the stream matches.
        boolean test(byte[] bytes);
    }

    /// Decoder that returns a format-specific record.
    @FunctionalInterface
    private interface Decoder {
        /// Decodes the stream.
        Object decode(byte[] bytes);
    }

    /// Encoder of raw RGBA.
    @FunctionalInterface
    private interface Encoder {
        /// Encodes the pixels.
        byte[] encode(int width, int height, byte[] rgba);
    }
}
