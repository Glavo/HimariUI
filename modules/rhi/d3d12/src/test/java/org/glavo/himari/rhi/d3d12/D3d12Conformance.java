package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.windows.WindowsBackend;
import org.glavo.himari.platform.windows.WindowsPlatform;
import org.glavo.himari.platform.windows.WindowsWindow;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes production D3D12 device and SDR present evidence.
@NotNullByDefault
public final class D3d12Conformance {
    /// Prevents instantiation.
    private D3d12Conformance() {
    }

    /// Opens a device, presents once to a production HWND, and writes artifacts.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        D3d12Capabilities capabilities;
        D3d12Presentation presentation;
        int descriptorIncrement = 0;
        int resourceReferences = 0;
        boolean gpuCopy = false;
        boolean textureRoundTrip = false;
        int gpuDiffMaxDelta = Integer.MAX_VALUE;
        boolean texturePresented = false;
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try (D3d12Device device = D3d12Device.open()) {
            capabilities = device.capabilities();
            if (!capabilities.r8g8b8a8RenderTarget()) {
                throw new IllegalStateException("R8G8B8A8 is not a render target");
            }
            if (capabilities.hdrPresentationEnabled()) {
                throw new IllegalStateException("D3D12 backend claimed HDR presentation");
            }
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "HimariUI D3D12 Conformance",
                            new LogicalRect(16.0, 16.0, 320.0, 240.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            presentation = device.presentSdr(window.nativeHandle(), 320, 240);
            if (!presentation.cleared() || !presentation.presented()) {
                throw new IllegalStateException("D3D12 swapchain did not clear and present");
            }
            if (presentation.p709CheckHresult() != 0 || !presentation.p709PresentSupported()) {
                throw new IllegalStateException(
                        "IDXGISwapChain3::CheckColorSpaceSupport(P709) did not report present support"
                );
            }
            try (D3d12GpuResource resource = device.createUploadResource(
                    java.lang.foreign.MemorySegment.ofArray(new byte[] { 7, 8, 9, 10 }))) {
                java.lang.foreign.MemorySegment readBack = resource.readBack(device);
                if (readBack.byteSize() != 4L
                        || readBack.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 0L) != 7
                        || readBack.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 3L) != 10) {
                    throw new IllegalStateException("D3D12 upload buffer did not round-trip mapped bytes");
                }
                descriptorIncrement = resource.descriptorIncrement();
                resourceReferences = resource.ownedReferences();
            }
            java.lang.foreign.MemorySegment copied = device.copyThroughDefaultHeap(
                    java.lang.foreign.MemorySegment.ofArray(new byte[] { 1, 2, 3, 4 }));
            gpuCopy = copied.byteSize() == 4L
                    && copied.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 0L) == 1
                    && copied.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 3L) == 4;
            if (!gpuCopy) {
                throw new IllegalStateException("D3D12 default-heap copy did not round-trip");
            }
            byte[] expectedBytes = new byte[16 * 8 * 4];
            for (int pixel = 0; pixel < expectedBytes.length; pixel += 4) {
                expectedBytes[pixel] = 51;
                expectedBytes[pixel + 1] = 102;
                expectedBytes[pixel + 2] = (byte) 204;
                expectedBytes[pixel + 3] = (byte) 255;
            }
            java.lang.foreign.MemorySegment expected = java.lang.foreign.MemorySegment.ofArray(expectedBytes);
            D3d12TextureRoundTrip trip = device.roundTripSdrRgba(expected, 16, 8);
            textureRoundTrip = trip.copied() && trip.maxChannelDelta(expected) == 0;
            if (!textureRoundTrip) {
                throw new IllegalStateException("D3D12 texture upload did not round-trip RGBA");
            }
            D3d12TextureRoundTrip cleared = device.clearSdrAndReadback(0.2f, 0.4f, 0.8f, 1.0f, 16, 8);
            gpuDiffMaxDelta = cleared.maxChannelDelta(expected);
            if (gpuDiffMaxDelta > 1) {
                throw new IllegalStateException("D3D12 GPU clear exceeded software delta " + gpuDiffMaxDelta);
            }
            D3d12Presentation uploaded = device.presentSdrRgba(window.nativeHandle(), expected, 16, 8);
            texturePresented = uploaded.presented() && !uploaded.hdrMetadataApplied();
            if (!texturePresented) {
                throw new IllegalStateException("D3D12 texture present did not succeed");
            }
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("capabilities.json"),
                """
                        {
                          "profile": "m6-d3d12",
                          "r8g8b8a8RenderTarget": %s,
                          "support1": %d,
                          "hdrPresentationEnabled": false,
                          "presentationMode": "%s"
                        }
                        """.formatted(
                        capabilities.r8g8b8a8RenderTarget(),
                        capabilities.support1(),
                        capabilities.presentationMode()
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("presentation.json"),
                """
                        {
                          "presented": %s,
                          "cleared": %s,
                          "backBufferIndex": %d,
                          "format": "%s",
                          "colorSpace": "%s",
                          "hdrMetadataApplied": false,
                          "p709CheckHresult": %d,
                          "p709Support": %d,
                          "p709PresentSupported": %s,
                          "p2020PqCheckHresult": %d,
                          "p2020PqSupport": %d,
                          "ownedReferences": %d,
                          "releasedReferences": %d
                        }
                        """.formatted(
                        presentation.presented(),
                        presentation.cleared(),
                        presentation.backBufferIndex(),
                        presentation.format(),
                        presentation.colorSpace(),
                        presentation.p709CheckHresult(),
                        presentation.p709Support(),
                        presentation.p709PresentSupported(),
                        presentation.p2020PqCheckHresult(),
                        presentation.p2020PqSupport(),
                        presentation.ownedReferences(),
                        presentation.releasedReferences()
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("gpu-diff.json"),
                """
                        {
                          "expectedR": 51,
                          "expectedG": 102,
                          "expectedB": 204,
                          "maxChannelDelta": %d,
                          "tolerance": 1
                        }
                        """.formatted(gpuDiffMaxDelta),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m6-d3d12",
                          "workPackage": "D3D12-001",
                          "status": "passed",
                          "deviceCreated": true,
                          "sdrPresent": true,
                          "cleared": true,
                          "committedResource": true,
                          "descriptorIncrement": %d,
                          "resourceReferences": %d,
                          "gpuCopy": %s,
                          "textureRoundTrip": %s,
                          "gpuDiffMaxDelta": %d,
                          "texturePresented": %s,
                          "hdrClaimed": false
                        }
                        """.formatted(
                        descriptorIncrement,
                        resourceReferences,
                        gpuCopy,
                        textureRoundTrip,
                        gpuDiffMaxDelta,
                        texturePresented
                ),
                StandardCharsets.UTF_8
        );
    }
}
